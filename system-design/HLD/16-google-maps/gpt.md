Great — I re-read the whole thread and expanded everything into a **single, detailed, practical data-ingestion & processing reference** that includes everything we discussed so far plus many more specifics about **OSM raw data**, **satellite imagery**, how each looks, the tools/commands you’d use, exact processing steps, where we store each artifact (schemas), and concrete mock examples showing the transformations from raw → canonical → published artifacts.

I kept it concrete and interview-friendly so you can study, cite specifics in interviews, or copy–paste SQL/JSON examples into a demo. If you want, I can now produce ready-to-run `CREATE TABLE` + `INSERT` SQL for a PostGIS instance with all the example rows — say the word and I’ll dump them.

# Overview (what this doc covers)

1. OSM data: raw format, key concepts, parsing & normalization, example raw snippet → canonical rows.
2. Satellite imagery: raw file & metadata, sensor bands, file formats, preprocessing (detailed), ML extraction → vectorization, example pixel→lat/lon calculation → output.
3. How we merge OSM + satellite-derived features + telemetry (map matching & traffic) into canonical DBs.
4. Full set of schemas / tables (expanded) with fields for provenance, confidence, versions, diffs.
5. Storage & serving artifacts (tiles, graph binaries, MBTiles, COG).
6. Tools, commands, and practical notes.
7. Mock data examples and step-by-step transformations.

---

# 1) OSM — what the raw data looks like & how we process it

## 1.1 OSM raw structure (concepts)

OpenStreetMap raw data (planet .osm.pbf or regional extracts) encodes three primary primitives:

* **Node** — a point: `id`, `lat`, `lon`, zero or more `tags`.

  * Example: a bench, a POI, or a vertex of a way.
* **Way** — an ordered list of node references + `tags`. Ways represent roads, building outlines, paths, etc.

  * Example: `way id=1234 nodes=[n1,n2,n3] tags={highway=primary, name=MG Road}`
* **Relation** — a grouping of nodes/ways/relations with roles; used for multipolygons, turn restrictions, routes, administrative boundaries.

  * Example: relation for a bus route, or a building with courtyard.

Each primitive has `tags`: key=value strings with domain-specific semantics (`highway=residential`, `building=yes`, `addr:street=MG Road`, etc.)

OSM files are typically encoded in PBF format (binary) for efficiency.

## 1.2 Example raw OSM snippet (PBF equivalent shown as XML-like for readability)

```xml
<node id="1001" lat="12.9716" lon="77.5946" />
<node id="1002" lat="12.9720" lon="77.5950" />
<node id="1003" lat="12.9730" lon="77.5960" />

<way id="2001">
  <nd ref="1001"/>
  <nd ref="1002"/>
  <tag k="highway" v="residential"/>
  <tag k="name" v="MG Road"/>
</way>

<way id="2002">
  <nd ref="1002"/>
  <nd ref="1003"/>
  <tag k="highway" v="primary"/>
  <tag k="name" v="MG Road"/>
  <tag k="maxspeed" v="50"/>
</way>

<node id="3001" lat="12.9718" lon="77.5948">
  <tag k="name" v="Coffee Day"/>
  <tag k="amenity" v="cafe"/>
</node>
```

## 1.3 Key OSM processing steps (from raw → canonical)

1. **Acquire & store raw files**

   * Keep raw `.osm.pbf` in object store with metadata: provider, capture\_time, checksum.
   * Example path: `s3://maps-raw/osm/planet/planet-2025-09-12.osm.pbf`.

2. **Parsing**

   * Tools: `osmium`, `osmium-tool`, `osmconvert`, `osmosis`.
   * Example: extract only roads and POIs for region:

     ```
     osmium tags-filter planet.osm.pbf w/highway -o roads.osm.pbf
     osmium tags-filter planet.osm.pbf n/amenity -o pois.osm.pbf
     ```
   * `osmium` can also convert to GeoJSON or to PostGIS via `osm2pgsql`/`imposm`.

3. **Import to staging DB (PostGIS)**

   * `osm2pgsql` or `imposm3` will import nodes, ways into PostGIS tables (often schema: `planet_osm_nodes`, `planet_osm_ways`).
   * Example command (osm2pgsql):

     ```
     osm2pgsql -d maps_staging -C 2000 --slim planet-2025-09-12.osm.pbf
     ```

4. **Normalize / canonicalize schema**

   * Map OSM `tags` to canonical fields:

     * `highway` → `road_type` enum, `speed_limit` default rules (e.g., residential → 30 km/h)
     * `maxspeed` tag overrides inferred speed
     * `name` → `name`
     * `oneway=yes` → `one_way = true`
     * `building=yes` or `building:levels` → building attributes
   * Normalize multi-language names (`name:en`, `name:local`) into `name_local`, `name_en`.

5. **Topology & cleaning**

   * Snap nodes that are within ε meters to the same canonical node (fix OSM tiny mismatches, duplicate vertices).
   * Simplify geometries (optional): remove tiny segments under threshold (but preserve for accuracy).
   * Detect invalid geometries and repair (Self-intersections, unclosed polygons).
   * Tools: PostGIS `ST_Simplify`, `ST_MakeValid`, `ST_Snap`.

6. **Build road graph**

   * For each OSM `way` representing a road:

     * Convert into one or more directed `edges` between intersection nodes.
     * Compute `length_m = ST_Length(geom::geography)`.
     * Compute `base_time_s = length_m / (speed_limit_kmph * 1000/3600)`.
     * Split ways into edges at intersections (nodes with degree >2).
   * Decide on **edge-based** vs **node-based** graph representation (edge-based required for accurate turn penalties & restrictions).

7. **Turn restrictions & relations**

   * Parse OSM relations for `restriction` type (e.g., `no_left_turn`) and add turn-costs or block transitions between edges.
   * Represent turn restrictions in supplementary `turn_restrictions` table.

8. **Provenance & versioning**

   * Keep `source=OSM` and `source_osm_id` plus `source_ts` (from OSM `timestamp` and `user`).
   * Keep `last_modified` and `version` to support diffs.

9. **Diffs / incremental updates**

   * OSM provides diffs (minutely/hourly) or use `OSM change files` to apply incremental updates.
   * Strategy: apply diffs to staging PostGIS, run targeted reprocessing (local CH rebuild or fallback update strategies).

## 1.4 Example: parsed canonical rows (from the mock)

**From OSM raw snippet → canonical `road_nodes` and `road_edges`**

`road_nodes` rows:

```
id | osm_id | lat       | lon       | geom
1  | 1001   | 12.9716   | 77.5946   | POINT(77.5946 12.9716)
2  | 1002   | 12.9720   | 77.5950   | POINT(77.5950 12.9720)
3  | 1003   | 12.9730   | 77.5960   | POINT(77.5960 12.9730)
```

`road_edges` rows (split at intersections):

```
id| osm_way_id | u_node | v_node | geom (LINESTRING lon lat...)         | length_m | speed_limit_kmph | base_time_s | name
1 | 2001       | 1      | 2      | LINESTRING(77.5946 12.9716, 77.5950 12.9720) | 60       | 30               | 7           | MG Road
2 | 2002       | 2      | 3      | LINESTRING(77.5950 12.9720, 77.5960 12.9730) | 150      | 50               | 11          | MG Road
```

**Turn restriction example (from relation)**

```
id | from_edge | to_edge | restriction_type
1  | 1         | 2       | no_left_turn
```

---

# 2) Satellite imagery — how raw images look, how we process, and how we save/store results

Satellite imagery processing is large-area raster processing plus ML extraction to derive vectors. I’ll break it into:

* What raw imagery files look like and the key metadata.
* Full preprocessing pipeline (detailed).
* ML segmentation & vectorization.
* Storage & serving formats.
* Example conversions and a tiny pixel → lat/lon walkthrough.

## 2.1 Satellite imagery: raw file & metadata

### File formats & delivery

* **GeoTIFF** — common format with embedded georeferencing (affine transform), multiple bands. Can be large.
* **Cloud-optimized GeoTIFF (COG)** — GeoTIFF optimized for HTTP range-requests (best for cloud hosting).
* **JPEG2000 (JP2)** — used by some providers (Landsat).
* **HDF / NetCDF** — some sensor-specific formats (MODIS).
* **MBTiles** — container for tiles (SQLite) used for pre-rendered tiles.

### Key metadata embedded in a GeoTIFF

* **CRS** (Coordinate Reference System) — typically EPSG:4326 (WGS84) or EPSG:3857 (WebMercator) for tiles.
* **GeoTransform** — affine mapping from pixel coordinates (col,row) to geographic coordinates:

  * `x_geo = GT[0] + col * GT[1] + row * GT[2]`
  * `y_geo = GT[3] + col * GT[4] + row * GT[5]`
  * For north-up images, GT\[2] & GT\[4] typically 0 and GT\[1] = pixel width, GT\[5] = negative pixel height.
* **Resolution** — meters/pixel (e.g., 0.5 m/pixel for high-res).
* **Bands** — arrays for R,G,B or multispectral (R,G,B,NIR,SWIR,Thermal).
* **Capture time / Satellite pass time**
* **Sun angle, sensor ID, processing level**

### Tiny mock GeoTIFF metadata example (human-readable)

```
Filename: tile_0_0.tif
CRS: EPSG:4326
GeoTransform: [77.5940, 0.00001, 0.0, 12.9760, 0.0, -0.00001]
Width: 5000 px
Height: 5000 px
Bands: [R,G,B]
Resolution: 0.00001 degrees/px (~1.11 m/px at equator if using degrees)
Capture_time: 2025-09-12T08:59:00Z
Provider: PlanetLabs
```

The GeoTransform tells you exactly which lat/lon maps to each pixel.

## 2.2 Preprocessing steps (detailed, in order)

1. **Acquire & register metadata**

   * Record s3 path, capture\_time, sensor, resolution, checksum.
   * Insert row in `imagery_tiles` table (metadata index).

2. **Radiometric calibration**

   * Convert raw digital numbers (DN) to at-sensor radiance or reflectance.
   * Remove sensor gain/offset and normalize across scenes.

3. **Atmospheric correction**

   * Remove haze/atmospheric scattering to get surface reflectance (e.g., using Dark Object Subtraction, or Sen2Cor for Sentinel).
   * Optional: convert to top-of-atmosphere (TOA) reflectance if required.

4. **Pan-sharpening** (if multi-resolution pansharpened imagery available)

   * Fuse high-res panchromatic band with lower-res multispectral bands to improve visual sharpness.

5. **Cloud detection & masking**

   * Use threshold on spectral bands or ML-based cloud detectors to identify cloudy pixels.
   * Mark scenes with high cloud cover for rejection or for partial processing.

