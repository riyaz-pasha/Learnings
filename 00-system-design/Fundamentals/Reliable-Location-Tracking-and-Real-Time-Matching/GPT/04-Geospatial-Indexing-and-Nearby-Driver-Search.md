# Chapter 4 — Geospatial Indexing and Nearby Driver Search

So far, we've built a system that continuously receives driver locations and stores their latest positions in Redis.

Now comes the interesting question:

When a customer requests a ride, how do we find nearby drivers without searching through millions of drivers?

We'll approach this from basic geometry, gradually introduce spatial indexing algorithms, and finally build the actual nearby-driver search algorithm.

We'll cover:

1. The geometry behind latitude and longitude.
2. Why calculating the distance to every driver doesn't scale.
3. Radius searches and bounding boxes.
4. Geohash and its boundary problem.
5. Quadtrees.
6. H3 and S2.
7. How to select geographical cells correctly.
8. How Redis GEO fits into the picture.
9. How to avoid missing nearby drivers.

# 4.1 First, understand what we're actually searching for

Imagine the customer requests a ride.

```
Customer:
    Latitude  = 17.3850
    Longitude = 78.4867

Search radius = 3 km
```

Our live location index contains:

```
D101 -> (17.3855, 78.4871)
D102 -> (17.3900, 78.4900)
D103 -> (17.4200, 78.5100)
D104 -> (17.3860, 78.4870)
D105 -> (17.3500, 78.4600)
```

We need to return only the drivers inside the 3 km radius.

Visually:

\#chatgpt-mermaid-\_r_3u3\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3u3\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3u3\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3u3\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3u3\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u3\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3u3\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3u3\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3u3\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3u3\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3u3\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3u3\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u3\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u3\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3u3\_ p{margin:0;}#chatgpt-mermaid-\_r_3u3\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u3\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u3\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u3\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3u3\_ .label text,#chatgpt-mermaid-\_r_3u3\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u3\_ .node rect,#chatgpt-mermaid-\_r_3u3\_ .node circle,#chatgpt-mermaid-\_r_3u3\_ .node ellipse,#chatgpt-mermaid-\_r_3u3\_ .node polygon,#chatgpt-mermaid-\_r_3u3\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3u3\_ .rough-node .label text,#chatgpt-mermaid-\_r_3u3\_ .node .label text,#chatgpt-mermaid-\_r_3u3\_ .image-shape .label,#chatgpt-mermaid-\_r_3u3\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3u3\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3u3\_ .rough-node .label,#chatgpt-mermaid-\_r_3u3\_ .node .label,#chatgpt-mermaid-\_r_3u3\_ .image-shape .label,#chatgpt-mermaid-\_r_3u3\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3u3\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3u3\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u3\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u3\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3u3\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3u3\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3u3\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3u3\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3u3\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3u3\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3u3\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u3\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u3\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3u3\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u3\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3u3\_ .icon-shape,#chatgpt-mermaid-\_r_3u3\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3u3\_ .icon-shape p,#chatgpt-mermaid-\_r_3u3\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3u3\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3u3\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3u3\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3u3\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3u3\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3u3\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3u3\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3u3\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3u3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u3\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3u3\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3u3\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3u3\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u3\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3u3\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3u3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u3\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3u3\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3u3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u3\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3u3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u3\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3u3\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3u3\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3u3\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3u3\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3u3\_ .node rect,#chatgpt-mermaid-\_r_3u3\_ .node circle,#chatgpt-mermaid-\_r_3u3\_ .node ellipse,#chatgpt-mermaid-\_r_3u3\_ .node polygon,#chatgpt-mermaid-\_r_3u3\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3u3\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3u3\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3u3\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3u3\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u3\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Customer Pickup LocationDefine 3 km Search RadiusFind Geographic CandidatesCalculate / Verify ExactDistanceFilter Fresh LocationsFilter Available DriversReturn Nearby Candidates

Notice that our query is not simply:

```
Find drivers whose latitude and longitude are close.
```

It is:

```
Find drivers whose geographical distance from the pickup
is less than or equal to the specified radius.
```

That distinction is important.

# 4.2 Why can't we just calculate the distance to every driver?

The simplest possible algorithm is:

```
for (Driver driver : allDrivers) {
    double distance = calculateDistance(
        customerLocation,
        driver.location()
    );

    if (distance <= radiusKm) {
        nearbyDrivers.add(driver);
    }
}
```

This is a brute-force approach.

Suppose:

```
Total online drivers = 1,000,000
Ride requests/sec     = 1,000
```

Every ride request checks one million drivers.

Therefore:

\\[ 1,000 \times 1,000,000 = 1,000,000,000 \\]

We are potentially performing one billion distance calculations per second.

Even if each calculation is relatively inexpensive, this is an unnecessary amount of work.

The fundamental problem is:

We are searching geographically without using geographical structure.

Think about a physical city.

If someone asks:

> Find restaurants within 2 km of this location.

You wouldn't inspect every restaurant in the entire country.

You'd first identify the relevant neighbourhoods and then inspect the restaurants in those neighbourhoods.

We need the same idea for drivers.

This leads us to spatial indexing.

# 4.3 Latitude and longitude are not ordinary X and Y coordinates

Before learning spatial indexes, we need to understand a common mistake.

We receive:

```
Latitude  = 17.3850
Longitude = 78.4867
```

Can we calculate distance using ordinary Euclidean geometry?

Something like:

\\[ d = \sqrt{(x_2-x_1)^2+(y_2-y_1)^2} \\]

Not directly.

Why?

Because latitude and longitude are angular coordinates on the surface of the Earth.

They are not distances measured in metres.

For example:

- One degree of latitude corresponds to approximately 111 km.
- One degree of longitude corresponds to approximately \\(111 \times \cos(\text{latitude})\\) km.