6. **Orthorectification**

   * Correct geometric distortions due to sensor angle and topography using DEM (digital elevation model).
   * Results are geo-accurate images aligning with ground coordinates.

7. **Radiometric & color balancing across scenes**

   * When mosaicking multiple tiles, balance color/brightness to reduce seams.

8. **Mosaicking & cutlines**

   * Stitch adjacent tiles into larger mosaics where needed.
   * Keep cutlines to define which pixel from overlapping scenes to keep (freshness, cloud-free, lower angle).

9. **Tiling & pyramid generation**

   * Produce image tile pyramid (256×256 PNG/WebP or 512 tiles) for WebMercator (z/x/y).
   * Optionally produce vector tiles overlay (see vectorization below).

10. **Indexing & thumbnail generation**

    * Compute quick-look thumbnails, per-tile stats (mean, std), and NDVI or other indexes for land classification.

11. **Push artifacts to object store + update `imagery_tiles` processed flag.**

## 2.3 ML extraction: detect roads / buildings / landcover (detailed)

### ML model types

* **Semantic segmentation (pixel-wise)** — U-Net, DeepLab: outputs mask of classes (road, building, water, vegetation).
* **Instance segmentation** — detect individual building footprints (Mask R-CNN).
* **Object detection** — detect features like vehicles, swimming pools (bounding boxes).

### Typical pipeline for road/building extraction

1. **Tile slicing**: crop imagery into fixed-size patches (e.g., 512×512) with some overlap.
2. **Run segmentation model** on each patch → output mask with classes and per-pixel confidence.
3. **Post-process mask**:

   * Morphological ops (erode/dilate) to remove speckles.
   * Remove tiny islands (area < threshold).
4. **Vectorize** masks:

   * Use `GDAL`/`shapely`/`OpenCV` to trace mask boundaries and create polygons (for buildings) or polylines (for roads).
   * Simplify geometry with `ST_Simplify` (preserve topology).
5. **Attribute extraction**:

   * For buildings: infer `footprint_area`, `orientation`, `roof_type` (if possible).
   * For roads: infer centerline, approximate width (from mask thickness), possible `road_type` via width + context.
6. **Scoring**:

   * Assign `confidence` value per feature (model probability, IoU) and `coverage` (how many tiles agreed).
7. **Candidate features table**:

   * Store extracted features in `imagery_features` / `candidate_features` with geometry, class, confidence, source\_tile\_id, and thumbnails.

### Example segmentation output (mock)

Input patch (5×5 grayscale intensities):

```
[[200,200,200, 50,50],
 [200,200,200, 60,60],
 [200,200,200, 70,70],
 [200,200,200, 80,80],
 [200,200,200, 90,90]]
```

Model mask:

```
[[1,1,1,0,0],
 [1,1,1,0,0],
 [1,1,1,0,0],
 [1,1,1,0,0],
 [1,1,1,0,0]]
```

Vectorization → `LINESTRING(lon1 lat1, lon2 lat2, ...)` or narrow polygon converted to centerline.

## 2.4 Merging extracted features with existing vector datasets

* **Matching strategy**:

  * Spatial join: `ST_DWithin(extracted_geom, existing_geom, ε)` to find overlapping features.
  * Attribute similarity: names, widths, etc.
* **Conflict resolution**:

  * If existing OSM/POI has higher authority → prefer OSM unless extraction confidence very high.
  * If extracted feature is new (no overlapping vector), insert a new candidate and mark for QA or auto-merge if confidence > threshold.
* **Provenance**:

  * Store `source='satellite:planet:tile_20250912'`, `confidence`, `extracted_at`.

## 2.5 Storage & serving formats (raster & vector)

* **Raw & processed imagery**: store as **COG** in S3. Path: `s3://maps-imagery/{date}/{tile}.tif`.
* **Image tile pyramids (raster)**: generate z/x/y PNG tiles (WebMercator) and store in `s3://tiles/raster/{z}/{x}/{y}.png`. Serve via CDN.
* **Vector tiles**: Mapbox Vector Tile (`.pbf`) per z/x/y stored in S3 or MBTiles container. Tool: `tippecanoe` to create.
* **Extracted vector features**:

  * Primary store: **PostGIS** (`building_footprints`, `road_candidates`, `imagery_features`).
  * Secondary store: **GeoPackage** or **MBTiles** for static delivery.
* **Graph artifacts**: routing binary formats (CH/CRP) stored in S3: `s3://maps-graphs/{region}/{version}/graph.bin`.

### Recommended formats

* Imagery: Cloud Optimized GeoTIFF (COG).
* Tiles: Vector tiles (MVT) for road/polygon overlays; raster tiles for satellite basemap.
* Feature snapshots: GeoJSON/GeoPackage for audits & manual QA.

## 2.6 Example: a tiny pixel → lat/lon calculation

GeoTransform (example):

```
GT = [77.5940, 0.00001, 0.0, 12.9760, 0.0, -0.00001]
```

* Pixel i (column), j (row) → lon = 77.5940 + i \* 0.00001, lat = 12.9760 + j \* (-0.00001).

If pixel at (i=100, j=200):

* lon = 77.5940 + 100 \* 0.00001 = 77.5950
* lat = 12.9760 - 200 \* 0.00001 = 12.9740

This calculation is embedded in GDAL/Rasterio and used during vectorization to produce geometries in geographic coords.

---

# 3) How OSM + Satellite + Telemetry glue together (merge strategy & workflow)

1. **Canonical store is authoritative**: PostGIS tables `road_edges`, `pois`, `building_footprints`.
2. **Source of truth + confidence**:

   * OSM entries keep `source='osm'`, `source_osm_id`, OSM `version`.
   * Satellite-extracted features have `source='satellite'` and `confidence`.
   * Telemetry-inferred changes (e.g., new road traffic patterns) have `source='telemetry'` and `count`.
3. **Merge policy examples**:

   * If satellite feature overlaps OSM feature and `confidence > 0.9` and OSM `last_modified` older than some threshold → mark for auto-update or create `update_suggestion` record.
   * If telemetry shows consistent vehicle travel on a geometry not in OSM and `count >= k` (k-anonymity threshold) → create candidate road with `suggested=true` and `supporting_evidence=[telemetry_count, imagery_confidence]`.
4. **Human-in-the-loop**:

   * For low-confidence merges or sensitive changes (e.g., removing an existing road), present candidates in an internal QA dashboard for human validation.
5. **Atomic updates & rollback**:

   * Prepare updated artifacts in staging (new graph build, new tiles), run automated QA tests (sample routes, topology checks), then swap production pointer to new artifacts (versioning).
6. **Change feed**:

   * Publish changes to `changes` Kafka topic so route servers, tile servers, caches can invalidate.

---

# 4) Expanded canonical schemas (adds provenance, confidence, change-log)

Below are expanded tables to support the richer pipeline.

### 4.1 `road_nodes` (expanded)

```sql
CREATE TABLE road_nodes (
  id BIGSERIAL PRIMARY KEY,
  osm_id BIGINT,          -- nullable, original osm id
  geom geometry(Point,4326) NOT NULL,
  created_at TIMESTAMP DEFAULT now(),
  last_modified TIMESTAMP,
  source TEXT,            -- 'osm' | 'satellite' | 'telemetry'
  source_ref JSONB,       -- e.g. {"osm_version":4} or {"tile":"tile_0_0.tif"}
  confidence DOUBLE PRECISION DEFAULT 1.0
);
```

### 4.2 `road_edges` (expanded)

```sql
CREATE TABLE road_edges (
  id BIGSERIAL PRIMARY KEY,
  osm_way_id BIGINT,       -- optional
  u_node BIGINT NOT NULL REFERENCES road_nodes(id),
  v_node BIGINT NOT NULL REFERENCES road_nodes(id),
  geom geometry(LineString,4326) NOT NULL,
  length_m DOUBLE PRECISION NOT NULL,
  base_time_s DOUBLE PRECISION NOT NULL,
  last_known_time_s DOUBLE PRECISION, -- time with live traffic applied
  speed_limit_kmph DOUBLE PRECISION,
  allowed_modes TEXT[],
  name TEXT,
  one_way BOOLEAN DEFAULT FALSE,
  source TEXT,            -- 'osm' | 'satellite' | 'merged'
  source_confidence DOUBLE PRECISION,
  provenance JSONB,       -- list of source refs
  created_at TIMESTAMP DEFAULT now(),
  last_modified TIMESTAMP
);
```

### 4.3 `building_footprints`

```sql
CREATE TABLE building_footprints (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  geom geometry(Polygon,4326),
  source TEXT,            -- 'osm' | 'satellite' | 'commercial'
  source_ref JSONB,
  confidence DOUBLE PRECISION, -- model probability / consensus
  attrs JSONB,            -- e.g. {"levels":3,"roof":"flat"}
  created_at TIMESTAMP DEFAULT now(),
  last_modified TIMESTAMP
);
```

### 4.4 `imagery_features` (candidate features from imagery)

```sql
CREATE TABLE imagery_features (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tile_id UUID,           -- references imagery_tiles
  geom geometry(Geometry,4326),
  class TEXT,             -- 'road' | 'building' | 'water' | 'vegetation'
  confidence DOUBLE PRECISION,
  vectorized_at TIMESTAMP,
  processed BOOLEAN DEFAULT FALSE,
  merged_into_edge BIGINT NULL, -- if merged into road_edges
  created_at TIMESTAMP DEFAULT now()
);
```

### 4.5 `change_log` (audit)

```sql
CREATE TABLE change_log (
  id BIGSERIAL PRIMARY KEY,
  object_type TEXT,       -- 'road_edge','poi','building'
  object_id TEXT,         -- object id (bigint or uuid)
  op TEXT,                -- 'insert'|'update'|'delete'
  diff JSONB,             -- minimal patch
  source TEXT,
  created_at TIMESTAMP DEFAULT now(),
  version TEXT            -- graph/tile version affected
);
```

---

# 5) Storage & serving artifacts (detailed notes)

## 5.1 Object storage layout (S3)

* `s3://maps-raw/osm/{date}/planet-{date}.osm.pbf`
* `s3://maps-imagery/{date}/raw/{tile}.tif` (COG)
* `s3://maps-imagery/{date}/processed/{tile}.tif` (orthorectified, cloud-free)
* `s3://tiles/raster/{z}/{x}/{y}.png`
* `s3://tiles/vector/{z}/{x}/{y}.pbf`
* `s3://maps-graphs/{region}/{version}/graph.bin`
* `s3://snapshots/{version}/` (full snapshot for rollback)

## 5.2 Serving

* Raster tiles fronted by CDN (CloudFront, Fastly).
* Vector tiles delivered directly or via CDN.
* Tile metadata stored in `tile_metadata` DB to map requests to S3 keys.
* Routing servers pull `graph_versions` from S3; serve queries locally.

## 5.3 Databases

* **PostGIS**: canonical vector data & indexes. Good for complex spatial queries.
* **Kafka**: streaming ingestion (telemetry, POI changes, imagery manifests).
* **Redis**: hot traffic per-edge aggregates & last-known speeds.
* **TSDB** (Prometheus/Influx/TimescaleDB): historical per-edge speeds/time-series for analytics.
* **Search index** (Elasticsearch or OpenSearch): POI name autocomplete and geosearch.

---

# 6) Tools, libraries & representative commands

### OSM processing

* `osmium` / `osmconvert` / `osmosis`: parsing/filtering
* `osm2pgsql` or `imposm3` to import to PostGIS
* `ogr2ogr` (GDAL) for format conversion

### Raster & imagery

* `GDAL` / `Rasterio` for GeoTIFF handling:

  * Reproject / warp / translate:

    ```
    gdalwarp -t_srs EPSG:3857 input.tif output_warped.tif
    ```
  * Build COG (Cloud-Optimized GeoTIFF):

    ```
    gdal_translate -of COG input.tif output.cog.tif
    ```
* Tiling:

  * Generate tiles with `gdal2tiles.py` or tools that produce z/x/y.
* ML:

  * TensorFlow / PyTorch to train segmentation models.
  * Inference script that loads model and writes masks.

### Vector tiling

* `tippecanoe` to convert GeoJSON → MBTiles (vector tiles):

  ```
  tippecanoe -o tiles.mbtiles -zg --drop-densest-as-needed features.geojson
  ```

### Routing preprocess

* `osrm` or `valhalla` or custom CH builder.
* Build graph binary and upload to S3.

### Example pipeline orchestration

* Use Airflow / Prefect / Luigi or Kubernetes-native jobs to orchestrate nightly builds.

---

# 7) Mock end-to-end worked example (very concrete)

### Inputs (raw)

* `planet-2025-09-12.osm.pbf` (contains the OSM snippet earlier)
* `tile_0_0.tif` (small GeoTIFF covering our 3 nodes)
* telemetry messages: ping1, ping2, ping3

### Steps (compact)

1. `osmium tags-filter planet.pbf w/highway -o roads.osm.pbf`
2. `osm2pgsql -d maps_staging roads.osm.pbf` → now we have `planet_osm_line` etc.
3. PostGIS job: split ways into edges, compute `length_m` & `base_time_s`:

   ```sql
   INSERT INTO road_nodes (osm_id, geom) VALUES (1001, ST_SetSRID(ST_MakePoint(77.5946,12.9716),4326));
   -- ... nodes 1002,1003
   INSERT INTO road_edges (osm_way_id, u_node, v_node, geom, length_m, base_time_s, speed_limit_kmph, name, source)
   VALUES (2001,1,2, ST_SetSRID(ST_MakeLine(ST_MakePoint(77.5946,12.9716), ST_MakePoint(77.5950,12.9720)),4326), 60, 7, 30, 'MG Road', 'osm');
   ```
4. Imagery processing:

   * `gdal_translate` / `gdalwarp` → `tile_0_0.processed.tif` stored to S3.
   * Run segmentation on tile → `imagery_features` row created:

     ```
     id: f1, tile_id: t123, class: 'road', confidence: 0.92, geom: LINESTRING(...)
     ```
5. Map-matching telemetry:

   * `telemetry.raw` → map-matching (HMM) → `telemetry.matched` messages: ping1→edge1, ping2→edge2, ping3→edge2
6. Traffic aggregation:

   * aggregator computes avg speeds: edge1 avg 10 km/h, edge2 avg 27.5 km/h → writes to Redis
7. Merge candidate imagery feature:

   * imagery feature `f1` overlaps edge2 within 5m with high confidence → create `change_log` entry suggesting `edge geometry update` or `new edge` if no existing match. If policy auto-merge: merge geometry and update `road_edges.provenance` to include satellite tile ref.

### Final artifacts

* `road_edges` updated with last\_known\_time\_s computed using Redis speeds.
* `imagery_tiles` processed flag set to true and `processed_s3_key` recorded.
* Vector tiles regenerated for z that contains our edges; new MVT uploaded to `s3://tiles/vector/...`.
* Graph rebuild scheduled for the region (full or partial depending on changes).

---

# 8) Interview-friendly summary & talking points (concise)

* **OSM**: structured vector primitives (nodes, ways, relations). Parse with `osmium`/`osm2pgsql`, canonicalize tags, build directed graph, apply turn restrictions from relations, keep provenance, use diffs for incremental updates.
* **Satellite**: raster GeoTIFFs (COG preferred). Preprocess (radiometric, ortho, cloud mask), tile, run ML segmentation to extract roads/buildings, vectorize, store candidates in `imagery_features` with confidence, merge with canonical data after QA.
* **Telemetry**: ingest pings → map-match (HMM) → aggregate speeds → store in Redis/TSDB → used for live routing.
* **Storage**: use PostGIS for canonical vector, S3 for large artifacts (COG, tiles, graphs), Redis for hot traffic, Kafka for streaming, TSDB for historical.
* **Merge logic**: rely on confidence/provenance and human-in-the-loop for ambiguous cases. Version everything; build graphs and tiles atomically and keep rollback snapshots.

---
---

Great — let’s go step by step in plain language and with concrete mini-examples, so you see how maps systems like Google Maps convert **raw satellite images → usable roads/buildings → real-world map features**.

---

# 1. How do we know **where an image belongs**?

When a satellite takes a picture:

* The satellite knows **its orbit position** and **camera orientation** at capture time.
* Each pixel in the sensor corresponds to a ground patch.
* This information is stored as **georeferencing metadata**.

👉 Images are delivered in formats like **GeoTIFF**, which say:

* “Top-left pixel = lat 12.9716, lon 77.5946”
* “Pixel size = 0.5m × 0.5m”
* “Projection = WGS84”

So we can calculate:

```
Pixel (0,0) → (12.9716, 77.5946)
Pixel (100,0) → (12.9716, 77.5956)
Pixel (0,100) → (12.9726, 77.5946)
```

That’s why if you overlay two GeoTIFFs, they “line up” correctly on a map.

**Mock Example:**

```
Image size: 2x2 pixels
Metadata: top-left=(12.9710,77.5940), resolution=0.001° per pixel
Pixels:
 [[100,200],
  [150,250]]

Pixel(0,0)=12.9710,77.5940
Pixel(0,1)=12.9710,77.5950
Pixel(1,0)=12.9720,77.5940
Pixel(1,1)=12.9720,77.5950
```

So we know exactly which ground coordinates each pixel represents.

---

# 2. How do we **find roads and buildings** in images?

This is **image recognition / computer vision**.

### 2.1 Roads

* Roads appear as **long, narrow, continuous features** (lighter or darker strips).
* Algorithms:

  * Old: edge-detection + heuristics (detect lines).
  * Modern: **Deep Learning segmentation**:

    * Input: satellite patch (e.g., 256×256 pixels).
    * Output: binary mask where `1=road, 0=non-road`.

**Example (tiny mock 5×5 grayscale image):**

```
Pixels:
[
 [200,200,200, 50, 50],
 [200,200,200, 60, 60],
 [200,200,200, 70, 70],
 [200,200,200, 80, 80],
 [200,200,200, 90, 90],
]
```

* Left 3 columns (200s) are bright → detected as **road**.
* Right 2 columns (50–90) → non-road (vegetation).

Segmentation output mask:

```
[
 [1,1,1,0,0],
 [1,1,1,0,0],
 [1,1,1,0,0],
 [1,1,1,0,0],
 [1,1,1,0,0],
]
```

Then convert this into a **vector line**:

```
LINESTRING(12.9710 77.5940, 12.9750 77.5943)
```

---

### 2.2 Buildings

* Buildings are **rectangular/square blobs** with sharp edges.
* Similar process: use CNN models trained on labeled building footprints.
* Output: polygons.

Example segmentation mask:

```
[
 [0,0,0,0,0],
 [0,1,1,1,0],
 [0,1,1,1,0],
 [0,1,1,1,0],
 [0,0,0,0,0],
]
```

→ building polygon:

```
POLYGON((12.9712 77.5942, 12.9712 77.5944,
         12.9714 77.5944, 12.9714 77.5942, 12.9712 77.5942))
```

---

# 3. What do we **do with this**?

Once we extract **roads and buildings**, we integrate them into the map system:

### 3.1 Roads

* Add new roads to the **road graph** (nodes & edges).
* Update routing engine so users can navigate on new streets.
* Detect **changes** (construction, widening, new connections).
* Improve map accuracy where OSM/commercial data is missing.

### 3.2 Buildings

* Create **building footprints** for visualization (3D view, shadows).
* Use for **address geocoding** (house numbers along building perimeters).
* Detect **new developments** → update POI database.
* Urban planning / land use classification.

### 3.3 Land use

* Classify large regions (forest, water, urban).
* Useful for background layers and analytics.

### 3.4 Final Outputs

* **Raster tiles**: pretty satellite view in Google Maps.
* **Vector data**: roads, buildings, land-use polygons → searchable, routable, indexable.

---

# 4. Example End-to-End

### Input

A 256×256 GeoTIFF image (0.5m resolution) covering Bangalore downtown.

### Process

1. Georeference says top-left = (12.9710,77.5940), bottom-right = (12.9760,77.5990).
2. Run ML segmentation model:

   * Mask out roads (white lines).
   * Mask out buildings (gray blocks).
3. Convert masks → vector shapes.
4. Merge with OSM road graph.

   * Found 2 new roads not in OSM → add edges.
   * Found 10 new buildings → add to building layer.

### Output

* Updated road graph used by routing.
* Building footprints displayed in map client.
* Satellite tile (imagery) rendered and cached.

---

# 5. Why This Matters in System Design Interviews

If asked:

* **How do we know where an image belongs?** → GeoTIFF georeferencing + metadata.
* **How to find roads/buildings?** → Computer Vision (segmentation) → vectorization.
* **What do we do with this?** → Update routing graph, POIs, tiles, and provide satellite view.

That’s the story interviewers want: **raw raster → features → integrated map system**.

---

Perfect — let’s take a **mock input dataset** (tiny, toy-sized) and walk it through the **data ingestion & processing pipeline** step by step. That way you’ll see how raw map data turns into a clean road graph, searchable POIs, and live traffic integration.