This means that one degree of longitude represents different physical distances at different latitudes.

At the equator:

```
1 degree longitude ≈ 111 km
```

At latitude 60°:

```
1 degree longitude ≈ 55.5 km
```

Therefore, treating latitude and longitude as ordinary Cartesian coordinates can produce incorrect distance calculations.

## How do we calculate geographical distance?

For most short-distance matching applications, the Haversine formula is a common choice.

It calculates the great-circle distance between two points on an approximately spherical Earth.

Given:

```
Point A = (lat1, lon1)
Point B = (lat2, lon2)
```

First convert degrees to radians.

\\[ \phi_1 = lat_1 \times \frac{\pi}{180} \\]

\\[ \phi_2 = lat_2 \times \frac{\pi}{180} \\]

\\[ \Delta\phi = \phi_2-\phi_1 \\]

\\[ \Delta\lambda = (lon_2-lon_1)\times\frac{\pi}{180} \\]

Then:

\\[ a = \sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2) \sin^2\left(\frac{\Delta\lambda}{2}\right) \\]

\\[ c = 2\arctan2(\sqrt{a},\sqrt{1-a}) \\]

Finally:

\\[ \boxed{d = R \times c} \\]

Where:

- \\(R\\) is the Earth's approximate mean radius, 6,371 km.
- \\(d\\) is the great-circle distance.

You don't need to memorize this formula.

Understand its purpose:

It converts two geographic coordinates into an approximate physical distance along the Earth's surface.

For higher-precision geodesic calculations, we can use ellipsoidal Earth models rather than assuming a sphere.

For our driver-matching example, Haversine is usually an appropriate starting point.

# 4.4 The first optimization — Bounding boxes

Let's return to our customer.

```
Customer:
(17.3850, 78.4867)

Radius:
3 km
```

Our actual search area is a circle.

But searching a rectangular area is easier for many spatial indexing algorithms.

So we first create a bounding box around the circle.

Visually:

```

           Bounding Box
    +-------------------------+
    |                         |
    |       . . . . .         |
    |    .             .      |
    |   .               .     |
    |  .       C         .    |
    |   .               .     |
    |    .             .      |
    |       . . . . .         |
    |                         |
    +-------------------------+

    C = Customer pickup
    Circle = Actual 3 km search area
    Rectangle = Larger candidate search area

```

The rectangle contains the entire circle.

Some points inside the rectangle will be outside the circle.

That's okay.

The rectangle is just our first filtering stage.

We will perform exact distance validation afterward.

## How do we calculate the bounding box?

Let:

```
Customer latitude  = φ
Customer longitude = λ
Search radius      = r
Earth radius       = R
```

Convert the radius into an angular distance:

\\[ \delta = \frac{r}{R} \\]

Then:

\\[ \text{MinLatitude} = \phi-\delta \\]

\\[ \text{MaxLatitude} = \phi+\delta \\]

Longitude requires a correction for latitude:

\\[ \Delta\lambda = \arcsin\left(\frac{\sin(\delta)}{\cos(\phi)}\right) \\]

Then:

\\[ \text{MinLongitude} = \lambda-\Delta\lambda \\]

\\[ \text{MaxLongitude} = \lambda+\Delta\lambda \\]

All angles here are in radians.

For small urban search radii, an approximation is also commonly used:

\\[ \Delta\lambda \approx \frac{\delta}{\cos(\phi)} \\]

This is adequate for many small-distance calculations, but the exact bounding-box implementation should handle edge cases.

### Important edge cases

1. Near the poles, longitude bounds become problematic.
2. Near the International Date Line, longitude may wrap from +180° to -180°.
3. Latitude bounds must never exceed -90° or +90°.
4. Large search radii need more careful spherical geometry.

For a city-based ride-hailing application, polar edge cases are usually not operationally important, but longitude wrapping can matter for a globally deployed system.

## Why is this useful?

Suppose we have one million drivers.

After calculating the bounding box, our spatial index may narrow the search to 500 candidates.

We then perform exact distance calculations on those 500 candidates.

Instead of:

```
1,000,000 distance calculations
```

We might need:

```
500 distance calculations
```

The actual number depends on the geographical distribution and index.

This is the fundamental idea behind spatial indexing:

First eliminate obviously irrelevant geographical areas. Then perform accurate calculations on the remaining candidates.

# 4.5 Spatial indexing — Divide the world into cells

Now let's take the next step.

Imagine dividing the geographical space into cells.

```
+-------+-------+-------+-------+
|       |       |       |       |
|   A   |   B   |   C   |   D   |
|       |       |       |       |
+-------+-------+-------+-------+
|       |       |       |       |
|   E   |   F   |   G   |   H   |
|       |       |       |       |
+-------+-------+-------+-------+
|       |       |       |       |
|   I   |   J   |   K   |   L   |
|       |       |       |       |
+-------+-------+-------+-------+
```

Every driver belongs to a geographical cell according to their location.

For example:

```
D101 -> Cell F
D102 -> Cell F
D103 -> Cell G
D104 -> Cell B
D105 -> Cell J
```

Now suppose our customer is in Cell F.

Instead of searching the entire world, we can search Cell F and the cells around it.

```
Nearby cells:

+-------+-------+-------+
|   A   |   B   |   C   |
+-------+-------+-------+
|   E   |   F   |   G   |
+-------+-------+-------+
|   I   |   J   |   K   |
+-------+-------+-------+
```

This is a simple spatial partitioning approach.

However, it raises an important question:

How do we convert latitude and longitude into cell identifiers?

There are several approaches.

Let's understand the major ones.

# 4.6 Geohash — Convert coordinates into a geographical string

Geohash is one of the easiest spatial indexing systems to understand.

Its main idea:

> Convert a geographical coordinate into a string representing a geographical cell.

For example:

```
Latitude:  17.3850
Longitude: 78.4867

Geohash:   te7...
```

The exact string depends on the precision.

You don't need to calculate the geohash manually in production. Libraries exist for this.

But let's understand how it works.

## Step 1: Divide the longitude range

Longitude ranges from:

\\[ -180^\circ \text{ to } +180^\circ \\]

First, split the interval in half.

```
-180                 0                 +180
 |-------------------|-------------------|
        West                  East
```

Suppose our longitude is positive.

We choose the eastern half.

Now:

```
0                   +180
|--------------------|
```

Split again.

```
0          90          180
|-----------|-----------|
      W          E
```

Our longitude is approximately 78.49°, so we choose the western half of this interval.

We keep repeating the process.

Each step narrows the geographical interval.

## Step 2: Do the same for latitude

Latitude ranges from:

\\[ -90^\circ \text{ to } +90^\circ \\]

We repeatedly split the interval and select the half containing our coordinate.

## Step 3: Interleave the longitude and latitude decisions

Geohash alternates between longitude and latitude subdivision decisions.

Conceptually:

```
Longitude decision -> 1
Latitude decision  -> 1
Longitude decision -> 0
Latitude decision  -> 1
Longitude decision -> 0
...
```

We get a sequence of binary bits.

Those bits are encoded into a Base32 string.

That string is our geohash.

For example:

```
te7...
```

The exact encoding is not important to memorize.

Understand what the string represents:

A geohash is a compact hierarchical identifier for a rectangular geographical cell.

## Step 4: Why is geohash hierarchical?

This is the really useful property.

Suppose we have:

```
Geohash A = te7
Geohash B = te7k
Geohash C = te7kp
```

A longer geohash represents a smaller geographical area.

The shorter prefix represents a larger geographical area containing the more specific cell.

Think about file paths:

```
India/
    Andhra Pradesh/
        Vizianagaram/
            Area/
```

A shorter prefix represents a larger area.

A longer prefix narrows the location.

This makes geohash useful for hierarchical geographic partitioning.

# 4.7 Choosing geohash precision

A geohash's length determines the cell size.

Approximate dimensions vary by latitude and longitude.

Here is an illustrative reference:

| Geohash length | Approximate cell dimensions |
| -------------- | --------------------------- |
| 4              | 20 km × 20 km               |
| 5              | 4.9 km × 4.9 km             |
| 6              | 1.2 km × 0.61 km            |
| 7              | 153 m × 153 m               |
| 8              | 38 m × 19 m                 |

These are approximate dimensions, not universal fixed sizes.

Notice something interesting:

Geohash cells become increasingly narrow in an alternating manner because longitude and latitude bits are interleaved.

Now think about our matching radius.

```
Search radius = 3 km
```

Would a geohash precision of 7 be appropriate?

Probably not as our only search unit.

A cell is approximately 153 metres across.

Searching a 3 km radius would involve many cells.

Using precision 5 may reduce the number of cells, but individual cells contain more drivers.

There is a trade-off:

| Smaller cells                     | Larger cells                  |
| --------------------------------- | ----------------------------- |
| Fewer drivers per cell            | More drivers per cell         |
| More cells to search              | Fewer cells to search         |
| Smaller search result per cell    | Larger search result per cell |
| More boundary-management overhead | More candidate filtering      |

There is no universally best precision.

We choose based on:

- Typical matching radius.
- Driver density.
- Query frequency.
- Desired latency.
- Storage distribution.
- Cell occupancy.

# 4.8 The famous geohash boundary problem

This is extremely important.

Imagine two drivers:

```
D101 -> Cell A
D102 -> Cell B
```

They are geographically only 20 metres apart.

But they lie on opposite sides of the cell boundary.

```
+------------------+------------------+
|                  |                  |
|       D101       |       D102       |
|                  |                  |
|                  |                  |
+------------------+------------------+
                   ^
               Cell boundary
```

Suppose a customer requests a ride near D102.

We search only Cell A because we think it is nearby.

We miss D102.

This is a false negative.

That is unacceptable for nearby-driver discovery.

### Solution: Search neighboring cells

When searching a geohash cell, don't inspect just that cell.

Inspect all cells that intersect the actual search region.

For a simple example, this might include:

```
       North
   NW   N   NE

    W   C    E

   SW   S   SE
```

Where C is the customer's cell.

But don't blindly assume that searching the eight immediate neighbors is always sufficient.

Why?

The number of cells needed depends on:

- Search radius.
- Geohash precision.
- The customer's position inside its cell.
- Cell dimensions.
- Latitude.

For example, a 10 km radius might require more than one ring of neighboring cells.

The safe rule is:

Every cell that intersects the requested search area must be included in candidate discovery.

Then we perform exact distance filtering.

### Another important problem: Cell sizes vary geographically

Because longitude distances depend on latitude, a geohash cell's physical width changes as we move toward the poles.

So cell selection must account for the query latitude.

This is why using a fixed neighbor count globally can become incorrect.

# 4.9 How do we actually query using geohash?

Imagine our database stores:

```
geohash -> driver IDs
```

For example:

```
te7k -> D101, D102, D103
te7m -> D104, D105
te7q -> D106, D107
te7p -> D108
```

The customer's search area intersects:

```
te7k
te7m
te7q
te7p
```

Our query becomes:

```
1. Determine intersecting geohash cells.
2. Fetch drivers from those cells.
3. Retrieve their latest accepted locations.
4. Calculate exact geographical distance.
5. Discard drivers outside the radius.
6. Apply freshness and availability filtering.
7. Rank remaining drivers.
```

Pseudocode:

```
List<Cell> cells = geohash.coveringCells(
    pickupLocation,
    radius
);

Set<DriverId> candidates = new HashSet<>();

for (Cell cell : cells) {
    candidates.addAll(driverIndex.getDrivers(cell));
}

List<Driver> nearby = new ArrayList<>();

for (DriverId id : candidates) {

    Driver driver = driverStore.getLatest(id);

    if (driver == null) {
        continue;
    }

    if (!driver.locationIsFresh()) {
        continue;
    }

    double distance = haversine(
        pickupLocation,
        driver.location()
    );

    if (distance <= radius) {
        nearby.add(driver);
    }
}
```

This is the fundamental nearby discovery algorithm.

It is conceptually independent of the storage system.

It can work with Redis, a dedicated geospatial database, or another spatial index.

# 4.10 Quadtrees — Divide geographical space recursively

Geohash divides space into cells based on fixed hierarchical intervals.

Another approach is to recursively subdivide areas based on how much data they contain.

This leads us to Quadtrees.

Imagine we initially have one large geographical region:

```
+---------------------------+
|                           |
|       Entire City         |
|                           |
+---------------------------+
```

We divide it into four quadrants.

```
+-------------+-------------+
|      NW     |      NE     |
|             |             |
+-------------+-------------+
|      SW     |      SE     |
|             |             |
+-------------+-------------+
```

Now suppose the northwest region has too many drivers.

We subdivide it further.

```
+------+------+-------------+
| NW-1 | NW-2 |             |
|      |      |             |
+------+------+-------------+
| NW-3 | NW-4 |             |
|      |      |             |
+------+------+-------------+
|             |             |
|      SW     |      SE     |
+-------------+-------------+
```

We keep subdividing dense regions.

Less populated regions can remain large.

This is the important distinction:

A Quadtree adapts the size of spatial regions according to the distribution of data.

### Why is this useful for ride-hailing?

Consider:

```
City center: 100,000 drivers
Suburban area: 5,000 drivers
Rural area: 200 drivers
```

Using a fixed cell size everywhere may be inefficient.

The city center might have too many drivers inside each cell.

Rural cells might be unnecessarily small.

With a Quadtree:

```
Dense city center:
    Split into smaller cells.

Sparse suburban area:
    Keep larger cells.
```

This can improve search efficiency.

### How does searching work?

Suppose the customer requests a ride with a 3 km radius.

We start from the root node.

For every node:

1. Check whether its geographical rectangle intersects the search circle.
2. If it doesn't intersect, ignore the node.
3. If it intersects and is a leaf, inspect its drivers.
4. If it intersects and has children, recursively inspect relevant children.

Conceptually:

\#chatgpt-mermaid-\_r_435\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_435\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_435\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_435\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_435\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_435\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_435\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_435\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_435\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_435\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_435\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_435\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_435\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_435\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_435\_ p{margin:0;}#chatgpt-mermaid-\_r_435\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_435\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_435\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_435\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_435\_ .label text,#chatgpt-mermaid-\_r_435\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_435\_ .node rect,#chatgpt-mermaid-\_r_435\_ .node circle,#chatgpt-mermaid-\_r_435\_ .node ellipse,#chatgpt-mermaid-\_r_435\_ .node polygon,#chatgpt-mermaid-\_r_435\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_435\_ .rough-node .label text,#chatgpt-mermaid-\_r_435\_ .node .label text,#chatgpt-mermaid-\_r_435\_ .image-shape .label,#chatgpt-mermaid-\_r_435\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_435\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_435\_ .rough-node .label,#chatgpt-mermaid-\_r_435\_ .node .label,#chatgpt-mermaid-\_r_435\_ .image-shape .label,#chatgpt-mermaid-\_r_435\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_435\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_435\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_435\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_435\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_435\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_435\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_435\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_435\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_435\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_435\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_435\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_435\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_435\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_435\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_435\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_435\_ .icon-shape,#chatgpt-mermaid-\_r_435\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_435\_ .icon-shape p,#chatgpt-mermaid-\_r_435\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_435\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_435\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_435\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_435\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_435\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_435\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_435\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_435\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_435\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_435\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_435\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_435\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_435\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_435\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_435\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_435\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_435\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_435\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_435\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_435\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_435\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_435\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_435\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_435\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_435\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_435\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_435\_ .node rect,#chatgpt-mermaid-\_r_435\_ .node circle,#chatgpt-mermaid-\_r_435\_ .node ellipse,#chatgpt-mermaid-\_r_435\_ .node polygon,#chatgpt-mermaid-\_r_435\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_435\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_435\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_435\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_435\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_435\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Start at Root CellDoes cell intersect searcharea?Ignore CellIs Leaf Node?Retrieve DriversVisit Relevant ChildrenExact Distance FilteringNoYesYesNo

### Advantages

- Adaptive cell sizes.
- Dense geographical regions can be subdivided.
- Can reduce candidate counts.
- Useful for highly uneven populations.

### Disadvantages

- More complicated insertion and movement.
- A driver may trigger tree restructuring when density thresholds are crossed.
- Distributed ownership and rebalancing require additional design.
- Queries become more complicated than simple fixed-cell lookups.

Quadtrees are particularly useful when geographical density varies significantly.

But they aren't automatically better than geohash.

The right choice depends on how much complexity is justified by the performance gains.

# 4.11 H3 — Hierarchical hexagonal geographical indexing

Now let's explore a system specifically designed for large-scale geospatial indexing.

&#x20;is an open-source hierarchical geospatial indexing system developed by Uber.

Its core idea is:

Divide the Earth into hexagonal cells and assign every location to a cell at a particular resolution.

Why hexagons?

Consider a grid made of squares:

```
+-----+-----+-----+
|     |     |     |
+-----+-----+-----+
|     |  C  |     |
+-----+-----+-----+
|     |     |     |
+-----+-----+-----+
```