---

# 1) Mock Input Data (raw sources)

### 1.1 OSM-style road data (vector)

```
Node:
  n1: (lat=12.9716, lon=77.5946)   # Bangalore center
  n2: (lat=12.9720, lon=77.5950)
  n3: (lat=12.9730, lon=77.5960)

Way (road segments):
  w1: [n1 → n2], tags={highway=residential, name="MG Road"}
  w2: [n2 → n3], tags={highway=primary, name="MG Road", maxspeed=50}
```

### 1.2 POI data

```
poi1: {name="Coffee Day", category="cafe", lat=12.9718, lon=77.5948}
poi2: {name="Metro Station", category="transit", lat=12.9730, lon=77.5960}
```

### 1.3 Telemetry (GPS pings from users)

```
ping1: {device=abc, lat=12.9717, lon=77.5947, speed=10km/h, time=09:00}
ping2: {device=abc, lat=12.9721, lon=77.5952, speed=25km/h, time=09:01}
ping3: {device=xyz, lat=12.9728, lon=77.5959, speed=30km/h, time=09:02}
```

---

# 2) Pipeline Walkthrough with this data

## Step 1: Acquire

* We got road segments (OSM ways), POIs (business data), and GPS telemetry.
* For large systems → batch import OSM + stream ingest telemetry into Kafka.
* Here → just raw JSON/CSV.

---

## Step 2: Validate & Normalize

* Ensure all data is in **WGS84 (lat/lon)**, attributes standardized.
* Normalize roads:

  * `highway=residential → road_type=residential`
  * `highway=primary → road_type=primary, speed_limit=50`.

So roads become:

```
Edge candidate:
e1: n1 → n2, road_type=residential, name="MG Road", speed_limit=30 (default)
e2: n2 → n3, road_type=primary, name="MG Road", speed_limit=50
```

---

## Step 3: Deduplicate & Merge

* POI “Coffee Day” is unique.
* Road name “MG Road” appears on both segments → keep as one logical road but multiple edges (n1→n2, n2→n3).

---

## Step 4: Clean & Enrich

* Add **derived attributes**:

  * Distance (using Haversine formula). Suppose:

    * n1→n2 ≈ 60m
    * n2→n3 ≈ 150m
  * Travel time at speed\_limit:

    * e1: 60m @30km/h ≈ 7s
    * e2: 150m @50km/h ≈ 11s
* Add default speed\_limit=30 where missing (residential road).

---

## Step 5: Build Road Graph

Graph structure:

**Nodes:**

```
n1: 12.9716,77.5946
n2: 12.9720,77.5950
n3: 12.9730,77.5960
```

**Edges:**

```
e1: n1→n2, length=60m, base_time=7s
e2: n2→n3, length=150m, base_time=11s
```

Bidirectional? Yes, unless road is one-way. So add reverse edges too.

---

## Step 6: Indexing

* Build a **spatial index**:

  * Put POIs and nodes into geohash/quadtree for “nearest POI” queries.
  * Example: query near (12.9720, 77.5950) → finds Coffee Day (40m away).
* Build **text index**:

  * Tokenize `"Coffee Day" → {coffee, day}`
  * `"MG Road" → {mg, road}` for autocomplete.

---

## Step 7: Telemetry → Map Matching

* pings → snap to nearest road edge:

  * ping1 (12.9717,77.5947) \~ 10m from edge e1 → match to e1.
  * ping2 (12.9721,77.5952) near n2 → between e1 & e2.
  * ping3 (12.9728,77.5959) \~ 15m from edge e2 → match to e2.

Result:

```
probe_event1: edge=e1, speed=10
probe_event2: edge=e2, speed=25
probe_event3: edge=e2, speed=30
```

---

## Step 8: Traffic Aggregation

* For edge e1: speeds = \[10] → avg=10 km/h vs limit=30 km/h → congestion.
* For edge e2: speeds = \[25,30] → avg=27.5 vs limit=50 → moderate traffic.

So update edge weights:

```
e1 live_time = length / avg_speed ≈ 60m / (10km/h=2.78 m/s) = 21s (instead of 7s)
e2 live_time = 150m / (27.5km/h=7.64 m/s) = 20s (instead of 11s)
```

---

## Step 9: Routing Example

Query: route from n1 to n3.

* Static graph: n1→n2 (7s) + n2→n3 (11s) = 18s.
* Live traffic graph: n1→n2 (21s) + n2→n3 (20s) = 41s.

So ETA changes based on telemetry. 🚦

---

# 3) Final Data Outputs

### Graph DB / Binary file

```
Nodes:
  n1 (12.9716,77.5946)
  n2 (12.9720,77.5950)
  n3 (12.9730,77.5960)

Edges:
  e1: n1→n2, 60m, base=7s, live=21s
  e2: n2→n3, 150m, base=11s, live=20s
```

### POI Index

```
poi1: Coffee Day, category=cafe, location=(12.9718,77.5948)
poi2: Metro Station, category=transit, location=(12.9730,77.5960)
```

### Tile (vector, zoom 16)

```
Layer "roads": [e1, e2 with geometry polylines, name="MG Road"]
Layer "pois": [Coffee Day, Metro Station]
```

---

✅ This toy example shows how:

* Raw data → normalized → merged → enriched → indexed → graph.
* Telemetry adjusts edge weights dynamically.
* POIs are indexed for search/reverse geocoding.
* Tiles can be built from the final geometries.

---

# Hierarchical routing — deep dive (why, what, how, and when)

Nice — this is a great topic for system-design interviews. Below I’ll explain **why multiple graph levels matter**, **what problems they solve**, **how to construct each level**, **how the routing service chooses levels at query time**, and **how to stitch results together**. I’ll include concrete algorithms, data structures, preprocessing steps, maintenance/updating strategies, dynamic-traffic considerations, and a small worked example so you can explain or implement it confidently.

---

# 1) Why multiple levels? Problems hierarchical routing solves

1. **Search space explosion / performance**

   * Running A\* or Dijkstra on a full planet-scale graph is too slow and memory heavy — the search expands far more nodes than necessary for long trips.
   * Multi-level graphs reduce nodes explored by lifting the long-distance parts of a route onto a sparse graph.

2. **Different semantics at different distances**

   * For local navigation you care about minor streets, alleys, turn restrictions and driveways.
   * For inter-city travel you care about highways, interchanges, and long segments where local detail is irrelevant.
   * Using one graph mixes concerns and wastes CPU exploring irrelevant local detail for long trips.

3. **Caching & reuse**

   * Most long-distance trips share large mid-route segments (e.g., highways). A sparse mid-level graph makes it efficient to cache or precompute common highway segments and reuse them.

4. **Precomputation tradeoffs**

   * Many speedups (CH, CRP) are possible if we preprocess. Hierarchical partitioning enables cheap preprocessing per level and localizes re-processing when data changes.

5. **Scalability & memory**

   * Keeping a single full graph in memory for global routing is heavy. Per-level graphs are smaller, and you can load levels on demand or partition by region.

6. **Better update strategies**

   * Local edits (new building or road) often only affect Level 1. Level 2/3 rarely change, reducing the need to rebuild expensive artifacts frequently.

---

# 2) What each level represents (semantics & design choices)

* **Level 1 — Local graph**

  * All roads, intersections, driveways, pedestrian links.
  * High density; used for local maneuvers, last-mile navigation.
  * Good for short trips and for the “fringe” of a long trip (first/last mile).

* **Level 2 — Regional / arterial graph**

  * Major urban arterials, primary roads, main collectors, regional highways.
  * Significantly sparser: nodes are important junctions or on/off ramps.
  * Represents mid-distance connectors within metro/region.

* **Level 3 — Global / backbone graph**

  * Highways, interstates, major inter-city corridors.
  * Very sparse: nodes = major interchanges / city border entry points.
  * Used for long-haul routing across regions/countries.

> You can have more than 3 levels (multi-scale hierarchy), but 3 is a practical, easy-to-explain default.

---

# 3) How we **create** the levels — preprocessing steps

There are two main families of approaches to build hierarchical graphs:

* **A. Manual filtering / importance-based selection (simple, intuitive)**

  * Decide rules/heuristics mapping road tags to levels. E.g.:

    * `motorway`, `trunk` → Level 3
    * `primary`, `secondary` → Level 2
    * `tertiary`, `residential`, `service` → Level 1
  * Post-process: contract sequences of Level-2/3 segments forming long straight corridors into single edges (shortcuts) while recording the full underlying geometry for reconstruction.
  * Pros: simple, fast; Cons: heuristics may miss semantic importance (e.g., a long scenic local road).

* **B. Importance score & contraction (algorithmic, robust)**

  * Compute a numeric **importance score** for nodes (and/or edges) using metrics:

    * Traffic volume (telemetry), degree, betweenness centrality, road class, connectivity to other high-importance nodes.
  * Sort nodes by importance; use contraction (like CH) or multi-level partitioning to produce levels:

    * Remove (contract) low-importance nodes first to create higher-level shortcuts.
  * Result: levels naturally emerge from importance ordering.
  * Pros: principled, can incorporate telemetry/popularity; Cons: heavier preprocessing, more complex.

### Recommended practical pipeline (hybrid)

1. **Tag-based candidate assignment**: use OSM tags and business rules to assign preliminary levels.
2. **Compute importance**: for borderline cases, use traffic counts, betweenness, or centrality.
3. **Contract & shortcut generation**:

   * For Level 2 graph: contract local nodes not marked important and add shortcuts connecting important nodes while storing the underlying path.
   * For Level 3 graph: further contract Level 2 to keep only highest-importance nodes and add corresponding shortcuts.
4. **Store mappings**: for every shortcut edge, maintain `shortcut.inner_edges = [edge_id1, edge_id2, ...]` so you can reconstruct the full Level 1 path.

---

# 4) Data structures & storage for multi-level graphs

For each level `L` store:

* `L.nodes[]` (id, lat, lon, meta)
* `L.edges[]` (id, u\_node, v\_node, base\_weight, travel\_time\_func?, allowed\_modes, geom\_summary)
* `L.adj[]` adjacency lists for quick traversal
* `L.shortcut_map`: for each shortcut (edge at level > 1) store the underlying `edge_id` sequence in the next-lower level (for reconstruction)
* `L.version` metadata; `L.region` (graph partitioning)

Physical storage:

* Memory-mapped binaries per (region, level) for fast reads.
* On-disk index mapping `coord -> node_id` per level for snapping.
* Shortcut lookup table stored in object store (S3) and loaded as needed.

Indexing:

* Per-level spatial index (S2/quad) to quickly find nearby nodes for snapping and boundary detection.

---

# 5) Query-time behavior — how the service chooses levels

### Step 0 — Pre-check: distance / region heuristic

* Compute **straight-line distance** `d` between origin and destination.
* If `d` < `D_local` (a small threshold, e.g., 5–10 km) → use Level 1 only.

  * Rationale: local A\* will be fast and exact because search space is limited.
* Else compute region membership:

  * If origin.region == destination.region (same city region) and `d` moderate → use Level 1 or Level1+2 hybrid.
  * If different regions or `d` > `D_region` (e.g., > 50 km) → plan to use Level 2 and/or Level 3.

This heuristic is cheap and avoids running potentially expensive cross-level planning for short trips.

### Multi-level planning (typical flow for long trips)

1. **Snap origin/destination** to Level 1 nodes (or edges).
2. **Find nearest Level 2 entry points** from origin: run a short A\* on Level 1 locally (bounded radius) to find one or more boundary nodes that connect up to Level 2 (on-ramps or major junctions). Call these `entry_nodes`.
3. **Find nearest Level 2 exit points** near destination similarly, `exit_nodes`.
4. **Plan on Level 2 / Level 3**:

   * If the path between `entry_nodes` and `exit_nodes` is long/hops across regions, promote to Level 3 for the main middle leg (entry → exit could be a Level 3 route).
   * Run A\* (or CH/CRP) on the chosen higher-level graph to compute the mid-route.
5. **Stitch segments**:

   * `origin -> entry_node` (Level 1 local route)
   * `entry_node -> exit_node` (Level 2/3 mid-route)
   * `exit_node -> destination` (Level 1 local route)
6. **Reconstruct full Level 1 geometry** by expanding any shortcuts along the mid-route using `shortcut_map` -> sequences of lower-level edges until Level 1 edges are obtained.
7. **Postprocess**: apply turn restrictions, apply live traffic multipliers if needed (see below), generate steps.

This is sometimes called **"reach-based routing"**, **"multi-level A\*"** or **"hierarchical routing with contraction"** depending on exact preprocessing.

---

# 6) Algorithms used per-level & how they interplay

* **Local legs (Level 1)**: use A\* or Dijkstra; search area small so these are fast.
* **Mid legs (Level 2/3)**:

  * If preprocessed CH/CRP built for that level: use CH queries (fast).
  * Else use A\* on sparse graph — still fast because few nodes.
* **Inter-level planning**: either

  * Run hierarchical A\* that treats levels as different graphs and uses an admissible heuristic (e.g., straight-line distance) to guide cross-level search, or
  * Use the three-stage method above (local-entry → high-level mid → local-exit) which is easier to implement and explain.

**Important detail:** Ensure heuristics remain admissible when mixing levels so A\* remains correct. A common strategy: use optimistic estimates (free-flow speed on Level 3) in heuristic to guarantee admissibility.

---

# 7) Reconstructing full low-level path from shortcuts

* Each high-level edge may correspond to a `shortcut_id`.
* Use `shortcut_map[shortcut_id] = [edgeId_L-1_a, edgeId_L-1_b, ...]`.
* Recursively expand until you reach Level 1 edge IDs.
* Concatenate Level 1 edge geometries (`edge.geom`) to build final polyline.
* While expanding, recompute accurate travel times using live traffic where available (since shortcut weight was computed on base/level-level weight).

---

# 8) Handling turn restrictions & edge-based considerations across levels

* Turn restrictions are fundamentally local (e.g., at intersections). Ensure Level 2/3 contraction preserves turn semantics:

  * When constructing shortcuts, record not only edge sequences but also the **turn constraints** and **turn penalties** that apply within that shortcut.
  * If a pair of lower-level edges inside a shortcut includes a forbidden turn, do **not** create a shortcut that enables that forbidden maneuver.

Edge-based transformation approach:

* Convert to edge-based graph (where nodes represent edges, and transitions represent turns) before contraction to preserve turn restrictions. This is more expensive but yields correct high-level shortcuts.

---

# 9) Dynamic traffic & hierarchy: how to apply live weights

Challenges:

* Shortcuts are precomputed with base weights. Live traffic may change relative ordering of candidate routes; we must handle this without rebuilding entire hierarchy often.

Practical approaches:

1. **Query-time adjustment**:

   * Use precomputed shortcuts to get candidate high-level route.
   * Expand candidate route to Level 1 (or at least to Level 2) and re-evaluate its true cost using live data.
   * Optionally run a local search around the candidate path to find improvements.

2. **Weight buckets / time-dependent shortcuts**:

   * For heavy-duty systems, store per-edge time-of-day weight buckets or time-dependent travel-time functions.
   * Precompute multiple shortcut weights for typical time buckets (e.g., peak/off-peak) and choose appropriate precomputed weights at query-time.
   * This increases storage but improves accuracy for scheduled traffic.

3. **CRP / customizable routing**:

   * CRP was designed to allow fast weight updates (good for traffic) without full reprocessing, so combine CRP with hierarchical partitioning for dynamic updates.

Summary: start with query-time adjustment + candidate re-evaluation (simple, effective), then move to time-dependent or CRP methods if strict accuracy needed.

---

# 10) When to rebuild levels and maintenance

* **Level 1 (local)**

  * Rebuild frequently: daily or on-diff (OSM changes, imagery merges).
  * Cheap to reprocess for small regions or incremental updates.

* **Level 2 (regional)**

  * Rebuild less frequently: weekly or when many local changes affect arterial classes or when traffic patterns change significantly.
  * Incremental updates: if you add/remove a highway or major arterial, update affected partitions.

* **Level 3 (global)**

  * Rebuild rarely: monthly/quarterly or on major map vendor updates. Highways don't change very often.
  * For new inter-city links, schedule targeted updates.

Practical maintenance strategy:

* Partition graphs by geography so you can rebuild only affected partitions.
* Maintain graph `versions` and perform staged rollout: precompute new versions, run QA/regression, then atomically switch pointers.

---

# 11) Example — concrete small scenario

Mock city pair:

* Level 1: dense city graph (streets A..Z).
* Level 2: city ring road + main arterials.
* Level 3: interstate connecting City1 and City2.

Request: `origin` in City1 local street; `destination` in City2 local street (200 km apart).

Flow:

1. Snap origin/dest to Level 1 nodes.
2. Local search from origin to nearest Level 2 on-ramp (bounded A\*, maybe 1–2 km).
3. On Level 3, find inter-city corridor from City1 border node to City2 border node (CH on Level 3).
4. Local search from City2 border node to destination on Level 1.
5. Expand Level 3 shortcuts to Level 2 if needed, then to Level 1 and compute exact ETA with traffic aggregates.

Time & resources:

* Local A\*s explore hundreds of nodes.
* Level 3 CH explores a handful of nodes.
* Stitching and expansion reconstructs full route; total nodes visited far less than global A\* would.

---

# 12) Complexity & performance notes

* **Single-level A**\*: worst-case nodes expanded ∝ graph size; not feasible for global queries.
* **Hierarchical**: local legs cost small (bounded), mid leg cost small because the higher-level graph has far fewer nodes; expansions are massively reduced.
* **Memory**: storing multiple levels costs more space but each level is sparser, and you can partition/load levels on demand.

---

# 13) Pitfalls & gotchas (be ready to discuss these in interviews)

1. **Incorrect shortcuts**: naive contraction can produce shortcuts that violate turn restrictions. Solution: use edge-based contraction or record turn constraints.
2. **Traffic invalidates precomputed shortcuts**: resolved by candidate re-evaluation or time-dependent buckets.
3. **Edge weight inconsistency**: must preserve admissibility of heuristics when mixing levels — use optimistic heuristics.
4. **Reconstruction cost**: reconstructing full Level 1 path from many shortcuts can be expensive; store compressed inner-edge sequences and cache expansions.
5. **Boundary node explosion**: if partitioning is too fine or roads poorly partitioned, you may end up with too many border nodes. Use balanced partitioning algorithms (METIS, inertial partitioning).
6. **Complex turn modeling**: for complicated intersections (ramps, braided ramps), ensure correct modeling at higher levels (include ramp nodes in Level 2).

---

# 14) Interview talking points — concise script

* Start: “We use hierarchical routing (multi-level graph) to limit search space: Level 1 for local detail, Level 2 for regional arterials, Level 3 for global highways.”
* Explain creation: “Assign levels by tags + importance; contract lower-importance nodes to create shortcuts; store mapping to expand shortcuts back to Level 1.”
* Query flow: “For short trips use Level 1 A\*; for long trips do local-to-entry (L1) → mid-route (L2/L3 CH/CRP) → exit-to-destination (L1), then reconstruct full path.”
* Live traffic: “Use query-time re-weighting and candidate re-evaluation; for higher fidelity use time-dependent buckets or CRP for fast weight updates.”
* Maintenance: “Rebuild L1 frequently, L2 less so, L3 rarely; partition by geography for targeted rebuilds; version artifacts and atomically swap on deploy.”
* Tradeoffs: “Precomputation (CH) yields fast queries but complicates dynamic updates; CRP + hierarchy are better for traffic-rich environments.”

---

# 15) Pseudocode — high-level multi-level routing

```text
function route(origin, dest, mode, departure):
  o1 = snap_to_level1(origin)
  d1 = snap_to_level1(dest)
  d = haversine(origin, dest)

  if d < D_local:
    return Astar_Level1(o1, d1, mode, departure)

  // get candidate level2 entry/exit by bounded local search
  entries = bounded_Astar_Level1(o1, radius = R_entry)
  exits = bounded_Astar_Level1(d1, radius = R_entry)

  // map entries/exits to level2 nodes
  entry_l2_nodes = map_to_level2(entries)
  exit_l2_nodes = map_to_level2(exits)

  // decide if level3 needed
  if distance_between_regions(entry_l2_nodes, exit_l2_nodes) > D_region:
    // use level3 for main leg
    mid_path = route_on_level3(entry_l2_nodes, exit_l2_nodes, mode, departure)
  else:
    mid_path = route_on_level2(entry_l2_nodes, exit_l2_nodes, mode, departure)

  // stitch local legs + mid_path
  path = stitch( local_path(o1, entry), mid_path, local_path(exit, d1) )

  // expand shortcuts in path to level1 edges
  full_level1_edges = expand_shortcuts(path)

  // apply live traffic reweighting on full_level1_edges and optionally re-optimize
  final_path = refine_with_live_traffic(full_level1_edges)

  return build_route_response(final_path)
```

---