A square has:

- Four edge-sharing neighbours.
- Four corner-sharing neighbours.

This creates ambiguity when calculating neighbourhoods.

Hexagons have six edge-sharing neighbours.

```
       ______
      /      \
  ___/   C    \___
 /   \        /   \
 \___/        \___/
     \        /
      \______/
```

The geometry is more uniform in many local-neighbourhood calculations.

This makes hexagonal grids useful for geographical analytics and proximity operations.

H3 divides the globe into hexagonal cells, with 12 pentagons at each resolution due to the geometry required to cover a sphere.

You generally don't need to worry about the pentagons for ordinary neighbourhood queries, but a robust implementation must use H3's actual grid operations instead of assuming every cell behaves identically.

## How does H3 work?

Suppose a driver sends:

```
Latitude  = 17.3850
Longitude = 78.4867
```

We select an H3 resolution.

H3 converts those coordinates into a cell identifier.

Conceptually:

```
(17.3850, 78.4867)
          |
          v
      H3 Index
          |
          v
   Cell: 8xxxxxxx
```

All drivers inside the same cell can be indexed together.

```
H3 Cell A:
    D101
    D102
    D103

H3 Cell B:
    D104
    D105

H3 Cell C:
    D106
    D107
```

We can maintain an index like:

```
h3_cell_id -> Set of Driver IDs
```

Now a customer request can first identify the relevant H3 cells.

Then we retrieve drivers from those cells and perform exact distance checks.

## How do we select H3 resolution?

H3 supports multiple resolutions.

Higher resolution means smaller cells.

Lower resolution means larger cells.

Here is an approximate reference:

| H3 resolution | Average hexagon area |
| ------------- | -------------------- |
| 4             | 1,770 km²            |
| 5             | 253 km²              |
| 6             | 36.1 km²             |
| 7             | 5.16 km²             |
| 8             | 0.737 km²            |
| 9             | 0.105 km²            |
| 10            | 0.015 km²            |

These are average areas. Individual cells aren't all exactly identical.

Notice that resolution 8 has cells of approximately 0.737 km² on average.

This might be a useful starting point for indexing an urban area, but it is not automatically the correct resolution for a 3 km matching radius.

We still need to evaluate:

- Search radius.
- Driver density.
- Search frequency.
- Number of cells to visit.
- Candidate count per cell.

## How do we find neighbouring cells?

H3 provides neighbourhood operations.

For example, conceptually:

```
gridDisk(originCell, k)
```

This returns cells within a grid distance of `k` from the origin cell.

For example:

```
gridDisk(Cell F, 1)
```

Returns:

```
        A
      /   \
     B  F  C
      \   /
       D-E
```

The exact arrangement depends on the H3 grid.

But here's an important subtlety:

A grid distance of one is not the same thing as a geographical radius of 1 km.

It measures cell-to-cell distance in the H3 grid.

So we cannot simply do:

```
Search radius = 3 km

Therefore k = 3
```

That would be an unjustified assumption.

Instead, we need a reliable method for finding every cell that could intersect our geographical search circle.

Two approaches are useful.

### Approach A: Geographical polygon covering

1. Create a polygon approximating the search circle.
2. Ask the spatial indexing library to cover that polygon with H3 cells.
3. Retrieve drivers from all intersecting cells.
4. Calculate exact distance for every candidate.

The generated cell set must be a conservative cover: it must not leave gaps that could contain eligible drivers.

### Approach B: Conservative neighbourhood expansion

1. Identify the customer's H3 cell.
2. Determine a sufficiently large grid neighbourhood.
3. Retrieve all candidates from that neighbourhood.
4. Apply exact geographical distance filtering.

The chosen neighbourhood must be large enough to cover the entire requested radius, accounting for cell dimensions and geometry.

For correctness, polygon covering is generally the cleaner conceptual approach when an appropriate H3 library operation is available.

## Why is H3 interesting for Uber-like systems?

Imagine a matching service receiving thousands of requests in the same city.

Instead of querying every driver, requests can locate the cells they need.

```
Request 1 -> Cells A, B, C
Request 2 -> Cells B, C, D
Request 3 -> Cells X, Y, Z
Request 4 -> Cells A, B, C
```

We can also cache or maintain aggregated cell information.

For example:

```
Cell A:
    Online drivers = 250
    Available drivers = 120
```

This can help quickly identify promising search regions.

However, these counts are just hints.

They cannot replace checking actual current driver state when issuing an offer.

# 4.12 S2 — Another hierarchical spherical indexing system

Another important alternative is S2 Geometry.

S2 is designed to represent and index regions on a sphere.

Its basic idea:

1. Project the spherical Earth onto the six faces of a cube.
2. Subdivide each cube face recursively.
3. Assign hierarchical cell identifiers.
4. Use those identifiers for geographical indexing and region operations.

Conceptually:

```
             Earth
               |
               v
        Six cube faces
               |
               v
       Recursive subdivision
               |
               v
          S2 Cell IDs
```

Just like H3, S2 allows us to represent a point as a cell at a given level.

We can also cover geographical regions with cells.

The key capabilities we're interested in are:

- Hierarchical cell IDs.
- Neighbouring and parent/child cells.
- Region covering.
- Efficient geographical indexing.
- Spatial intersection operations.

## H3 vs S2 vs Geohash vs Quadtree

Let's compare them.