Nice — your high-level idea is basically right. I cleaned it up, fixed gaps, and expanded it into a precise, interview-ready design you can quote or implement. Below you’ll find:

* A corrected **end-to-end flow** for single-level vs hierarchical routing
* **Why** multiple levels are needed (short summary)
* Exactly **how** L1/L2/L3 are constructed (practical steps)
* How the **service decides** which graph(s) to use at query time (rules & heuristics)
* How to **find entry/exit nodes** and stitch the result back to L1 geometry
* How **live traffic, turn restrictions, caching, and updates** fit in
* Pseudocode showing the full routing flow
* Two worked examples: **Hyderabad → Karimnagar** (short/medium) and **Hyderabad → Bangalore** (long)
* Practical notes: complexity, failure modes, testing & monitoring

I’ve kept it concrete and implementation-focused.

# Hierarchical routing — corrected, detailed flow

## Goal

Return the best route (polyline + steps + ETA) between `origin(lat,lon)` and `destination(lat,lon)` quickly and correctly. Use L1 for local detail and higher levels (L2, L3) to accelerate mid/long-distance routing.

---

# 1 — Quick summary: when to use which level

* If straight-line distance `d < D_local` (e.g. 5–10 km) → run **L1 only** (local A\* / Dijkstra).
* If `D_local <= d < D_regional` (e.g. 5–250 km) or same-state: run **hybrid L1 + L2**.
* If `d >= D_regional` or crossing states/countries: run **hybrid L1 + L2 + L3** (use L3 for the long middle leg).

Thresholds (`D_local`, `D_regional`) are tunable by region and can consider real road distance estimates, not just straight-line.

---

# 2 — Why multiple graphs (short)

* Reduce search space for long trips so queries are fast (don’t expand every tiny local road across 500 km).
* Separate concerns: local accuracy (turns/driveways) vs global connectivity (highway corridors).
* Enable cheaper precomputation and localized updates: L1 changes often; L3 rarely.

---

# 3 — What L1, L2, L3 contain & how we build them

### L1 — Local (full) graph

* Contains every road segment from canonical `edges` table (residential, tertiary, service, etc.).
* Build: import OSM / vendor data → split ways into edges at intersections → compute `length`, `base_time`, `allowed_modes`.
* Use for snapping, last-mile, turn restrictions.

### L2 — Regional / arterial graph

* Contains important arterials: primary/secondary roads, state highways, major connectors, and on/off-ramps.
* Build (practical recipe):

  1. Rule-based select: keep edges whose `road_class` ∈ {primary, secondary, trunk, motorway, state\_highway}.
  2. Compute **importance** (optional): edge/node degree, traffic volume, betweenness; promote edges/nodes above threshold.
  3. **Contract** local sequences to create shortcuts between L2 nodes: run local Dijkstra inside partitions to find shortest internal paths and create `shortcut` edges mapping to underlying L1 edge sequences. Store `shortcut_map[shortcut_id] = [L1_edge_ids...]`.
* L2 is much sparser than L1.

### L3 — Global / backbone graph

* Contains only highest-level corridors: motorways, interstate highways, major country links.
* Build similarly to L2: select `motorway`, `trunk`, long-distance `national` highways; contract long continuous corridors into super-edges. Store their underpinning L2/L1 sequences.

> Store per-level graph artifacts separately (memory-mapped binaries per region/level). Keep `version` and provenance.

---

# 4 — Data model additions for hierarchy

Add these tables/columns (conceptual):

* `edges` (L1 canonical) — existing.
* `graph_level_edges(level, edge_id, u_node, v_node, base_time_s, geom_summary, is_shortcut BOOLEAN, underlying_edge_sequence JSONB)`
  (For L2/L3 edges, `underlying_edge_sequence` stores lower-level edge ids.)
* `nodes_level(level, node_id, lat, lon, is_border BOOLEAN)`
  (border nodes connect regions or levels)
* `partition` / `region` table to know which region a node/edge belongs to.

---

# 5 — Query-time decision & flow (full detail)

1. **Receive request** with origin & destination. Validate and auth.
2. **Compute approximate straight-line distance `d0`** and optionally quick lower-bound travel time (d0 / max\_speed).
3. **Map-to-region check**: find `region(origin)` and `region(dest)` (S2 cell, admin boundary).
4. **Choose routing strategy**:

   * If `d0 < D_local` and region(origin) == region(dest): do **L1-only** A\*.
   * Else if `d0 < D_regional` or same-state: do **L1→L2→L1** hybrid.
   * Else (cross-state, d0 ≥ D\_regional): do **L1→L3/L2→L1** hybrid. (Prefer L3 for the long middle leg.)
5. **Snapping**:

   * Snap origin & destination to L1 (prefer projecting onto edge then creating virtual node if not exactly on a node).
   * These snaps produce `o_L1_node`, `d_L1_node`.
6. **Find candidate entry/exit nodes on upper level**:

   * Run a **bounded A\*** (or Dijkstra limited by radius or cost) from the origin on L1 to discover a small set of candidate `entry_nodes` that are on L2/L3 (on-ramps, border nodes). Similarly bounded search near destination for `exit_nodes`.
   * This is cheap because search radius is limited (e.g., up to 5–20 km or until you hit a node flagged `connects_to_L2`).
7. **Map candidate nodes to level nodes**:

   * Each candidate L1 node maps to one or more L2 nodes (often same coordinates, or via a `lift` table).
8. **Plan middle leg on higher level**:

   * Run chosen algorithm on L2 or L3 graph to find path among `entry_l2_nodes` → `exit_l2_nodes`. Use CH/CRP/A\* depending on precomputation.
9. **Stitch**:

   * Get L1 path from `origin` → chosen `entry_node` (we already computed local search for candidates. Keep best one).
   * Append middle leg result (expand all shortcut edges into underlying L1 sequences using `underlying_edge_sequence` or recursively).
   * Append L1 path from `exit_node` → `destination`.
10. **Refine with live traffic**:

* Option A: during weight calc, apply Redis multipliers. If CH used, apply hybrid re-evaluation: get candidate route(s) from CH, expand to L1 and recompute exact travel time using live speeds then pick best.
* Option B: for heavy accuracy demands, use precomputed time-buckets (morning/evening) per shortcut.

11. **Postprocess**:

* Generate human-readable steps, recompute turn restrictions at stitch boundaries, compress consecutive same-street edges into a single step.

12. **Cache & respond**: store route in cache with TTL that depends on traffic freshness then return result.

---

# 6 — How to find the `entry/exit` nodes (detailed)

We must find a small set of likely points where the route should move up from L1 to L2 (or L2→L3).

Practical algorithm (bounded local search):

* From `o_L1_node` run a **bounded A\*** on L1 with cost budget `C_entry` or radius `R_entry` (e.g., 5–20 km or 5–15 minutes). Stop when you reach nodes flagged `connects_to_L2` (on-ramps, highway junctions) or when you exceed budget.
* Collect top-K distinct candidate nodes ordered by cost (distance/time) — e.g., K=3.
* For each candidate, compute mapping `candidate -> l2_node_id`.
* Repeat from `d_L1_node` to get `exit_candidates`.
* If no candidate found within budget, you may: increase budget (rare) or fall back to L1-only plan.

Why bounded search?

* You don’t want to run a full L1 search across the whole city. Entry/exit are typically close to origin/destination.

---

# 7 — Stitching & expanding shortcuts

* Middle leg edges at L2/L3 may be shortcuts. For each shortcut edge, load its `underlying_edge_sequence` (list of lower-level edges).
* Concatenate all sequences to produce a contiguous L1 edge sequence.
* Recompute travel times using live traffic for L1 edges to produce accurate ETA.
* If recomputed travel time deviates significantly from originally chosen candidate, optionally attempt local re-optimization: run a small local search near points of deviation.

---

# 8 — Live traffic interactions & correctness notes

* **CH problem**: CH shortcuts are precomputed using base weights. Live traffic can make a path not optimal compared to another that CH didn’t propose.
* **Practical solutions**:

  * Use CH to produce a **small set** of candidate paths quickly.
  * Expand candidate paths to L1 and re-evaluate using live edge speeds (choose best).
  * For heavy traffic sensitivity, use **CRP** (customizable routing) or time-dependent CH.
* **Privacy / safety**: require `min_samples` before applying observed speed for edge.

---

# 9 — Pseudocode: full route function (practical)

```text
function ROUTE(origin, dest, mode, departure_time):
  o_L1 = SNAP_TO_L1(origin)
  d_L1 = SNAP_TO_L1(dest)
  d0 = Haversine(origin, dest)

  if d0 < D_local and region(o_L1)==region(d_L1):
    path_L1 = ASTAR_L1(o_L1, d_L1, mode, departure_time)
    return POSTPROCESS_AND_RETURN(path_L1)

  // find entry & exit candidates on higher level
  entry_candidates = BOUNDED_SEARCH_L1(o_L1, budget=C_entry, predicate=node_connects_to_L2)
  exit_candidates = BOUNDED_SEARCH_L1(d_L1, budget=C_entry, predicate=node_connects_to_L2)

  // map to L2 nodes
  entry_l2_nodes = MAP_TO_L2(entry_candidates)
  exit_l2_nodes  = MAP_TO_L2(exit_candidates)

  // decide mid-level: if cross-region or d0 >= D_regional then use L3 else L2
  mid_level = choose_mid_level(d0, region(o_L1), region(d_L1))

  // route on mid-level graph (fast; CH/CRP/A*)
  best_mid = ROUTE_ON_LEVEL(mid_level, entry_l2_nodes, exit_l2_nodes, mode, departure_time)

  // select best entry/exit combination using combined cost (local leg cost + mid_leg_cost)
  best_combo = PICK_BEST_COMBINATION(entry_candidates, best_mid, exit_candidates)

  // reconstruct full L1 path:
  //  - if mid edges are shortcuts, expand each to underlying L1 edge sequences
  path_origin_to_entry = PATH_FROM_BOUND_SEARCH_RESULT(best_combo.entry)
  path_mid_expanded = EXPAND_SHORTCUTS_TO_L1(best_mid.path_for_combo)
  path_exit_to_dest = PATH_FROM_BOUND_SEARCH_RESULT(best_combo.exit)
  full_path_L1 = CONCAT(path_origin_to_entry, path_mid_expanded, path_exit_to_dest)

  // recompute times with live traffic and optionally re-optimize local deviations
  full_path_L1 = REFINE_WITH_LIVE_TRAFFIC(full_path_L1)

  return POSTPROCESS_AND_RETURN(full_path_L1)
```

---

# 10 — Example walk-throughs

### A. Hyderabad → Karimnagar (\~150 km — same state)