| Property              | Geohash                               | Quadtree       | H3                               | S2                               |
| --------------------- | ------------------------------------- | -------------- | -------------------------------- | -------------------------------- |
| Cell shape            | Rectangle                             | Rectangle      | Mostly hexagonal                 | Spherical quadrilateral          |
| Hierarchical          | Yes                                   | Yes            | Yes                              | Yes                              |
| Adaptive density      | No, unless combined with custom logic | Yes            | No inherent adaptive subdivision | No inherent adaptive subdivision |
| Neighbour queries     | Must calculate carefully              | Tree traversal | Grid operations                  | Spherical cell operations        |
| Region covering       | Requires implementation/library       | Tree traversal | Supported through libraries      | Supported through libraries      |
| Ease of understanding | Easy                                  | Moderate       | Moderate                         | Moderate to advanced             |

### Which would I choose?

For a straightforward system:

- Redis GEO can already handle many practical radius searches.
- Geohash is easy to understand and implement.
- H3 is attractive when geographic partitioning and neighbourhood operations are central to the architecture.
- S2 is attractive when spherical geometry and hierarchical region covering are important.
- Quadtree is attractive when adaptive spatial subdivision is useful.

Don't choose H3 just because Uber created it.

Don't choose geohash just because it is simple.

Choose according to the indexing and query requirements.

# 4.13 So how does Redis GEO fit into all of this?

This brings us back to Redis.

You might be thinking:

> We already have Redis GEO. Why do we need to understand geohash, H3, S2, and Quadtrees?

Because they solve related but not identical problems.

Redis GEO provides a convenient geospatial index.

For example:

```
GEOADD geo:{p3} 78.4867 17.3850 D101
```

Search:

```
GEOSEARCH geo:{p3}
    FROMLONLAT 78.4867 17.3850
    BYRADIUS 3 km
    WITHDIST
    ASC
```

Redis handles the spatial indexing and radius filtering internally.

You do not have to manually:

1. Calculate geohash cells.
2. Find neighbouring cells.
3. Retrieve all members.
4. Calculate their distance.

Redis GEO does the geographic search for you.

But it does not solve our entire distributed architecture.

For example:

```
Redis Cluster

Node A:
    geo:{p0}

Node B:
    geo:{p1}

Node C:
    geo:{p2}
```

Our matching service might need to search multiple partitions.

We still need to determine which partitions to query.

And if we use geographic partitions, we may need H3 or geohash to route requests to relevant Redis indexes.

Think about the responsibilities:

| Component         | Responsibility                                         |
| ----------------- | ------------------------------------------------------ |
| H3 / Geohash / S2 | Determine geographical cells                           |
| Partition router  | Determine which storage partitions contain those cells |
| Redis GEO         | Perform nearby search within an index                  |
| Matching service  | Apply business filters and rank candidates             |

These components can work together.

For example:

```
                  Customer Request
                         |
                         v
                  H3 Cell Covering
                         |
                         v
                Identify Relevant Cells
                         |
                         v
                   Redis Partitions
                         |
                         v
                  GEO / Cell Lookup
                         |
                         v
               Exact Distance Validation
                         |
                         v
                  Matching Candidates
```

You don't need H3 and Redis GEO simultaneously in every implementation.

H3 can itself serve as the spatial index when we maintain cell-to-driver membership sets.

Redis GEO can provide spatial search directly.

We should avoid maintaining multiple redundant spatial indexes unless their separate responsibilities justify the additional consistency and write overhead.

# 4.14 The most important concept — Candidate search vs exact distance filtering

Let's pause and consolidate something.

Suppose the actual search radius is 3 km.

Our spatial index returns these drivers:

| Driver | Straight-line distance |
| ------ | ---------------------- |
| D101   | 0.4 km                 |
| D102   | 1.2 km                 |
| D103   | 2.8 km                 |
| D104   | 3.1 km                 |
| D105   | 4.5 km                 |

Which ones are eligible geographically?

```
D101 -> Yes
D102 -> Yes
D103 -> Yes
D104 -> No
D105 -> No
```

The spatial index is responsible for finding a sufficiently complete candidate set.

Exact distance filtering is responsible for enforcing our radius.

This separation is extremely important.

We must avoid a common implementation mistake:

```
Find the customer's cell.
Fetch drivers from that cell.
Return them as nearby drivers.
```

That's incorrect because:

- Drivers in neighboring cells might be closer.
- Drivers inside the customer's cell might be outside the radius.
- Cell sizes may vary.
- The circle might intersect several cells.

The correct approach is:

```
Geographical region
       |
       v
Find all intersecting index partitions/cells
       |
       v
Retrieve candidate drivers
       |
       v
Validate exact geographical distance
       |
       v
Return geographically eligible drivers
```

## 4.15 Build the complete nearby-search algorithm

Let's put everything together.

Input:

```
Customer pickup:
    Latitude  = 17.3850
    Longitude = 78.4867

Search radius:
    3 km
```

Available data:

```
Driver latest locations
Driver availability
Driver freshness information
Spatial index
```

Our algorithm:

Step 1 — Determine the geographic search region

Construct the 3 km circle and its conservative bounding region.

Step 2 — Determine intersecting spatial cells or partitions

Depending on our implementation:

- Redis GEO: Search the relevant GEO indexes using the radius.
- Geohash: Find all intersecting geohash cells.
- H3: Cover the search region with H3 cells.
- S2: Cover the region using spherical cells.
- Quadtree: Traverse nodes intersecting the search region.

Step 3 — Retrieve candidates

Fetch driver IDs and, where available, indexed distances.

Step 4 — Validate candidate freshness

Discard drivers whose locations are older than our accepted freshness threshold.

For example:

```
Location received 2 seconds ago -> Fresh
Location received 8 seconds ago -> Fresh, depending on policy
Location received 30 seconds ago -> Stale
```

Step 5 — Validate current operational availability

Discard drivers who are:

- Offline.
- Busy.
- Already reserved.
- Incompatible with the requested service.
- Otherwise ineligible.

Step 6 — Validate exact geographic distance

Use Haversine or another suitable geodesic distance implementation.

Keep:

\\[ distance(driver,pickup) \leq radius \\]

Step 7 — Rank the remaining candidates

Possible ranking inputs:

- Straight-line distance.
- Estimated road travel time.
- Driver's ETA.
- Vehicle compatibility.
- Driver reliability.
- Acceptance probability.
- Platform-specific fairness rules.

Step 8 — Return the candidate list to dispatch

The matching service now has a set of eligible drivers.

It can begin the offer process.

Here's the full algorithm:

\#chatgpt-mermaid-\_r_46b\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_46b\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_46b\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_46b\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_46b\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46b\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_46b\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_46b\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_46b\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_46b\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_46b\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_46b\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_46b\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_46b\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_46b\_ p{margin:0;}#chatgpt-mermaid-\_r_46b\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46b\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46b\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46b\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_46b\_ .label text,#chatgpt-mermaid-\_r_46b\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46b\_ .node rect,#chatgpt-mermaid-\_r_46b\_ .node circle,#chatgpt-mermaid-\_r_46b\_ .node ellipse,#chatgpt-mermaid-\_r_46b\_ .node polygon,#chatgpt-mermaid-\_r_46b\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_46b\_ .rough-node .label text,#chatgpt-mermaid-\_r_46b\_ .node .label text,#chatgpt-mermaid-\_r_46b\_ .image-shape .label,#chatgpt-mermaid-\_r_46b\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_46b\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_46b\_ .rough-node .label,#chatgpt-mermaid-\_r_46b\_ .node .label,#chatgpt-mermaid-\_r_46b\_ .image-shape .label,#chatgpt-mermaid-\_r_46b\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_46b\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_46b\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_46b\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_46b\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_46b\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_46b\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_46b\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_46b\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_46b\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_46b\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_46b\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46b\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46b\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_46b\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46b\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_46b\_ .icon-shape,#chatgpt-mermaid-\_r_46b\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_46b\_ .icon-shape p,#chatgpt-mermaid-\_r_46b\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_46b\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_46b\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_46b\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_46b\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_46b\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_46b\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_46b\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_46b\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_46b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_46b\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_46b\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_46b\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_46b\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_46b\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_46b\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_46b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_46b\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_46b\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_46b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_46b\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_46b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_46b\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_46b\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_46b\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_46b\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_46b\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_46b\_ .node rect,#chatgpt-mermaid-\_r_46b\_ .node circle,#chatgpt-mermaid-\_r_46b\_ .node ellipse,#chatgpt-mermaid-\_r_46b\_ .node polygon,#chatgpt-mermaid-\_r_46b\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_46b\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_46b\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_46b\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_46b\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_46b\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Ride RequestPickup Coordinates andSearch RadiusDetermine Search RegionFind Intersecting Cells / GEOPartitionsRetrieve Driver CandidatesLocation Fresh?DiscardDriver Available?Calculate Exact GeographicDistanceWithin Radius?Rank DriverDispatch CandidatesNoYesNoYesNoYes

# 4.16 A crucial distinction — Straight-line distance vs road distance

Suppose we find:

```
Driver A:
Straight-line distance = 1 km

Driver B:
Straight-line distance = 1.5 km
```

Should we always select Driver A?

No.

Imagine Driver A is on the opposite side of a river.

There is no nearby bridge.

Driver B is on the same side of the river and has direct road access.

The actual situation might be:

```
                 Customer
                    |
                    |
                 Road
                    |
River --------------|--------------
                    |
                Bridge
                    |
                  Driver A


Driver B is nearby on the customer's side.
```

The road travel distances might be:

```
Driver A -> 6 km by road
Driver B -> 1.8 km by road
```

Driver B is the better candidate.

This is why we distinguish:

Geospatial distance: Distance between two coordinates, approximately as the crow flies.

Road distance: Distance along the available road network.

Estimated travel time (ETA): Expected time to reach the destination, considering routes and potentially traffic.

These are different quantities.

### Should we calculate road distance for every driver?

Usually not.

Imagine our spatial search returns 500 drivers.

Calling a routing or mapping service 500 times per customer request may be expensive and slow.

Instead, we use a staged approach:

```
Stage 1:
Spatial search
    |
    v
500 drivers

Stage 2:
Cheap filtering and ranking
    |
    v
50 drivers

Stage 3:
Route / ETA estimation
    |
    v
10 drivers

Stage 4:
Dispatch strategy
    |
    v
Selected candidates
```

We can also use a precomputed or locally available routing engine, rather than making hundreds of external API calls.

The key principle:

Use inexpensive geographic filtering to reduce the candidate set before expensive travel-time calculations.

# 4.17 How should we choose the search radius?

There isn't one universally correct radius.

It depends on the business.

For example:

| Situation        | Possible search strategy                           |
| ---------------- | -------------------------------------------------- |
| Dense urban area | Start with a relatively small radius               |
| Suburban area    | Use a larger radius                                |
| Rural area       | Search a much larger region                        |
| High demand      | Broaden the search if local supply is insufficient |
| Low demand       | Expand gradually while maintaining acceptable ETA  |

Imagine:

```
Initial radius = 2 km
```

We search and find only one suitable driver.

But our dispatch service needs five candidates.

We can expand:

```
2 km -> 3 km -> 5 km -> 8 km
```

Each expansion discovers more geographical candidates.

We should not necessarily jump directly to 20 km.

A gradual expansion gives us greater control over:

- Driver travel time.
- Search latency.
- Network traffic.
- Candidate quality.
- Driver notification load.

Also, the chosen radius should be based on actual serviceability requirements, not just arbitrary distances.

A 4 km search radius in a city with heavy traffic can be less useful than a 6 km search radius in a less congested area.

This leads us naturally into the next chapter, where we'll build the complete matching strategy.