* `d0 ≈ 150 km` → falls into `D_regional` range; choose **L1+L2**.
* Snap origin/dest to L1.
* Bounded L1 search around Hyderabad finds one or more on-ramps / arterials connecting to L2 (entry candidates).
* Bounded L1 around Karimnagar finds exits into L2 (exit candidates).
* Route on L2 between those L2 nodes (A\* or CH on L2). L2 mid leg likely covers most of the trip.
* Expand L2 shortcuts to L1 edges and attach local legs. Recompute ETA using traffic.

### B. Hyderabad → Bangalore (\~570 km — cross-state)

* `d0 ≈ 570 km` → use **L1→L3→L1** (or L1→L2→L3→L2→L1 depending on how L2/L3 are defined).
* Snap to L1.
* Bounded L1 search finds nearest L3 on-ramp nodes (these are border / major interchange nodes).
* Route on L3 (very sparse) between Hyderabad-area L3 border node and Bangalore-area L3 border node (CH/CRP on L3).
* Expand L3 shortcuts (L3 → underlying L2 sequences → eventually L1) and attach local origin/destination legs.
* Recompute full ETA with live traffic and possibly re-optimize.

---

# 11 — Caching & optimizations

* **Cache** local snaps and entry/exit candidate lists per tile/region.
* **Cache** popular mid-level segments and precomputed common OD pairs.
* **Route cache** (Redis) keyed by `(snap_o, snap_d, mode, graph_version, traffic_fingerprint)` with TTL depending on traffic volatility (e.g., 30s–5m).
* **Pre-warm** memory mapped graph for hot regions.

---

# 12 — Updates & maintenance

* L1 rebuilds: frequent (daily or incremental diffs). Partitioned updates allowed.
* L2 rebuilds: less frequent (weekly) — update when major changes occur (new highways, arterials).
* L3 rebuilds: rare (monthly or on major map updates).
* For edits in L1 that affect L2/L3 shortcuts: detect affected partitions and schedule incremental rebuild of shortcuts; keep previous graph version until new passes QA.

---

# 13 — Edge cases & failure modes

* **No L2/L3 entry found within budget**: fall back to L1-only route (might be slower).
* **Traffic data missing**: use base\_time, mark `traffic_applied=false`.
* **Shortcut expansion error**: use stored `fallback_geometry` or run L1 A\* for the mid-leg (fallback).
* **Graph version mismatch** (client & server): embed graph\_version in response and handle mismatch gracefully.

---

# 14 — Complexity & perf estimates

* L1 A\* for local trips: expands O(k) nodes where k << city size due to heuristic & bounded search.
* Mid-level routing on L2/L3: operates on sparse graph with orders of magnitude fewer nodes — queries are fast (ms–tens ms).
* Total cost = cost(local-entry) + cost(mid) + cost(local-exit) + cost(expansion). Expansion & recompute dominate only if many shortcuts need expansion.

---

# 15 — Testing & monitoring you should implement

* **Unit**: verify snippet of graph contraction, shortcut correctness, underlying\_edge\_sequence integrity.
* **Integration**: compare hierarchical route vs L1-only A\* for correctness on sample O/D pairs (should match or be close).
* **Regression**: set of O/D pairs across distances; verify ETA/distance bounds after each build.
* **Metrics**: query latency, nodes-expanded L1/L2/L3, shortcut expansion time, cache hit ratio, traffic stale ratio.

---

# Final polished checklist (for interviews / implementation)

* Define thresholds `D_local`, `D_regional` per region (use empirical tests).
* Implement L1 graph first and correctness (turn restrictions, edge-based model).
* Build L2 via rule-based selection + contraction shortcuts (store underlying L1 sequences).
* Build L3 from motorways + contract long corridors.
* Implement bounded candidate searches for entry/exit nodes on L1.
* Implement mid-level routing with CH/CRP for speed.
* Expand mid-level shortcut edges to L1 and recalc times with live traffic.
* Add caching & robust fallbacks.
* Version graph artifacts and implement staged deploys + QA.

---

# Map tile serving & caching — deep dive (low-level, end-to-end)

Excellent — here’s a thorough, practical guide to **map tiles**: where the raw data comes from, how tiles (vector & raster) are generated, how tile requests are handled and cached, how tiles are stored and invalidated, and operational concerns (monitoring, cost, performance). I’ll include concrete tools/commands, storage layouts, formats, and implementation patterns you can use in production or in interviews.

---

# 1. What is a “tile” and tiling scheme basics

* A **tile** is a small pre-cut piece of the map at a particular zoom level. Standard scheme: `/{z}/{x}/{y}` (Web Mercator / XYZ).
* **Projection**: Web maps typically use **Web Mercator (EPSG:3857)**. Converting source data (WGS84 lat/lon) into EPSG:3857 is required for tiles.
* **Tile size**: usually 256×256 or 512×512 pixels (512 for high-DPI/retina, or use scale parameter `@2x`).
* **Zoom levels**: z = 0 (whole world) up to 22+ depending on dataset. Each zoom doubles resolution.
* **Vector tiles**: encoded (protobuf) Mapbox Vector Tile (MVT) format — contain geometries + attributes, client renders them.
* **Raster tiles**: PNG/WebP/JPEG images — pre-rendered map imagery (or satellite).

Tile URL pattern examples:

* Vector: `https://tiles.example.com/vector/{z}/{x}/{y}.pbf?style=v1`
* Raster: `https://tiles.example.com/raster/{z}/{x}/{y}.png`

---

# 2. Data sources (where tile data comes from)

1. **Vector sources**

   * Canonical vector DB (PostGIS) with `nodes`, `edges`, `pois`, `building_footprints`.
   * Third-party vendor vector data (licensed).
   * ML-extracted features (satellite → road/building vectorization).
2. **Raster / imagery**

   * Satellite imagery (GeoTIFF/COG) from providers (Maxar, Planet, Sentinel).
   * Aerial imagery (airplanes / drones).
   * Pre-rendered styled raster tile archives.
3. **Supplemental**

   * Label/attribution data, traffic overlays, weather overlays.
4. **Metadata**

   * Tile index / metadata DB listing tiles generated, versions, generation timestamps.

---

# 3. Two tile types: Vector vs Raster — pros & cons

* **Vector tiles (MVT)**

  * Pros: small size, client-side styling, single source for many styles, less bandwidth for zoomed-in maps.
  * Cons: requires client rendering (WebGL / vector render engine), slightly more CPU on client.
* **Raster tiles**

  * Pros: instant to display, compatible with all clients.
  * Cons: large bandwidth, style changes require re-rendering server-side, heavy for satellite imagery.

Most modern systems serve vector tiles for the base map and raster tiles for satellite imagery.

---

# 4. How tiles are generated — pipeline overview

## 4.1 Steps for vector tiles

1. **Extract features** from PostGIS (or GeoJSON source) for the target bounding box (tile extent) and zoom.
2. **Simplify / generalize** geometries for the zoom level (reduce vertex counts). Use `ST_SimplifyPreserveTopology` or tippecanoe’s built-in thinning.
3. **Quantize** coordinates to tile coordinate space to keep sizes small and ensure consistent rendering (`extent` field in MVT; e.g., 4096 grid).
4. **Encode** features into Mapbox Vector Tile (protobuf) with layers (roads, labels, pois, buildings).
5. **Compress** (gzip) and write `.pbf` tile file.

Tools:

* `tippecanoe` — build MBTiles (multi-zoom) from GeoJSON.

  * Example:

    ```
    tippecanoe -o tiles.mbtiles -zg --drop-densest-as-needed --extend-zooms-if-still-dropping features.geojson
    ```
* `ogr2ogr` + `vtzero` or `tilelive` + `tilelive-bridge` for custom generation.
* Custom generator: iterate tiles, run SQL `ST_Transform` + `ST_AsMVT` (PostGIS 3.0+ supports `ST_AsMVT`), directly produce MVT blobs.

Example PostGIS SQL to produce a single MVT tile (conceptual):

```sql
WITH bounds AS (
  SELECT ST_TileEnvelope(z, x, y) AS geom
)
SELECT ST_AsMVT(tile, 'roads', 4096, 'geom') FROM (
  SELECT id, ST_AsMVTGeom(geom, bounds.geom, 4096, 256, true) AS geom, name
  FROM roads, bounds
  WHERE ST_Intersects(roads.geom, bounds.geom)
) AS tile;
```

## 4.2 Steps for raster tiles (satellite or rendered maps)

Two main modes:

* **Pre-rendered raster tiles** (render vector into raster at many zooms).
* **On-the-fly / render-on-demand** (render requested tiles dynamically).

Pre-rendered:

1. **Rasterize** vector data into images using Mapnik, Mapbox Studio, or Mapbox GL Native server-side rendering at each tile zoom. For satellite imagery, produce PNG/JPEG tiles from COG or pre-rendered imagery.
2. **Tile pyramid**: generate images for the whole zoom pyramid (z0..zN). Tools: `gdal2tiles.py`, `Mapnik`, `TileMill`, `mb-util`.
3. **Compress & optimize** images (PNG8, PNGQuant, WebP).
4. **Push to object storage (S3)** in `/{z}/{x}/{y}.png` layout.

On-demand:

* Render tile using server-side rendering engine when requested (Mapnik, Mapbox GL Native). Cache result in CDN/S3.

Satellite raster generation (COG -> tiles):

* Use GDAL: `gdal_translate` to COG, then `gdal2tiles.py` or `rio-tiler` to produce z/x/y tiles.
* Example COG creation:

  ```
  gdal_translate -of COG input.tif output.cog.tif
  gdal2tiles.py -z 0-18 output.cog.tif tiles/
  ```

---

# 5. Tile generation strategies (batch vs incremental vs on-demand)

### Batch (full rebuild)

* Pros: deterministic, can be heavily optimized, good for initial builds.
* Cons: expensive for global datasets; long time to rebuild.

### Incremental (diff-driven)

* Only regenerate tiles that are affected by data changes.
* Steps:

  * Determine changed geometries (changed edges/POIs).
  * Compute tile cover (which tile z/x/y this geometry intersects for target zooms).
  * Regenerate only those tiles.
* Important for frequent updates (OSM diffs, POI updates).
* Use spatial indexing and precomputed reverse mapping `edge_id -> affected_tiles`.

### On-demand (render at request)

* Render tile if missing; respond and cache result downstream.
* Use when storage is expensive or for low-traffic areas.
* Needs strong caching and throttling to avoid overload.

Practical systems often combine: batch pre-generate popular tiles; incremental updates when data changes; on-demand rendering for cold tiles.