# 4.18 What about multiple cities and geographical regions?

Suppose we're operating in:

```
Hyderabad
Bangalore
Mumbai
Delhi
```

We shouldn't necessarily maintain one global driver index and query it for every request.

We can organize the infrastructure geographically:

```
                    Matching System
                           |
                    Regional Router
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
          Hyderabad     Bangalore      Mumbai
             |             |             |
             v             v             v
          Redis GEO      Redis GEO     Redis GEO
          Partitions     Partitions    Partitions
```

A request in Hyderabad searches Hyderabad's relevant geographical partitions.

It should not query drivers in Mumbai.

For regions near city or service boundaries, our design must account for actual service coverage.

A ride pickup near a regional boundary may require searching adjacent regions if their drivers are eligible to serve it.

Do not blindly equate administrative city boundaries with operational service boundaries.

We can define service regions based on:

- Driver operating areas.
- Service coverage polygons.
- Operational constraints.
- Regulatory restrictions.
- Dispatch ownership.

The regional router determines which search infrastructure owns the relevant geographical area.

This is an early building block for the multi-region architecture we'll revisit in Chapter 10.

# 4.19 What should we actually choose for our implementation?

Let's make the decision practical.

Suppose you're designing a system in an interview.

Start with this:

Version 1 — Moderate scale

```
Redis GEO
    |
    +---- GEO index for drivers
```

Search using `GEOSEARCH`.

No need to introduce H3 or Quadtree immediately.

Version 2 — Redis write bottleneck

Partition drivers across multiple GEO keys.

```
Redis Cluster
    |
    +---- geo:{p0}
    +---- geo:{p1}
    +---- geo:{p2}
    +---- geo:{p3}
```

The matching service searches the required partitions in parallel.

Version 3 — Search fan-out bottleneck

Introduce geographical cells.

```
H3 / Geohash
      |
      v
Relevant geographical cells
      |
      v
Regional Redis indexes
      |
      v
Candidate merge
```

Version 4 — Highly uneven geographical density

Consider adaptive spatial partitioning such as a Quadtree, or a combination of fixed hierarchical cells and density-aware subdivisions.

You should be able to explain not only what algorithm you chose, but also which bottleneck motivated it.

## 4.20 One additional spatial index worth knowing: R-tree

You may encounter another spatial indexing structure called an R-tree.

Unlike geohash, which converts points into hierarchical cell identifiers, an R-tree indexes geographical objects using bounding rectangles.

Conceptually:

```
                 Root
                   |
          +--------+--------+
          |                 |
      Rectangle A       Rectangle B
          |                 |
       +--+--+           +--+--+
       |     |           |     |
      R1    R2          R3    R4
```

When searching an area, we traverse only the bounding rectangles that intersect the requested region.

R-trees are useful for:

- Geographic objects.
- Complex shapes.
- Roads and geographical boundaries.
- Spatial databases.
- Range and intersection queries.

They are an important alternative, although Redis GEO, H3, S2, and geohash are often more natural starting points for a moving-driver point-location workload.

# Chapter 4 — Final mental model

Let's consolidate everything into one picture.

```
              Customer Pickup
                     |
                     v
              Search Radius
                     |
                     v
             Geographical Region
                     |
                     v
             Spatial Index Lookup
                     |
         +-----------+-----------+
         |           |           |
         v           v           v
      Cell A       Cell B      Cell C
         |           |           |
         +-----------+-----------+
                     |
                     v
              Candidate Drivers
                     |
                     v
              Freshness Check
                     |
                     v
             Availability Check
                     |
                     v
             Exact Distance Check
                     |
                     v
                Driver Ranking
                     |
                     v
               Dispatch Service
```

The concepts to understand deeply:

| Concept                  | What problem does it solve?                                          |
| ------------------------ | -------------------------------------------------------------------- |
| Haversine                | Calculates geographical distance                                     |
| Bounding box             | Quickly narrows the geographical search area                         |
| Geohash                  | Converts coordinates into hierarchical rectangular cells             |
| Quadtree                 | Adaptively subdivides dense geographical areas                       |
| H3                       | Provides hierarchical hexagonal geographical indexing                |
| S2                       | Provides hierarchical indexing over spherical geometry               |
| Redis GEO                | Provides convenient geographical indexing and radius searches        |
| Exact distance filtering | Prevents false positives from approximate spatial candidate searches |
| Road ETA                 | Determines practical travel time rather than straight-line distance  |

My recommendation: Start with Redis GEO and learn geospatial cell covering through H3 or geohash. You don't need to build your own Quadtree or spatial indexing algorithm unless your workload justifies it.

Also, remember that geographic proximity is only the first stage of matching. A nearby driver may still be busy, stale, incompatible, or already reserved.

# Next: Chapter 5 — Nearby Driver Discovery and Matching Algorithms

Now that we understand the geographical algorithms, let's build the actual matching engine.

We'll answer some particularly important questions:

1. How do we efficiently search across multiple Redis geographical partitions?
2. Should we fetch all nearby drivers or only the top K?
3. What happens when we have 10,000 nearby drivers?
4. How do we expand the radius without wasting time?
5. How do we handle drivers moving while a search is happening?
6. How do we combine GEO distance, availability, freshness, vehicle type, and ETA?
7. Should we rank drivers by distance, ETA, or acceptance probability?
8. How do we avoid two simultaneous ride requests selecting the same driver?
9. How do we handle low driver supply in rural areas?
10. Should we select the nearest driver or send offers to multiple drivers in parallel?

We'll also distinguish three very different operations:

- Discovery: Find geographically suitable drivers.
- Eligibility: Determine which drivers can actually accept work.
- Dispatch: Decide whom to contact and in what order.

This separation is essential to designing matching systems correctly.

Say next to continue.