---

# 6. Tile storage & layout (S3 / object store best practices)

* Store tiles in object store (S3/GCS) in a predictable path: `s3://tiles/{type}/{style}/{z}/{x}/{y}.{ext}`.

  * Examples:

    * `s3://tiles/vector/v1/12/1203/1534.pbf`
    * `s3://tiles/raster/satellite/14/1203/1534.webp`
* Use versioning in paths: include `style_vX` or `graph_version` in the path to enable atomic swaps (no need to purge CDN aggressively).
* Store MBTiles (SQLite) for offline or batch packaging: `tiles-{region}.mbtiles`.
* Keep metadata DB/table:

  ```sql
  CREATE TABLE tile_metadata (
    style TEXT,
    z INT, x INT, y INT,
    s3_key TEXT,
    generated_at TIMESTAMP,
    version TEXT,
    PRIMARY KEY(style,z,x,y)
  );
  ```
* For vector tiles, store small indexes for high-frequency tiles.

---

# 7. Tile request flow & serving architecture

### 7.1 Typical architecture

Client → CDN (edge) → CDN origin (S3) or Tile server / Render-on-demand service → Origin DB (metadata) / PostGIS (if rendering).

Two main designs:

* **CDN + S3 origin**: static tiles (pre-generated) stored in S3 with CDN in front for global caching. Very cheap and fast.
* **CDN + Tile server**: CDN caches tiles; if miss, forward to tile server that may render on-demand or fetch from S3.

### 7.2 Request handling steps (vector tile)

1. Client requests `GET /vector/style_v3/14/1203/1534.pbf`.
2. CDN edge checks cache. If cached and valid (based on Cache-Control/ETag), it serves.
3. On miss, CDN queries origin — typically S3 (static tile) or tile server endpoint.
4. If tile exists in S3, return with headers (Cache-Control, ETag, Content-Encoding: gzip). CDN caches per TTL.
5. If tile missing and origin is a tile server, server:

   * Looks up metadata: which data to include, style.
   * Runs SQL `ST_AsMVT` or calls `tippecanoe` generated MBTiles to get tile.
   * Saves tile to S3 (optional), returns tile to CDN to cache.
6. Client renders tile.

### 7.3 HTTP headers & caching control

* `Cache-Control: public, max-age=31536000, immutable` for versioned tiles (include style/version in path).
* For non-versioned tiles: `Cache-Control: public, max-age=60, stale-while-revalidate=300`.
* Use `ETag` and `If-None-Match` to support conditional GETs for validation.
* For vector tiles gzipped, set `Content-Encoding: gzip` and `Vary: Accept-Encoding`.

---

# 8. Caching strategies & invalidation

### Basic caching tiers

1. **Client-level**: browser cache (Cache-Control + ETag).
2. **CDN edge**: first responder for requests.
3. **CDN regional / origin cache**: S3 or tile server caches.

### TTL & invalidation

* **Immutable versioned tiles**: set very long `max-age` (e.g., 1 year) and update by changing version in path when data/styling changes.
* **Mutable tiles**: shorter TTL + `stale-while-revalidate` to reduce tail latency.
* **Invalidation strategies**:

  * **Versioning** (preferred): include `style_version` and `data_version` in tile path; deploy new version atomically → no invalidation required.
  * **Purge/invalidate**: call CDN invalidation API (costly at scale).
  * **Tile-level invalidation**: push list of changed tile keys and purge specific keys (cheaper).
  * **Cache-busting query string**: change style parameter; but CDNs may not cache query strings uniformly.

### Incremental invalidation workflow

1. In ingestion pipeline, compute changed geometry → compute affected tiles for relevant zooms.
2. Generate list of tile keys to invalidate.
3. Either:

   * Regenerate tiles and overwrite S3 objects (if using versioned path, push new path and update pointer), or
   * Call CDN purge for affected keys.

Prefer versioned tiles to avoid heavy CDN purge costs.

---

# 9. Preventing cache stampede & render-on-demand throttling

* **Locking**: use a distributed lock (Redis) per tile key when rendering on-demand to avoid N workers rendering same tile simultaneously.
* **Queueing / rate-limiting**: limit tile generation concurrency.
* **Stale-while-revalidate**: serve stale cached tile while background worker regenerates a fresh one.
* **Pre-warming**: after pushing new version, pre-generate and push hot tiles to CDN (simulate popular requests) to warm caches.

Example lock pattern:

1. Worker receives cache-miss.
2. Try set Redis key `render-lock:{tile}` with short TTL via `SETNX`.
3. If success: render tile, write to S3, release lock.
4. If fail: wait or return a placeholder tile (low-res) and schedule background job.

---

# 10. Tile generation performance & parallelization

* Parallelize tile generation by **tilespace partitioning** (z/x ranges or tile queues).
* Use batch jobs on Kubernetes or Cloud Batch (spot instances) to rasterize/render many tiles in parallel.
* Use map-sharding and per-region workers to distribute load.
* Chunking: process tiles per region or per zoom band (e.g., low zooms first).
* Use tippecanoe with `--read-parallel` or `--processes` flags; `tilelive` pipelines often support streaming.

---

# 11. Vector tile specifics — schema & size optimizations

* **Layers**: group features by type (roads, labels, buildings, water) — clients can selectively render layers.
* **Attributes**: include only necessary attributes. Keep attributes small: e.g., `name`, `class`, `id`, `oneway`.
* **Quantization & extent**: use tile extent (e.g., 4096 grid) to quantize coordinates — reduces size.
* **Simplification per zoom**:

  * At lower zooms (z0..z8) aggressively simplify geometries and drop minor roads.
  * At higher zooms (z12+) include more detail.
* **Drop densest features**: tippecanoe's `--drop-densest-as-needed` helps meet MBTiles size and tile size targets.
* **Compression**: gzip PBF; set proper headers.

---

# 12. Satellite tiles (raster) — special concerns

* Use **Cloud-Optimized GeoTIFF (COG)** for storage; tile servers or CDN can serve partial requests via range-requests.
* **Orthorectify** and apply cloud masks. Preprocess: color correction, pan-sharpening, mosaicking.
* High-res imagery huge size — serve via CDN and use tile caching aggressively.
* Use formats like **WebP** for better compression on clients that support it.
* Consider tiling resolution tradeoffs — high zooms expensive.

---

# 13. Security & access control

* Public tiles often open; premium tiles (satellite, high-res) behind API keys or signed URLs.
* Signed tile URLs: generate expiring signed URLs for CDN/S3 access (CloudFront signed URLs).
* Rate-limit tile endpoints to prevent abuse.

---

# 14. Metrics & monitoring

Track:

* Tile request QPS by style & zoom.
* Cache hit ratio (edge & origin).
* 95th/99th latency for tile responses.
* Tile generation failures and queue backlog.
* Storage egress & S3 usage.
* Cost by region & by tile type (satellite/raster expensive).

Set alerts on:

* Cache hit ratio drops (indicates churn or bad caching).
* Spike in origin hits (indicates cache misses / purge storm).
* Tile generation job failures.

---

# 15. Cost & operational tradeoffs

* Pre-generate all tiles: high storage cost, low runtime cost.
* On-demand rendering: low storage, higher compute and latency.
* CDN caching reduces origin egress cost; aggressive versioning reduces purge costs.
* Satellite tiles (high-res) are heavy — consider only serving limited areas at highest zoom or require auth/paywall.

---

# 16. Example end-to-end commands & snippets (practical)

### Create vector tiles from PostGIS using `ST_AsMVT` (per tile)

```sql
-- bounds table computed from z/x/y
WITH
  tile AS (SELECT ST_TileEnvelope(z, x, y) AS geom)
SELECT ST_AsMVT(q, 'roads', 4096, 'geom') FROM (
  SELECT id, ST_AsMVTGeom(roads.geom, tile.geom, 4096, 256, true) AS geom, name, class
  FROM roads, tile
  WHERE ST_Intersects(roads.geom, tile.geom)
) q;
```

### Build MBTiles with tippecanoe

```bash
# create GeoJSON from PostGIS (example)
ogr2ogr -f GeoJSONSeq /vsistdout/ -sql "SELECT id, ST_AsGeoJSON(geom) as geometry, name FROM roads" PG:"dbname=maps" | \
  tippecanoe -o roads.mbtiles --drop-densest-as-needed --read-parallel
```

### Create COG and produce tiles

```bash
gdal_translate -of COG raw.tif output.cog.tif
gdaladdo -r average output.cog.tif 2 4 8 16
gdal2tiles.py -z 0-18 output.cog.tif /tmp/tiles
# upload /tmp/tiles to s3://tiles/satellite/{z}/{x}/{y}.png
```

### S3 path layout suggestion

```
s3://maps-tiles/
  vector/style_v3/{z}/{x}/{y}.pbf
  raster/satellite_v1/{z}/{x}/{y}.webp
  mbtiles/{region}.mbtiles
  metadata/tile_manifest_{version}.json
```

---

# 17. Tile metadata & change feed (incremental updates)

* Maintain `tile_manifest` per version listing `z/x/y` tiles and generation time.
* In ingestion pipeline:

  * On geometry change → compute tile cover (for zooms of interest).
  * Emit messages to Kafka `tiles.regen` with tile keys.
  * Tile workers consume and regenerate tiles; upload to S3 and optionally publish `tiles.updated` events for cache invalidation.

Tile cover calculation:

* For geometry g and zoom z:

  * Use `ST_TileEnvelope` functions and `ST_TileIntersects` or compute tile bounding box with tile coordinate math (lon/lat → tile x/y).

---

# 18. Client considerations

* Include `style_version` in tile URLs so client and server are consistent.
* Client should use vector rendering (Mapbox GL Web, MapLibre) to take full advantage.
* For offline: provide MBTiles exports (select bbox & zoom range), signed download links.

---

# 19. Checklist — implementation roadmap (practical)

1. Decide tile types (vector for base, raster for satellite), tile size, zoom range.
2. Create canonical vector data pipeline (PostGIS → features).
3. Implement vector tile generation (Tippecanoe or PostGIS ST\_AsMVT).
4. Build raster tile pipeline for imagery (COG → gdal2tiles).
5. Upload tiles to S3 with versioned path scheme.
6. Put CDN (CloudFront) in front and set caching headers.
7. Implement incremental regeneration: detect changed geometries → compute tile cover → push to queue.
8. Implement tile workers with locking & retry for regen and S3 upload.
9. Implement cache invalidation strategy (pref: versioned paths; fallback: per-tile CDN purge).
10. Monitor metrics, optimize tile size & attributes to meet latency/cost targets.

---
