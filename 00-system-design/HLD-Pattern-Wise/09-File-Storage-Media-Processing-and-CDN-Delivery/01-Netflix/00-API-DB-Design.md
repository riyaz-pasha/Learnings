# System Design: Movie / Series Streaming Platform (Netflix / Prime Video)

We will design a video streaming platform similar to Netflix or Amazon Prime Video.

The system should allow users to:

1. Register, log in, and manage profiles.
2. Browse movies, series, seasons, and episodes.
3. Search and filter content.
4. View movie/series details, trailers, genres, cast, etc.
5. Stream videos in different resolutions and formats.
6. Resume watching from where they left off.
7. Maintain watch history and watchlists.
8. Like/dislike content and receive personalized recommendations.
9. Manage subscriptions and payments.
10. Support multiple devices and simultaneous streaming.
11. Restrict content based on region, subscription plan, and licensing.
12. Support subtitles, audio languages, and multiple video qualities.

We will focus on Database Design and REST API Design, keeping scalability and production-level considerations in mind.

# 1. High-Level Domain Understanding

Before designing the database, understand the core domain entities and their relationships.

There are two primary types of content:

- Movie: A standalone video.
- Series: A collection of seasons, where each season contains multiple episodes.

For example:

```
Content
│
├── Movie
│   ├── Inception
│   └── Interstellar
│
└── Series
    ├── Breaking Bad
    │   ├── Season 1
    │   │   ├── Episode 1
    │   │   ├── Episode 2
    │   │   └── Episode 3
    │   │
    │   └── Season 2
    │       ├── Episode 1
    │       └── Episode 2
    │
    └── Stranger Things
        ├── Season 1
        └── Season 2
```

An important design decision is to have a common `Content` entity for movies and series.

This allows us to reuse common functionality such as:

- Search
- Genres
- Ratings
- Cast and crew
- Languages
- Content availability
- Recommendations
- Watchlists

We will use a relational database such as PostgreSQL for the core transactional data.

Video files themselves will NOT be stored in PostgreSQL. They will be stored in object storage such as Amazon S3, with a CDN such as CloudFront serving the video content.

# 2. Database Design

Let's divide the database into logical domains.

1. User and Profile Management
2. Content Management
3. Video Asset Management
4. Subscription and Payment Management
5. Watch History and Progress
6. Watchlist and User Interactions
7. Device and Streaming Session Management
8. Content Availability and Regional Restrictions

## 2.1 User and Profile Management

An important distinction exists between a User and a Profile.

A user represents the account holder, while profiles represent individual viewers within that account.

For example:

```
User: John
│
├── Profile: John
├── Profile: Sarah
├── Profile: Kids
└── Profile: Guest
```

Each profile can have its own:

- Watch history
- Watchlist
- Recommendations
- Language preferences
- Parental controls

### Table: users

Stores account-level information.

| Column        | Type      | Description              |
| ------------- | --------- | ------------------------ |
| id            | UUID      | Primary key              |
| email         | VARCHAR   | Unique email             |
| password_hash | VARCHAR   | Hashed password          |
| full_name     | VARCHAR   | User name                |
| status        | VARCHAR   | ACTIVE, BLOCKED, DELETED |
| created_at    | TIMESTAMP | Account creation         |
| updated_at    | TIMESTAMP | Last update              |

Constraints:

- `id` is the primary key.
- `email` must be unique.
- Passwords must be securely hashed, never stored in plaintext.

### Table: profiles

Stores individual viewing profiles under an account.

| Column             | Type      | Description                  |
| ------------------ | --------- | ---------------------------- |
| id                 | UUID      | Primary key                  |
| user_id            | UUID      | FK → users.id                |
| name               | VARCHAR   | Profile name                 |
| avatar_url         | TEXT      | Avatar image                 |
| is_kids_profile    | BOOLEAN   | Kids profile                 |
| preferred_language | VARCHAR   | Preferred language           |
| maturity_level     | VARCHAR   | Content maturity restriction |
| created_at         | TIMESTAMP | Creation timestamp           |

Relationship:

```
users (1) ─────────── (N) profiles
```

A user can have multiple profiles, but each profile belongs to exactly one user.

Important: Most viewing-related operations should use `profile_id`, not `user_id`.

Otherwise, different family members will share the same watch history and recommendations.

## 2.2 Content Management

This is the most important part of the database.

We need to represent movies, series, seasons, and episodes.

### Table: contents

Stores common information for movies and series.

| Column           | Type      | Description                |
| ---------------- | --------- | -------------------------- |
| id               | UUID      | Primary key                |
| title            | VARCHAR   | Content title              |
| description      | TEXT      | Description                |
| content_type     | VARCHAR   | MOVIE or SERIES            |
| release_date     | DATE      | Release date               |
| duration_seconds | INTEGER   | Duration for movies        |
| maturity_rating  | VARCHAR   | PG, PG-13, R, etc.         |
| status           | VARCHAR   | DRAFT, PUBLISHED, ARCHIVED |
| poster_url       | TEXT      | Poster image               |
| backdrop_url     | TEXT      | Background image           |
| created_at       | TIMESTAMP | Creation timestamp         |
| updated_at       | TIMESTAMP | Last update                |

Examples:

| id | title           | content_type |
| -- | --------------- | ------------ |
| C1 | Inception       | MOVIE        |
| C2 | Interstellar    | MOVIE        |
| C3 | Breaking Bad    | SERIES       |
| C4 | Stranger Things | SERIES       |

Why use a common table?

Imagine a user searches for "Christopher Nolan".

We might need to return:

- Inception
- Interstellar
- The Dark Knight

Having common content metadata makes it easier to search across different content types.

Design decision: `duration_seconds` is populated for movies. For series, duration belongs to individual episodes.

### Table: seasons

Stores seasons belonging to a series.

| Column        | Type    | Description           |
| ------------- | ------- | --------------------- |
| id            | UUID    | Primary key           |
| series_id     | UUID    | FK → contents.id      |
| season_number | INTEGER | Season number         |
| title         | VARCHAR | Optional season title |
| description   | TEXT    | Season description    |
| release_date  | DATE    | Release date          |

Example:

```
Breaking Bad (C3)
│
├── Season 1 (S1)
├── Season 2 (S2)
└── Season 3 (S3)
```

Constraints:

- `series_id` references `contents.id`.
- `(series_id, season_number)` must be unique.
- Only content with `content_type = SERIES` can have seasons. Enforce this through application validation or a database trigger.

### Table: episodes

Stores individual episodes.

| Column           | Type    | Description         |
| ---------------- | ------- | ------------------- |
| id               | UUID    | Primary key         |
| season_id        | UUID    | FK → seasons.id     |
| episode_number   | INTEGER | Episode number      |
| title            | VARCHAR | Episode title       |
| description      | TEXT    | Episode description |
| duration_seconds | INTEGER | Duration            |
| release_date     | DATE    | Release date        |
| thumbnail_url    | TEXT    | Episode thumbnail   |

Example:

```
Breaking Bad
│
└── Season 1
    │
    ├── Episode 1: Pilot
    ├── Episode 2: Cat's in the Bag...
    └── Episode 3: ...And the Bag's in the River
```

Constraint:

`UNIQUE(season_id, episode_number)`

### Content hierarchy

\#chatgpt-mermaid-\_r_2qk\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2qk\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2qk\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2qk\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2qk\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qk\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2qk\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2qk\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2qk\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2qk\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2qk\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2qk\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qk\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qk\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2qk\_ p{margin:0;}#chatgpt-mermaid-\_r_2qk\_ .entityBox{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2qk\_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2qk\_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-\_r_2qk\_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-\_r_2qk\_ .edgeLabel{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qk\_ .edgeLabel .label rect{fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qk\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qk\_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-\_r_2qk\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qk\_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-\_r_2qk\_ .node rect,#chatgpt-mermaid-\_r_2qk\_ .node circle,#chatgpt-mermaid-\_r_2qk\_ .node ellipse,#chatgpt-mermaid-\_r_2qk\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2qk\_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-\_r_2qk\_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-\_r_2qk\_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-\_r_2qk\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2qk\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2qk\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2qk\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2qk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qk\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2qk\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2qk\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2qk\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qk\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2qk\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2qk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qk\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2qk\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2qk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qk\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2qk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qk\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}CONTENTSuuididPKstringtitlestringcontent_typedaterelease_dateSEASONSuuididPKuuidseries_idFKintseason_numberEPISODESuuididPKuuidseason_idFKintepisode_numberstringtitleintduration_secondscontainscontains

Note that the `contents` table stores both movies and series, while seasons and episodes are separate tables.

## 2.3 Genres, Cast and Crew

We should avoid storing multiple genres, actors, or directors in comma-separated columns.

These are many-to-many relationships.

### Table: genres

| Column | Type           |
| ------ | -------------- |
| id     | UUID PK        |
| name   | VARCHAR UNIQUE |

Examples:

- Action
- Drama
- Thriller
- Comedy
- Science Fiction

### Table: content_genres

| Column     | Type    |
| ---------- | ------- |
| content_id | UUID FK |
| genre_id   | UUID FK |

Composite primary key:

`(content_id, genre_id)`

Relationship:

```
contents (N) ─────── (N) genres
```

### Table: people

Stores actors, directors, writers, producers, etc.

| Column    | Type    |
| --------- | ------- |
| id        | UUID PK |
| name      | VARCHAR |
| biography | TEXT    |
| photo_url | TEXT    |

### Table: content_credits

Connects content with actors and crew members.

| Column         | Type    | Description             |
| -------------- | ------- | ----------------------- |
| id             | UUID    | Primary key             |
| content_id     | UUID    | FK → contents.id        |
| person_id      | UUID    | FK → people.id          |
| role           | VARCHAR | ACTOR, DIRECTOR, WRITER |
| character_name | VARCHAR | Character name          |
| display_order  | INTEGER | Display order           |

Example:

```
Inception
│
├── Leonardo DiCaprio → Actor
├── Joseph Gordon-Levitt → Actor
└── Christopher Nolan → Director
```

For a large production system, episode-level credits can also be supported by introducing an episode credits association.

# 3. Video Asset Management

This is an important part of a streaming platform.

A movie is not necessarily stored as one video file.

The same movie can have:

- 480p version
- 720p version
- 1080p version
- 4K version
- Different audio tracks
- Different subtitle tracks
- Different streaming formats

We need to model these separately.

## 3.1 Table: video_assets

Stores video metadata and storage references.

| Column           | Type      | Description                |
| ---------------- | --------- | -------------------------- |
| id               | UUID      | Primary key                |
| content_id       | UUID      | FK → contents.id, nullable |
| episode_id       | UUID      | FK → episodes.id, nullable |
| storage_key      | TEXT      | S3 object/prefix reference |
| resolution       | VARCHAR   | 480p, 720p, 1080p, 4K      |
| codec            | VARCHAR   | H264, H265, AV1            |
| bitrate          | INTEGER   | Bitrate                    |
| streaming_format | VARCHAR   | HLS, DASH                  |
| status           | VARCHAR   | PROCESSING, READY, FAILED  |
| created_at       | TIMESTAMP | Creation timestamp         |

Exactly one of `content_id` or `episode_id` should be populated.

Use a database CHECK constraint for this.

Example:

```
Inception
│
└── Video Assets
    ├── 480p
    ├── 720p
    ├── 1080p
    └── 4K
```

Important: We should ideally store references to packaged streaming manifests and segments rather than one large video file.

For example:

```
s3://streaming-bucket/inception/
│
├── master.m3u8
│
├── 480p/
│   ├── playlist.m3u8
│   └── segments...
│
├── 720p/
│   ├── playlist.m3u8
│   └── segments...
│
└── 1080p/
    ├── playlist.m3u8
    └── segments...
```

The CDN delivers these segments to users.

### 3.2 Table: audio_tracks

| Column         | Type    |
| -------------- | ------- |
| id             | UUID PK |
| video_asset_id | UUID FK |
| language_code  | VARCHAR |
| codec          | VARCHAR |
| is_default     | BOOLEAN |
| storage_key    | TEXT    |

### 3.3 Table: subtitle_tracks

| Column         | Type    |
| -------------- | ------- |
| id             | UUID PK |
| video_asset_id | UUID FK |
| language_code  | VARCHAR |
| format         | VARCHAR |
| storage_key    | TEXT    |
| is_default     | BOOLEAN |

This allows users to select different audio languages and subtitles.

# 4. Subscription and Payment Management

A streaming platform generally supports multiple subscription plans.

For example:

- Basic
- Standard
- Premium

Each plan may have different restrictions:

| Feature              | Basic   | Standard | Premium |
| -------------------- | ------- | -------- | ------- |
| Resolution           | 720p    | 1080p    | 4K      |
| Simultaneous streams | 1       | 2        | 4       |
| Ads                  | Yes     | No       | No      |
| Downloads            | Limited | Yes      | Yes     |

### Table: subscription_plans

| Column           | Type    |
| ---------------- | ------- |
| id               | UUID PK |
| name             | VARCHAR |
| price            | DECIMAL |
| currency         | VARCHAR |
| billing_interval | VARCHAR |
| max_streams      | INTEGER |
| max_resolution   | VARCHAR |
| is_active        | BOOLEAN |

### Table: subscriptions

Stores the subscription associated with a user account.

| Column     | Type      |
| ---------- | --------- |
| id         | UUID PK   |
| user_id    | UUID FK   |
| plan_id    | UUID FK   |
| status     | VARCHAR   |
| start_date | TIMESTAMP |
| end_date   | TIMESTAMP |
| auto_renew | BOOLEAN   |
| created_at | TIMESTAMP |

Possible statuses:

- ACTIVE
- CANCELLED
- EXPIRED
- PAST_DUE
- PENDING

### Table: payments

Stores payment transactions.

| Column                  | Type      |
| ----------------------- | --------- |
| id                      | UUID PK   |
| subscription_id         | UUID FK   |
| amount                  | DECIMAL   |
| currency                | VARCHAR   |
| payment_provider        | VARCHAR   |
| provider_transaction_id | VARCHAR   |
| status                  | VARCHAR   |
| created_at              | TIMESTAMP |

Important considerations:

1. Never store card numbers or CVVs.
2. Use payment gateway tokens.
3. Use idempotency keys for payment requests.
4. Process payment provider webhooks safely.
5. Keep payment records immutable wherever possible.

A user can have multiple subscription records over time, but typically only one effective active subscription at a time.

# 5. Watch History and Playback Progress

This is another critical component.

Imagine:

A user watches a movie for 35 minutes, closes the application, and comes back tomorrow.

The application should resume from 35 minutes.

We need to track viewing progress at the profile level.

### Table: watch_history

| Column                | Type      | Description                |
| --------------------- | --------- | -------------------------- |
| id                    | UUID      | Primary key                |
| profile_id            | UUID      | FK → profiles.id           |
| content_id            | UUID      | FK → contents.id, nullable |
| episode_id            | UUID      | FK → episodes.id, nullable |
| progress_seconds      | INTEGER   | Current playback position  |
| duration_seconds      | INTEGER   | Total duration             |
| completion_percentage | DECIMAL   | Percentage completed       |
| last_watched_at       | TIMESTAMP | Last playback time         |
| completed_at          | TIMESTAMP | Completion timestamp       |

Exactly one of `content_id` or `episode_id` must be populated.

A unique constraint on `(profile_id, content_id)` for movies and `(profile_id, episode_id)` for episodes can prevent duplicate progress records. In PostgreSQL, use separate partial unique indexes.

Example:

```
Profile: John
│
├── Inception
│   ├── Duration: 148 minutes
│   ├── Watched: 35 minutes
│   └── Progress: 23.6%
│
└── Breaking Bad
    │
    └── Season 1
        ├── Episode 1 → Completed
        └── Episode 2 → Watched 15 minutes
```

Important design consideration:

Do not update PostgreSQL every second while the video is playing.

Instead:

- Save progress periodically, e.g. every 15–30 seconds.
- Save when the user pauses.
- Save when the user exits the player.
- Save when the player reaches the end.

The exact interval depends on the acceptable loss of playback progress.

For highly scalable systems, playback events can first be sent to a streaming ingestion service and processed asynchronously.

# 6. Watchlist and User Interactions

## 6.1 Table: watchlists

A simple implementation can directly associate profiles with content.

### Table: watchlist_items

| Column     | Type      |
| ---------- | --------- |
| id         | UUID PK   |
| profile_id | UUID FK   |
| content_id | UUID FK   |
| added_at   | TIMESTAMP |

Constraint:

`UNIQUE(profile_id, content_id)`

A profile can add or remove movies and series from its watchlist.

For series, adding the series to the watchlist should generally add the series-level content, not every episode individually.

## 6.2 Table: content_ratings

Stores likes/dislikes.

| Column     | Type      |
| ---------- | --------- |
| id         | UUID PK   |
| profile_id | UUID FK   |
| content_id | UUID FK   |
| rating     | VARCHAR   |
| created_at | TIMESTAMP |
| updated_at | TIMESTAMP |

Possible values:

- LIKE
- DISLIKE

Constraint:

`UNIQUE(profile_id, content_id)`

This ensures one current rating per profile per content.

# 7. Device and Streaming Session Management

A user may access Netflix from:

- Mobile
- Laptop
- Smart TV
- Tablet

We need to track devices and active streaming sessions.

### Table: devices

| Column            | Type      |
| ----------------- | --------- |
| id                | UUID PK   |
| user_id           | UUID FK   |
| device_name       | VARCHAR   |
| device_type       | VARCHAR   |
| device_identifier | VARCHAR   |
| last_active_at    | TIMESTAMP |

### Table: streaming_sessions

| Column            | Type              |
| ----------------- | ----------------- |
| id                | UUID PK           |
| profile_id        | UUID FK           |
| device_id         | UUID FK           |
| content_id        | UUID FK, nullable |
| episode_id        | UUID FK, nullable |
| status            | VARCHAR           |
| started_at        | TIMESTAMP         |
| last_heartbeat_at | TIMESTAMP         |
| ended_at          | TIMESTAMP         |

Possible statuses:

- ACTIVE
- PAUSED
- ENDED
- EXPIRED

Why do we need streaming sessions?

Suppose a user has a Premium subscription that allows four simultaneous streams.

The system must ensure the user does not exceed this limit.

When a user starts playback:

1. Validate the subscription.
2. Check the simultaneous stream limit.
3. Check whether the content is available.
4. Create a streaming session.
5. Issue playback authorization.
6. Allow the player to access the video.

A heartbeat updates `last_heartbeat_at`.

If a device crashes or loses connectivity, the session can eventually expire based on a timeout.

Concurrency consideration: Two devices may try to start streaming simultaneously when only one slot is available. Session allocation must be atomic. Use a transaction with appropriate locking or a distributed coordination mechanism to avoid exceeding the limit.

# 8. Regional Content Availability

Not all content is available in all countries.

For example:

- Movie A is available in India.
- Movie A is unavailable in the US.
- Movie B is available globally.

We need to model regional licensing.

### Table: content_availability

| Column          | Type      |
| --------------- | --------- |
| id              | UUID PK   |
| content_id      | UUID FK   |
| region_code     | VARCHAR   |
| available_from  | TIMESTAMP |
| available_until | TIMESTAMP |
| is_available    | BOOLEAN   |

Constraint:

`UNIQUE(content_id, region_code)`

The region can be determined using trusted geolocation information, not blindly from a client-provided country value.

For a more sophisticated system, availability can also include plan restrictions, licensing windows, and exclusions.

# 9. Complete Database Relationship Diagram

Let's combine the important entities.

\#chatgpt-mermaid-\_r_2rf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2rf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2rf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2rf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2rf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2rf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2rf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2rf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2rf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2rf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2rf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2rf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2rf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2rf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2rf\_ p{margin:0;}#chatgpt-mermaid-\_r_2rf\_ .entityBox{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2rf\_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2rf\_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-\_r_2rf\_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-\_r_2rf\_ .edgeLabel{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2rf\_ .edgeLabel .label rect{fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2rf\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2rf\_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-\_r_2rf\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2rf\_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-\_r_2rf\_ .node rect,#chatgpt-mermaid-\_r_2rf\_ .node circle,#chatgpt-mermaid-\_r_2rf\_ .node ellipse,#chatgpt-mermaid-\_r_2rf\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2rf\_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-\_r_2rf\_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-\_r_2rf\_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-\_r_2rf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2rf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2rf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2rf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2rf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2rf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2rf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2rf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2rf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2rf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2rf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2rf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2rf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2rf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2rf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2rf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2rf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2rf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}USERSPROFILESSUBSCRIPTIONSDEVICESSUBSCRIPTION_PLANSPAYMENTSCONTENTSSEASONSEPISODESVIDEO_ASSETSAUDIO_TRACKSSUBTITLE_TRACKSCONTENT_GENRESGENRESCONTENT_CREDITSPEOPLEWATCH_HISTORYWATCHLIST_ITEMSCONTENT_RATINGSSTREAMING_SESSIONSCONTENT_AVAILABILITYhasownsregistersdefineshascontainscontainshashascontainscontainsclassifiedbelongshasparticipatesmaintainswatchedwatchedmaintainsaddedratesreceivesstartsusesrestricted_by

This is the core relational model. In production, we would also introduce additional entities for downloads, recommendations, trailers, content licensing, DRM, and editorial collections.

# 10. REST API Design

Now let's design the APIs.

We should organize APIs around resources rather than actions.

Base URL:

```
https://api.streaming.com/v1
```

We will divide the APIs into the following domains:

1. Authentication and Users
2. Profiles
3. Content Discovery
4. Movies and Series
5. Watchlist
6. Watch History
7. Playback
8. Subscriptions and Payments
9. Devices

# 11. Authentication APIs

| Method | Endpoint                | Description            |
| ------ | ----------------------- | ---------------------- |
| POST   | `/auth/register`        | Register user          |
| POST   | `/auth/login`           | Login                  |
| POST   | `/auth/refresh`         | Refresh access token   |
| POST   | `/auth/logout`          | Logout                 |
| POST   | `/auth/forgot-password` | Request password reset |
| POST   | `/auth/reset-password`  | Reset password         |

### Example: Register

`POST /v1/auth/register`

Request:

```
{
  "email": "john@example.com",
  "password": "StrongPassword123",
  "fullName": "John Doe"
}
```

Response: `201 Created`

```
{
  "userId": "user-123",
  "email": "john@example.com",
  "status": "ACTIVE"
}
```

### Example: Login

`POST /v1/auth/login`

Request:

```
{
  "email": "john@example.com",
  "password": "StrongPassword123"
}
```

Response:

```
{
  "accessToken": "access-token",
  "refreshToken": "refresh-token",
  "expiresIn": 900
}
```

In production, prefer secure, HttpOnly, SameSite cookies for browser refresh-token handling where appropriate. Protect refresh tokens through rotation and revocation.

# 12. Profile APIs

| Method | Endpoint                | Description         |
| ------ | ----------------------- | ------------------- |
| POST   | `/profiles`             | Create profile      |
| GET    | `/profiles`             | Get all profiles    |
| GET    | `/profiles/{profileId}` | Get profile details |
| PATCH  | `/profiles/{profileId}` | Update profile      |
| DELETE | `/profiles/{profileId}` | Delete profile      |

### Example: Create profile

`POST /v1/profiles`

Request:

```
{
  "name": "Kids",
  "isKidsProfile": true,
  "preferredLanguage": "en",
  "maturityLevel": "PG"
}
```

Response: `201 Created`

```
{
  "id": "profile-123",
  "name": "Kids",
  "isKidsProfile": true,
  "preferredLanguage": "en",
  "maturityLevel": "PG"
}
```

Important: Verify that the authenticated user owns the profile before allowing access to it.

# 13. Content Discovery APIs

These APIs are responsible for the home page, search, genres, and recommendations.

| Method | Endpoint                                  | Description                  |
| ------ | ----------------------------------------- | ---------------------------- |
| GET    | `/contents`                               | Browse content               |
| GET    | `/contents/{contentId}`                   | Get content details          |
| GET    | `/contents/search`                        | Search content               |
| GET    | `/genres`                                 | List genres                  |
| GET    | `/genres/{genreId}/contents`              | Get content by genre         |
| GET    | `/profiles/{profileId}/recommendations`   | Personalized recommendations |
| GET    | `/profiles/{profileId}/continue-watching` | Continue watching            |

### Example: Browse content

`GET /v1/contents?type=MOVIE&genre=ACTION&page=0&size=20`

Response:

```
{
  "content": [
    {
      "id": "movie-101",
      "title": "Inception",
      "type": "MOVIE",
      "releaseDate": "2010-07-16",
      "maturityRating": "PG-13",
      "posterUrl": "https://cdn.example.com/inception.jpg"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

### Example: Search

`GET /v1/contents/search?q=inception&type=MOVIE`

Possible filters:

- `type`
- `genre`
- `language`
- `releaseYear`
- `maturityRating`
- `sortBy`
- `page`
- `size`

For a large content catalog, use a dedicated search engine such as Elasticsearch or OpenSearch instead of relying entirely on PostgreSQL `LIKE` queries.

### Example: Content details

`GET /v1/contents/{contentId}`

For a movie, return movie details.

For a series, return series details and optionally its seasons.

Example:

```
{
  "id": "series-101",
  "title": "Breaking Bad",
  "type": "SERIES",
  "description": "A chemistry teacher...",
  "releaseDate": "2008-01-20",
  "genres": [
    "Drama",
    "Crime",
    "Thriller"
  ],
  "cast": [
    "Actor A",
    "Actor B"
  ],
  "seasons": [
    {
      "id": "season-1",
      "seasonNumber": 1,
      "episodeCount": 7
    }
  ]
}
```

For a large series, don't return every episode of every season in the initial response.

Use separate endpoints.

# 14. Season and Episode APIs

| Method | Endpoint                       | Description         |
| ------ | ------------------------------ | ------------------- |
| GET    | `/series/{seriesId}/seasons`   | Get seasons         |
| GET    | `/seasons/{seasonId}`          | Get season details  |
| GET    | `/seasons/{seasonId}/episodes` | Get episodes        |
| GET    | `/episodes/{episodeId}`        | Get episode details |

### Example: Get episodes

`GET /v1/seasons/{seasonId}/episodes`

Response:

```
{
  "seasonId": "season-1",
  "episodes": [
    {
      "id": "episode-1",
      "episodeNumber": 1,
      "title": "Pilot",
      "durationSeconds": 3600,
      "thumbnailUrl": "https://cdn.example.com/episode-1.jpg"
    },
    {
      "id": "episode-2",
      "episodeNumber": 2,
      "title": "Cat's in the Bag...",
      "durationSeconds": 3000,
      "thumbnailUrl": "https://cdn.example.com/episode-2.jpg"
    }
  ]
}
```

# 15. Watchlist APIs

| Method | Endpoint                                      | Description    |
| ------ | --------------------------------------------- | -------------- |
| POST   | `/profiles/{profileId}/watchlist`             | Add content    |
| GET    | `/profiles/{profileId}/watchlist`             | Get watchlist  |
| DELETE | `/profiles/{profileId}/watchlist/{contentId}` | Remove content |

### Add to watchlist

`POST /v1/profiles/{profileId}/watchlist`

Request:

```
{
  "contentId": "movie-101"
}
```

Response: `201 Created`

```
{
  "profileId": "profile-123",
  "contentId": "movie-101",
  "addedAt": "2026-11-05T08:00:00Z"
}
```

Use a unique constraint on `(profile_id, content_id)` to prevent duplicate entries.

For repeated requests, you can also make this operation idempotent by returning the existing resource when it is already present.

# 16. Watch History APIs

| Method | Endpoint                                                    | Description             |
| ------ | ----------------------------------------------------------- | ----------------------- |
| GET    | `/profiles/{profileId}/watch-history`                       | Get viewing history     |
| GET    | `/profiles/{profileId}/continue-watching`                   | Get incomplete content  |
| PUT    | `/profiles/{profileId}/watch-progress/{contentId}`          | Update movie progress   |
| PUT    | `/profiles/{profileId}/episodes/{episodeId}/watch-progress` | Update episode progress |
| DELETE | `/profiles/{profileId}/watch-history/{historyId}`           | Remove history entry    |

### Update watch progress

`PUT /v1/profiles/{profileId}/watch-progress/{contentId}`

Request:

```
{
  "progressSeconds": 2100,
  "durationSeconds": 8880
}
```

Response:

```
{
  "contentId": "movie-101",
  "progressSeconds": 2100,
  "durationSeconds": 8880,
  "completionPercentage": 23.64,
  "updatedAt": "2026-11-05T08:30:00Z"
}
```

Why PUT?

Because we are updating the current playback progress resource to a specified state.

We should also consider out-of-order updates.

For example:

- Device A sends progress = 100 seconds.
- Device B sends progress = 200 seconds.
- Due to network delays, the 100-second request arrives last.

Without protection, progress could move backwards.

Possible solutions:

- Include a playback session ID and sequence number.
- Use optimistic versioning.
- Accept updates only from the current authorized playback session.
- Use server-side event timestamps carefully.

This becomes especially important when multiple devices are involved.

# 17. Playback APIs

This is the most interesting API design area.

A common mistake is to expose an endpoint like:

```
GET /movies/{movieId}/video
```

and directly return the entire video.

This is not how a large-scale streaming platform should work.

Instead, the backend authorizes playback and returns a short-lived playback manifest URL.

### Playback APIs

| Method | Endpoint                                   | Description          |
| ------ | ------------------------------------------ | -------------------- |
| POST   | `/playback-sessions`                       | Start playback       |
| GET    | `/playback-sessions/{sessionId}`           | Get session details  |
| PATCH  | `/playback-sessions/{sessionId}`           | Update session state |
| POST   | `/playback-sessions/{sessionId}/heartbeat` | Send heartbeat       |
| POST   | `/playback-sessions/{sessionId}/stop`      | Stop playback        |

### Start playback

`POST /v1/playback-sessions`

Request:

```
{
  "profileId": "profile-123",
  "deviceId": "device-123",
  "contentId": "movie-101",
  "startPositionSeconds": 2100
}
```

Response: `201 Created`

```
{
  "sessionId": "session-123",
  "status": "ACTIVE",
  "playback": {
    "manifestUrl": "https://cdn.example.com/signed/master.m3u8",
    "manifestType": "HLS",
    "expiresAt": "2026-11-05T09:00:00Z"
  },
  "resumePositionSeconds": 2100,
  "availableAudioTracks": [
    {
      "language": "en",
      "label": "English"
    },
    {
      "language": "hi",
      "label": "Hindi"
    }
  ],
  "availableSubtitles": [
    {
      "language": "en",
      "label": "English"
    },
    {
      "language": "hi",
      "label": "Hindi"
    }
  ]
}
```

### What happens internally?

Object StorageCDNPostgreSQLAuthorization ServicePlayback APIClientObject StorageCDNPostgreSQLAuthorization ServicePlayback APIClient#chatgpt-mermaid-\_r_2u9\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2u9\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2u9\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2u9\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2u9\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2u9\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2u9\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2u9\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2u9\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2u9\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2u9\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2u9\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2u9\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2u9\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2u9\_ p{margin:0;}#chatgpt-mermaid-\_r_2u9\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_2u9\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2u9\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2u9\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2u9\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2u9\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_2u9\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2u9\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2u9\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2u9\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_2u9\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2u9\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2u9\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2u9\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_2u9\_ .labelText,#chatgpt-mermaid-\_r_2u9\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2u9\_ .loopText,#chatgpt-mermaid-\_r_2u9\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2u9\_ .sectionTitle,#chatgpt-mermaid-\_r_2u9\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2u9\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_2u9\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2u9\_ .noteText,#chatgpt-mermaid-\_r_2u9\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_2u9\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2u9\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2u9\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2u9\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_2u9\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_2u9\_ .actor-man circle,#chatgpt-mermaid-\_r_2u9\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_2u9\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2u9\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2u9\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2u9\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2u9\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2u9\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2u9\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2u9\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2u9\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2u9\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2u9\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2u9\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2u9\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2u9\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2u9\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2u9\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2u9\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2u9\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2u9\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}POST /playback-sessionsValidate user and profileCheck subscriptionCheck content availabilityCheck concurrent stream limitCreate streaming sessionGenerate playback authorizationSession ID + signed manifest URLRequest manifestFetch manifest/segments if cache missManifest and video segmentsRequest subsequent segments

The important point is:

The application backend does not proxy the entire video stream.

Instead:

1. Backend authenticates the user.
2. Backend validates subscription and content permissions.
3. Backend creates a streaming session.
4. Backend provides a signed URL or playback token.
5. Client fetches video segments directly from the CDN.
6. CDN validates authorization and serves content.

For premium content, integrate DRM systems such as Widevine, FairPlay, or PlayReady, depending on device support and licensing requirements.

A signed URL alone is not equivalent to DRM.

### Heartbeat

`POST /v1/playback-sessions/{sessionId}/heartbeat`

Request:

```
{
  "positionSeconds": 2400,
  "sequenceNumber": 12
}
```

Response:

```
{
  "status": "ACTIVE",
  "nextHeartbeatAfterSeconds": 30
}
```

The heartbeat helps:

- Detect active sessions.
- Maintain concurrent-stream accounting.
- Detect abandoned sessions.
- Track playback progress.

### Stop playback

`POST /v1/playback-sessions/{sessionId}/stop`

Request:

```
{
  "positionSeconds": 3000
}
```

Response:

```
{
  "sessionId": "session-123",
  "status": "ENDED"
}
```

You could also use `DELETE /playback-sessions/{sessionId}` to end a session. Both approaches are reasonable; choose one consistent API convention.

# 18. Subscription APIs

| Method | Endpoint                                       | Description              |
| ------ | ---------------------------------------------- | ------------------------ |
| GET    | `/subscription-plans`                          | List plans               |
| GET    | `/users/me/subscription`                       | Get current subscription |
| POST   | `/subscriptions`                               | Create subscription      |
| PATCH  | `/subscriptions/{subscriptionId}`              | Change subscription      |
| POST   | `/subscriptions/{subscriptionId}/cancellation` | Cancel subscription      |
| GET    | `/users/me/payments`                           | Payment history          |
| POST   | `/webhooks/payments/{provider}`                | Payment provider webhook |

### Get available plans

`GET /v1/subscription-plans`

### Create subscription

`POST /v1/subscriptions`

Request:

```
{
  "planId": "premium-plan",
  "paymentMethodId": "payment-method-123"
}
```

Response:

```
{
  "subscriptionId": "subscription-123",
  "status": "ACTIVE",
  "plan": "PREMIUM",
  "startDate": "2026-11-05T08:00:00Z",
  "endDate": "2026-12-05T08:00:00Z"
}
```

Payment processing should generally be integrated through a payment provider.

Use idempotency keys for subscription creation and payment operations to avoid duplicate charges caused by retries.

# 19. Device APIs

| Method | Endpoint                                  | Description          |
| ------ | ----------------------------------------- | -------------------- |
| POST   | `/devices`                                | Register device      |
| GET    | `/devices`                                | List devices         |
| DELETE | `/devices/{deviceId}`                     | Remove device        |
| GET    | `/profiles/{profileId}/playback-sessions` | List active sessions |

These APIs support device management and account security.

# 20. API Summary

Here is a consolidated view of the core APIs.

| Domain       | Method | Endpoint                                    |
| ------------ | ------ | ------------------------------------------- |
| Auth         | POST   | `/auth/register`                            |
| Auth         | POST   | `/auth/login`                               |
| Auth         | POST   | `/auth/refresh`                             |
| Profiles     | GET    | `/profiles`                                 |
| Profiles     | POST   | `/profiles`                                 |
| Profiles     | PATCH  | `/profiles/{id}`                            |
| Content      | GET    | `/contents`                                 |
| Content      | GET    | `/contents/{id}`                            |
| Search       | GET    | `/contents/search`                          |
| Genres       | GET    | `/genres`                                   |
| Series       | GET    | `/series/{id}/seasons`                      |
| Episodes     | GET    | `/seasons/{id}/episodes`                    |
| Watchlist    | POST   | `/profiles/{id}/watchlist`                  |
| Watchlist    | GET    | `/profiles/{id}/watchlist`                  |
| Watchlist    | DELETE | `/profiles/{id}/watchlist/{contentId}`      |
| History      | GET    | `/profiles/{id}/watch-history`              |
| History      | PUT    | `/profiles/{id}/watch-progress/{contentId}` |
| Playback     | POST   | `/playback-sessions`                        |
| Playback     | POST   | `/playback-sessions/{id}/heartbeat`         |
| Playback     | POST   | `/playback-sessions/{id}/stop`              |
| Subscription | GET    | `/subscription-plans`                       |
| Subscription | POST   | `/subscriptions`                            |
| Subscription | GET    | `/users/me/subscription`                    |
| Devices      | POST   | `/devices`                                  |
| Devices      | GET    | `/devices`                                  |

# 21. Important Database and API Design Considerations

For a system like Netflix, database design alone is not sufficient. We need to consider the following production-level concerns.

| Concern                    | Design approach                           |
| -------------------------- | ----------------------------------------- |
| Large content catalog      | PostgreSQL + Elasticsearch/OpenSearch     |
| Video storage              | S3 or equivalent object storage           |
| Video delivery             | CDN                                       |
| Video quality              | Adaptive bitrate streaming (HLS/DASH)     |
| Watch progress writes      | Batched updates, async processing         |
| Watch history queries      | Index on profile and last watched time    |
| Concurrent streams         | Atomic session allocation                 |
| Subscription validation    | Subscription service/cache                |
| Regional restrictions      | Content availability and licensing checks |
| Recommendations            | Dedicated recommendation service          |
| Authentication             | OAuth 2.0 / OIDC                          |
| Premium content protection | DRM + signed playback authorization       |
| High read traffic          | Redis caching and CDN caching             |
| Content search             | Dedicated search infrastructure           |

## Important indexes

Some useful PostgreSQL indexes:

```
-- Browse published content
CREATE INDEX idx_contents_status_type
ON contents(status, content_type);

-- Find seasons of a series
CREATE INDEX idx_seasons_series
ON seasons(series_id, season_number);

-- Find episodes of a season
CREATE INDEX idx_episodes_season
ON episodes(season_id, episode_number);

-- Get profile watch history
CREATE INDEX idx_watch_history_profile_last_watched
ON watch_history(profile_id, last_watched_at DESC);

-- Get profile watchlist
CREATE INDEX idx_watchlist_profile
ON watchlist_items(profile_id, added_at DESC);

-- Find content by genre
CREATE INDEX idx_content_genres_genre
ON content_genres(genre_id, content_id);

-- Find active streaming sessions
CREATE INDEX idx_streaming_sessions_user_status
ON streaming_sessions(status, last_heartbeat_at);
```

The last index is useful for finding stale sessions, although a production implementation may also need user-level concurrency queries. Consider partial indexes for active sessions.

# 22. Final Architecture Perspective

At the initial stage, we do not need dozens of microservices.

We can begin with a modular monolith:

\#chatgpt-mermaid-\_r_2vj\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2vj\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2vj\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2vj\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2vj\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2vj\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2vj\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2vj\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2vj\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2vj\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2vj\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2vj\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2vj\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2vj\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2vj\_ p{margin:0;}#chatgpt-mermaid-\_r_2vj\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2vj\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2vj\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2vj\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2vj\_ .label text,#chatgpt-mermaid-\_r_2vj\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2vj\_ .node rect,#chatgpt-mermaid-\_r_2vj\_ .node circle,#chatgpt-mermaid-\_r_2vj\_ .node ellipse,#chatgpt-mermaid-\_r_2vj\_ .node polygon,#chatgpt-mermaid-\_r_2vj\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2vj\_ .rough-node .label text,#chatgpt-mermaid-\_r_2vj\_ .node .label text,#chatgpt-mermaid-\_r_2vj\_ .image-shape .label,#chatgpt-mermaid-\_r_2vj\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2vj\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2vj\_ .rough-node .label,#chatgpt-mermaid-\_r_2vj\_ .node .label,#chatgpt-mermaid-\_r_2vj\_ .image-shape .label,#chatgpt-mermaid-\_r_2vj\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2vj\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2vj\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2vj\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2vj\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2vj\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2vj\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2vj\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2vj\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2vj\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2vj\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2vj\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2vj\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2vj\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2vj\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2vj\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2vj\_ .icon-shape,#chatgpt-mermaid-\_r_2vj\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2vj\_ .icon-shape p,#chatgpt-mermaid-\_r_2vj\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2vj\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2vj\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2vj\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2vj\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2vj\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2vj\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2vj\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2vj\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2vj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2vj\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2vj\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2vj\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2vj\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2vj\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2vj\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2vj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2vj\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2vj\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2vj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2vj\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2vj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2vj\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2vj\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2vj\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2vj\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2vj\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2vj\_ .node rect,#chatgpt-mermaid-\_r_2vj\_ .node circle,#chatgpt-mermaid-\_r_2vj\_ .node ellipse,#chatgpt-mermaid-\_r_2vj\_ .node polygon,#chatgpt-mermaid-\_r_2vj\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2vj\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2vj\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2vj\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2vj\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2vj\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Web / Mobile / TVApplicationsAPI Gateway / BackendAuthenticationProfile ManagementContent ManagementSearchPlayback ManagementSubscription ManagementWatch History / WatchlistPostgreSQLSearch EngineRedis CacheCDNVideo Object StoragePayment Provider

As the platform grows, we can extract independent services based on scaling requirements and domain boundaries.

For example:

- Content Service
- Catalog/Search Service
- Playback Authorization Service
- Subscription Service
- Recommendation Service
- User Activity Service
- Media Processing Service

The media processing service would handle video ingestion, transcoding, packaging, and subtitle processing.

# 23. Interview Discussion: Important Design Decisions

These are questions an interviewer may ask after you present the database and API design.

1\. Why separate users and profiles?

One account can have multiple viewers. Each viewer needs independent watch history, watchlist, preferences, and recommendations.

2\. Why store movies and series in a common contents table?

They share common metadata and discovery functionality. We avoid duplicating common attributes and can query them uniformly.

3\. Why separate seasons and episodes?

A series has a hierarchical structure. Separate tables allow us to model season and episode relationships and retrieve them independently.

4\. Why not store video files in the database?

Video files are large binary objects. Object storage is cheaper and more suitable for large-scale media storage. CDN handles efficient global delivery.

5\. How do you prevent users from exceeding simultaneous streaming limits?

Maintain active streaming sessions and atomically allocate a slot before issuing playback authorization. Use locking or distributed coordination when requests can arrive concurrently.

6\. How do you support resume playback?

Store the playback position against the profile and content/episode. Periodically persist progress and return it when playback starts again.

7\. How do you support different video qualities?

Transcode the original video into multiple resolutions and bitrates, package them into HLS or MPEG-DASH, and use adaptive bitrate streaming.

8\. How do you prevent unauthorized video access?

Authenticate playback requests, validate subscriptions and regional licensing, issue short-lived playback authorization, and use DRM for protected content.

9\. How do you handle millions of watch-progress updates?

Avoid writing every playback event synchronously to PostgreSQL. Batch updates, ingest events asynchronously, and persist the latest progress. Partitioning and caching can help at higher volumes.

10\. How do you support regional content restrictions?

Maintain licensing availability by region and time window. Validate the user's trusted region before issuing playback authorization.

## 24. What I would emphasize in a system design interview

There are three particularly important design areas in this problem:

1. Content modeling: Correctly model movies, series, seasons, episodes, metadata, and video assets.
2. Playback architecture: Understand why video delivery happens through object storage and CDN rather than through application servers.
3. User activity and concurrency: Correctly manage watch progress, multiple devices, and simultaneous streaming limits.

The most important distinction to remember:

- PostgreSQL stores metadata, subscriptions, profiles, and user activity.
- Object storage stores actual video assets.
- CDN delivers video segments.
- Playback service authorizes access.
- Search engine supports catalog discovery.
- Redis helps with caching and short-lived session coordination.

This gives us a solid foundation for designing a Netflix/Prime Video-like streaming platform.










# Netflix / Prime Video – Microservices Architecture, Database Selection and Important Queries

Let's evolve our previous movie/series streaming system into a production-oriented microservices architecture.

The important part of this design is not simply splitting the application into multiple services. We need to understand:

1. How to identify service boundaries.
2. What data each service owns.
3. Which database is appropriate for each service.
4. Why we select that database instead of alternatives.
5. How services communicate.
6. How to maintain data consistency across services.
7. What important database queries each service executes.
8. How the architecture handles high traffic, concurrent requests, and failures.

I'll use PostgreSQL, Redis, OpenSearch, DynamoDB, Kafka, object storage, and other technologies where appropriate, but we will evaluate the trade-offs rather than blindly assigning a different database to every service.

An important principle:

> A microservice should own its data and expose capabilities through APIs or events. Other services should not directly query its database.

For example, the Playback Service should never directly query the Subscription Service's PostgreSQL database. It should obtain subscription entitlements through an API, a local read model, or an entitlement cache.

# 1. Identify the Microservices

First, let's divide the platform into business domains.

I would start with these services:

| #  | Microservice               | Responsibility                                                   | Primary database             |
| -- | -------------------------- | ---------------------------------------------------------------- | ---------------------------- |
| 1  | API Gateway / BFF          | Client routing, authentication enforcement, response aggregation | No dedicated DB              |
| 2  | Identity Service           | Registration, login, credentials, tokens                         | PostgreSQL                   |
| 3  | User & Profile Service     | User preferences, profiles, parental controls                    | PostgreSQL                   |
| 4  | Content Catalog Service    | Movies, series, seasons, episodes, metadata                      | PostgreSQL                   |
| 5  | Search Service             | Full-text search, filters, autocomplete                          | OpenSearch                   |
| 6  | Media Asset Service        | Video assets, audio tracks, subtitles, manifests                 | PostgreSQL + S3              |
| 7  | Media Processing Service   | Transcoding, packaging, thumbnails, subtitles                    | S3 + job/event storage       |
| 8  | Subscription Service       | Plans, subscriptions, entitlements                               | PostgreSQL                   |
| 9  | Payment Service            | Payment transactions, refunds, provider integration              | PostgreSQL                   |
| 10 | Playback Service           | Playback authorization, streaming sessions, concurrency limits   | Redis + PostgreSQL           |
| 11 | User Activity Service      | Watch history, playback progress, viewing events                 | DynamoDB                     |
| 12 | Watchlist & Rating Service | Watchlists, likes, dislikes                                      | PostgreSQL                   |
| 13 | Recommendation Service     | Personalized recommendations                                     | NoSQL / Vector DB (optional) |
| 14 | Notification Service       | Email, push notifications, subscription alerts                   | PostgreSQL + Queue           |
| 15 | Device Management Service  | Registered devices and device management                         | PostgreSQL                   |

We don't necessarily need all 15 services on day one.

For example, Media Asset Management and Media Processing could initially be part of one Media Service. Similarly, Watchlist and Rating can be combined.

The important thing is to establish good domain boundaries.

## 1.1 High-level architecture

\#chatgpt-mermaid-\_r_44t\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_44t\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_44t\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_44t\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_44t\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44t\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_44t\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_44t\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_44t\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_44t\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_44t\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_44t\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44t\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44t\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_44t\_ p{margin:0;}#chatgpt-mermaid-\_r_44t\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44t\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44t\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44t\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_44t\_ .label text,#chatgpt-mermaid-\_r_44t\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44t\_ .node rect,#chatgpt-mermaid-\_r_44t\_ .node circle,#chatgpt-mermaid-\_r_44t\_ .node ellipse,#chatgpt-mermaid-\_r_44t\_ .node polygon,#chatgpt-mermaid-\_r_44t\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_44t\_ .rough-node .label text,#chatgpt-mermaid-\_r_44t\_ .node .label text,#chatgpt-mermaid-\_r_44t\_ .image-shape .label,#chatgpt-mermaid-\_r_44t\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_44t\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_44t\_ .rough-node .label,#chatgpt-mermaid-\_r_44t\_ .node .label,#chatgpt-mermaid-\_r_44t\_ .image-shape .label,#chatgpt-mermaid-\_r_44t\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_44t\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_44t\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44t\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44t\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_44t\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_44t\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_44t\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_44t\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_44t\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_44t\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_44t\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44t\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44t\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_44t\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44t\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_44t\_ .icon-shape,#chatgpt-mermaid-\_r_44t\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_44t\_ .icon-shape p,#chatgpt-mermaid-\_r_44t\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_44t\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_44t\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_44t\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_44t\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_44t\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_44t\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_44t\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_44t\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_44t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44t\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_44t\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_44t\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_44t\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44t\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_44t\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_44t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44t\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_44t\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_44t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44t\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_44t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44t\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_44t\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_44t\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_44t\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_44t\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_44t\_ .node rect,#chatgpt-mermaid-\_r_44t\_ .node circle,#chatgpt-mermaid-\_r_44t\_ .node ellipse,#chatgpt-mermaid-\_r_44t\_ .node polygon,#chatgpt-mermaid-\_r_44t\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_44t\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_44t\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_44t\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_44t\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44t\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Web / Mobile / Smart TVAPI Gateway / BFFIdentity ServiceUser & Profile ServiceContent Catalog ServiceSearch ServicePlayback ServiceUser Activity ServiceWatchlist ServiceSubscription ServicePostgreSQLOpenSearchPostgreSQLPostgreSQLPostgreSQLRedisPostgreSQLDynamoDBPostgreSQLCDNVideo Object StorageKafkaRecommendation ServiceNotification ServiceEventsEventsEvents

Notice something important:

- Each service owns its database.
- Services communicate through APIs or events.
- Kafka distributes domain events.
- Redis handles high-speed, short-lived data.
- CDN and S3 handle actual video delivery.

Now let's understand each service and its database selection.

# 2. Identity Service

### Responsibility

The Identity Service handles authentication and identity-related operations.

It owns:

- User registration
- Login
- Password management
- Password reset
- Access token generation
- Refresh token management
- Account lockout
- Login security
- MFA (multi-factor authentication)

It does NOT own:

- Viewing profiles
- Watch history
- Subscription information
- Content information

### Database: PostgreSQL

Why PostgreSQL?

Identity data requires strong consistency and relational integrity.

For example, when a user registers, we must guarantee that two accounts cannot be created with the same email address.

PostgreSQL provides:

- ACID transactions.
- Unique constraints.
- Foreign keys.
- Reliable indexing.
- Strong consistency.
- Mature backup and recovery mechanisms.

A document database could store identity information, but there is little benefit here. The data is structured, relationships are simple, and transactional guarantees are important.

### Schema

users

| Column         | Type           |
| -------------- | -------------- |
| id             | UUID PK        |
| email          | VARCHAR UNIQUE |
| password_hash  | VARCHAR        |
| status         | VARCHAR        |
| email_verified | BOOLEAN        |
| created_at     | TIMESTAMP      |

refresh_tokens

| Column     | Type      |
| ---------- | --------- |
| id         | UUID PK   |
| user_id    | UUID      |
| token_hash | VARCHAR   |
| expires_at | TIMESTAMP |
| revoked_at | TIMESTAMP |
| created_at | TIMESTAMP |

login_attempts

| Column       | Type      |
| ------------ | --------- |
| id           | UUID PK   |
| user_id      | UUID      |
| ip_address   | INET      |
| status       | VARCHAR   |
| attempted_at | TIMESTAMP |

### Important queries

1\. Find user by email

```
SELECT id, email, password_hash, status
FROM users
WHERE email = :email;
```

The unique index on email makes this efficient.

2\. Check whether an email already exists

```
SELECT EXISTS (
    SELECT 1
    FROM users
    WHERE email = :email
);
```

In reality, registration should rely on the unique constraint rather than a check-then-insert pattern, because two registration requests could race.

3\. Create user

```
INSERT INTO users (
    id,
    email,
    password_hash,
    status,
    created_at
)
VALUES (
    :id,
    :email,
    :password_hash,
    'ACTIVE',
    NOW()
)
RETURNING id, email;
```

4\. Validate refresh token

```
SELECT id, user_id, expires_at
FROM refresh_tokens
WHERE token_hash = :token_hash
  AND revoked_at IS NULL
  AND expires_at > NOW();
```

Store only a cryptographic hash of refresh tokens, not the raw token.

### Why not Redis as the primary database?

Redis is excellent for:

- Login rate limiting.
- OTP expiry.
- Temporary authentication state.
- Token lookup caches.

But it should not be the authoritative identity store.

We need durable persistence, relational constraints, and reliable recovery.

A practical implementation could use Redis alongside PostgreSQL.

# 3. User & Profile Service

### Responsibility

This service manages account profiles and viewing preferences.

For example:

```
Account: John

Profiles:
    John
    Sarah
    Kids
```

Each profile has separate:

- Name and avatar.
- Preferred language.
- Maturity restrictions.
- Autoplay preferences.
- Accessibility preferences.
- Recommendation preferences.

### Database: PostgreSQL

Why?

Profile data is structured and relatively small.

We need:

- Relationships between users and profiles.
- Unique profile constraints where appropriate.
- Transactions when creating or updating profiles.
- Reliable parental-control settings.

PostgreSQL is a natural fit.

A document database could also work, especially if profile preferences become highly dynamic, but JSONB in PostgreSQL provides sufficient flexibility for most use cases.

### Schema

profiles

| Column             | Type      |
| ------------------ | --------- |
| id                 | UUID PK   |
| user_id            | UUID      |
| name               | VARCHAR   |
| avatar_url         | TEXT      |
| is_kids_profile    | BOOLEAN   |
| maturity_level     | VARCHAR   |
| preferred_language | VARCHAR   |
| created_at         | TIMESTAMP |

profile_preferences

| Column      | Type      |
| ----------- | --------- |
| profile_id  | UUID PK   |
| preferences | JSONB     |
| updated_at  | TIMESTAMP |

Example preferences:

```
{
  "autoplay": true,
  "audioLanguage": "en",
  "subtitleLanguage": "hi",
  "subtitleEnabled": true
}
```

### Important queries

1\. Get all profiles for an account

```
SELECT id, name, avatar_url, is_kids_profile
FROM profiles
WHERE user_id = :user_id
ORDER BY created_at;
```

2\. Verify profile ownership

```
SELECT id, name, maturity_level
FROM profiles
WHERE id = :profile_id
  AND user_id = :user_id;
```

This is important for authorization. Knowing a profile ID must not allow another account to access it.

3\. Update preferences

```
INSERT INTO profile_preferences (
    profile_id,
    preferences,
    updated_at
)
VALUES (
    :profile_id,
    :preferences::jsonb,
    NOW()
)
ON CONFLICT (profile_id)
DO UPDATE SET
    preferences = EXCLUDED.preferences,
    updated_at = NOW();
```

Why JSONB?

Different profiles may have different preference combinations. We don't necessarily want a new database column for every preference.

However, frequently filtered or queried preferences should have proper indexes or dedicated columns.

# 4. Content Catalog Service

This service is responsible for the actual movie and series catalog.

### Responsibility

- Create movies.
- Create series.
- Manage seasons.
- Manage episodes.
- Manage genres.
- Manage actors and directors.
- Manage descriptions.
- Manage release dates.
- Manage maturity ratings.
- Publish and unpublish content.

This service is the source of truth for content metadata.

### Database: PostgreSQL

This is one of the clearest cases for PostgreSQL.

Why?

Consider the relationships:

```
Movie
  ├── Genres
  ├── Actors
  ├── Directors
  └── Languages

Series
  ├── Seasons
  │     └── Episodes
  ├── Genres
  ├── Actors
  └── Directors
```

These relationships are naturally relational.

We also need transactional consistency.

For example, when publishing a series, we might need to validate its seasons, episodes, metadata, and associated assets.

PostgreSQL gives us:

- Foreign keys.
- Transactions.
- Unique constraints.
- Many-to-many relationships.
- Complex joins.
- JSONB for flexible metadata.

MongoDB is possible, but embedding all seasons and episodes into a series document becomes problematic when series are large or frequently updated.

### Schema

We can reuse the earlier schema:

- `contents`
- `seasons`
- `episodes`
- `genres`
- `content_genres`
- `people`
- `content_credits`

Additionally, introduce:

content_localizations

| Column                | Type    |
| --------------------- | ------- |
| id                    | UUID PK |
| content_id            | UUID    |
| language_code         | VARCHAR |
| localized_title       | VARCHAR |
| localized_description | TEXT    |

This allows the same movie to have different titles and descriptions in different languages.

### Important queries

1\. Browse published movies

```
SELECT
    id,
    title,
    description,
    release_date,
    poster_url
FROM contents
WHERE content_type = 'MOVIE'
  AND status = 'PUBLISHED'
ORDER BY release_date DESC
LIMIT 20 OFFSET 0;
```

Index:

```
CREATE INDEX idx_contents_published_movies
ON contents(release_date DESC)
WHERE content_type = 'MOVIE'
  AND status = 'PUBLISHED';
```

2\. Get series with seasons

```
SELECT
    c.id AS series_id,
    c.title,
    s.id AS season_id,
    s.season_number
FROM contents c
LEFT JOIN seasons s
    ON s.series_id = c.id
WHERE c.id = :series_id
ORDER BY s.season_number;
```

3\. Get episodes in a season

```
SELECT
    id,
    episode_number,
    title,
    duration_seconds,
    thumbnail_url
FROM episodes
WHERE season_id = :season_id
ORDER BY episode_number;
```

4\. Find content by genre

```
SELECT c.id, c.title, c.content_type
FROM contents c
JOIN content_genres cg
    ON cg.content_id = c.id
JOIN genres g
    ON g.id = cg.genre_id
WHERE g.name = 'ACTION'
  AND c.status = 'PUBLISHED'
ORDER BY c.release_date DESC
LIMIT 20;
```

### Important architectural decision: Search is a separate service

We should not make the Catalog Service responsible for every search query.

Why?

Catalog and search have different workloads:

- Catalog: transactional writes and metadata management.
- Search: high-volume reads, full-text queries, filtering, ranking, autocomplete.

We'll discuss that separately.

# 5. Search Service

### Responsibility

The Search Service provides catalog discovery capabilities.

Examples:

- Search movies by title.
- Search actors and directors.
- Search by genre.
- Filter by release year.
- Filter by language.
- Filter by content type.
- Autocomplete.
- Full-text search.
- Search ranking and relevance.

### Database: OpenSearch / Elasticsearch

Why not PostgreSQL?

PostgreSQL supports full-text search through `tsvector`, `tsquery`, and indexes such as GIN.

For a small or medium-sized catalog, PostgreSQL full-text search might be sufficient.

However, consider a platform with:

- Millions of content records.
- Hundreds of millions of search requests.
- Multiple languages.
- Fuzzy matching.
- Typo tolerance.
- Search suggestions.
- Relevance ranking.
- Faceted filtering.

OpenSearch is designed for this kind of workload.

It provides:

- Inverted indexes.
- Full-text search.
- Fuzzy search.
- Relevance scoring.
- Autocomplete.
- Aggregations.
- Distributed search.

### Why not use OpenSearch as the primary database?

Because OpenSearch is not our transactional source of truth.

It is optimized for searching, not for enforcing relational integrity and managing transactional business data.

For example, we don't want to rely on OpenSearch to guarantee that every episode references a valid season.

Therefore:

- PostgreSQL = source of truth.
- OpenSearch = search-optimized projection.

### How does data reach OpenSearch?

Suppose an administrator publishes a movie.

```
Admin
  |
  v
Content Catalog Service
  |
  v
PostgreSQL
  |
  v
Outbox Event
  |
  v
Kafka
  |
  v
Search Indexer
  |
  v
OpenSearch
```

The Catalog Service publishes a `ContentPublished` event.

The Search Service consumes it and updates its index.

This is eventually consistent.

For a short period, a newly published movie might exist in PostgreSQL but not yet appear in search.

That is an acceptable trade-off for most catalog discovery operations.

### OpenSearch document

We can denormalize the data into a single searchable document.

```
{
  "id": "movie-101",
  "title": "Inception",
  "description": "A thief enters people's dreams...",
  "type": "MOVIE",
  "genres": [
    "SCI_FI",
    "ACTION",
    "THRILLER"
  ],
  "cast": [
    "Leonardo DiCaprio",
    "Joseph Gordon-Levitt"
  ],
  "directors": [
    "Christopher Nolan"
  ],
  "releaseYear": 2010,
  "languages": [
    "en",
    "hi"
  ],
  "status": "PUBLISHED"
}
```

Notice that we intentionally duplicate information.

This is denormalization.

It is acceptable because the Search Service optimizes for reads rather than transactional writes.

### Important queries

1\. Search by title

```
{
  "query": {
    "multi_match": {
      "query": "incepton",
      "fields": [
        "title^5",
        "description",
        "cast^2",
        "directors^3"
      ],
      "fuzziness": "AUTO"
    }
  }
}
```

Here:

- `title^5` gives title matches higher priority.
- `directors^3` gives director matches additional weight.
- Fuzziness helps with spelling mistakes.

2\. Filter by genre and year

```
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "genres": "ACTION"
          }
        }
      ],
      "filter": [
        {
          "term": {
            "type": "MOVIE"
          }
        },
        {
          "term": {
            "releaseYear": 2010
          }
        },
        {
          "term": {
            "status": "PUBLISHED"
          }
        }
      ]
    }
  }
}
```

3\. Autocomplete

Use a `search_as_you_type` field or an appropriately configured completion suggester.

Example:

```
{
  "query": {
    "multi_match": {
      "query": "ince",
      "type": "bool_prefix",
      "fields": [
        "title",
        "title._2gram",
        "title._3gram"
      ]
    }
  }
}
```

### Trade-offs

| Advantages                 | Disadvantages             |
| -------------------------- | ------------------------- |
| Excellent full-text search | Additional infrastructure |
| Fuzzy matching             | Indexing overhead         |
| Distributed search         | Eventual consistency      |
| Flexible filtering         | Requires index management |
| Search relevance scoring   | Data duplication          |

My recommendation: PostgreSQL remains the source of truth. OpenSearch is a read-optimized search projection.

# 6. Media Asset Service

This service manages information about video files and their associated assets.

Remember that the Content Catalog Service knows what a movie is, while the Media Asset Service knows how that movie can be streamed.

### Responsibility

- Video asset metadata.
- Video resolution and bitrate.
- Audio tracks.
- Subtitle tracks.
- HLS/DASH manifests.
- Video encoding information.
- DRM-related asset references.
- Asset readiness and processing status.

### Database: PostgreSQL + S3

We need two different storage systems because we have two different types of data.

PostgreSQL stores metadata.

S3 stores actual files.

Example:

```
Media Asset Service
│
├── PostgreSQL
│   ├── Video asset metadata
│   ├── Audio track metadata
│   ├── Subtitle metadata
│   └── Manifest references
│
└── S3
    ├── Original video
    ├── 480p segments
    ├── 720p segments
    ├── 1080p segments
    ├── Audio files
    ├── Subtitle files
    └── HLS/DASH manifests
```

### Why not store videos in PostgreSQL?

Video files can be several gigabytes.

Storing them in PostgreSQL would:

- Increase database storage costs.
- Increase backup sizes.
- Increase database I/O.
- Make content delivery inefficient.
- Make global distribution difficult.

Object storage is much more suitable for large media files.

### Schema

video_assets

| Column               | Type          |
| -------------------- | ------------- |
| id                   | UUID PK       |
| content_id           | UUID nullable |
| episode_id           | UUID nullable |
| original_storage_key | TEXT          |
| status               | VARCHAR       |
| duration_seconds     | INTEGER       |
| created_at           | TIMESTAMP     |

video_renditions

| Column         | Type    |
| -------------- | ------- |
| id             | UUID PK |
| video_asset_id | UUID    |
| resolution     | VARCHAR |
| bitrate        | INTEGER |
| codec          | VARCHAR |
| manifest_key   | TEXT    |
| status         | VARCHAR |

audio_tracks

| Column         | Type    |
| -------------- | ------- |
| id             | UUID PK |
| video_asset_id | UUID    |
| language_code  | VARCHAR |
| codec          | VARCHAR |
| storage_key    | TEXT    |

subtitle_tracks

| Column         | Type    |
| -------------- | ------- |
| id             | UUID PK |
| video_asset_id | UUID    |
| language_code  | VARCHAR |
| format         | VARCHAR |
| storage_key    | TEXT    |

### Important queries

1\. Get available renditions

```
SELECT
    resolution,
    bitrate,
    codec,
    manifest_key
FROM video_renditions
WHERE video_asset_id = :asset_id
  AND status = 'READY'
ORDER BY bitrate;
```

2\. Get available audio tracks

```
SELECT
    language_code,
    codec,
    storage_key
FROM audio_tracks
WHERE video_asset_id = :asset_id;
```

3\. Find processing-ready assets

```
SELECT id, content_id, episode_id
FROM video_assets
WHERE status = 'READY'
  AND created_at >= :start_time;
```

### Why not DynamoDB?

DynamoDB could store asset metadata, but asset metadata has relationships with renditions, audio tracks, subtitles, and content.

PostgreSQL makes these relationships and administrative queries easier.

DynamoDB would be reasonable if the asset metadata access pattern were extremely simple and operated at massive scale.

However, the operational complexity and data modeling constraints would probably not justify it initially.

# 7. Media Processing Service

This service handles the actual video processing pipeline.

### Responsibility

When a content provider uploads a movie, the platform must prepare it for streaming.

Typical operations:

1. Receive uploaded video.
2. Validate the video.
3. Extract metadata.
4. Transcode into different resolutions.
5. Generate HLS/DASH manifests.
6. Generate thumbnails.
7. Process audio tracks.
8. Process subtitles.
9. Store output files in S3.
10. Notify the Content/Media Asset Services when processing completes.

### Database selection: S3 + workflow state store

I would not introduce a large relational database just for media processing.

Use:

- S3 for original and processed files.
- SQS or a workflow engine for processing jobs.
- DynamoDB for job execution state when using AWS.
- PostgreSQL in Media Asset Service for final published asset metadata.

For example, AWS Step Functions can orchestrate long-running processing workflows.

### Why DynamoDB for processing jobs?

A processing job has a relatively simple structure:

```
{
  "jobId": "job-101",
  "assetId": "asset-101",
  "status": "PROCESSING",
  "currentStep": "TRANSCODING_1080P",
  "retryCount": 2,
  "createdAt": "2026-11-05T08:00:00Z"
}
```

We primarily access jobs by `jobId`, status, or workflow execution.

DynamoDB is useful because:

- It handles high write throughput.
- It provides flexible job metadata.
- It supports conditional updates.
- It scales without managing database servers.

However, if job volume is moderate, PostgreSQL is perfectly adequate.

### Important queries

1\. Get job by ID

```
GetItem:
    PK = JOB#job-101
```

2\. Update job status atomically

```
UpdateExpression:
    SET #status = :status,
        currentStep = :step,
        updatedAt = :updatedAt

ConditionExpression:
    #status = :expectedStatus
```

The condition prevents an unexpected state transition.

3\. Find pending jobs

Use a GSI:

```
GSI1PK = STATUS#PENDING
GSI1SK = CREATED_AT#2026-11-05T08:00:00Z
```

Then query pending jobs in creation order.

Avoid using DynamoDB table scans for routine job polling.

### Important design consideration

Video transcoding is an expensive, long-running task.

We should not process it inside a normal HTTP request.

Instead:

AssetDBProcessorKafkaS3APIAdminAssetDBProcessorKafkaS3APIAdmin#chatgpt-mermaid-\_r_499\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_499\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_499\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_499\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_499\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_499\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_499\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_499\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_499\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_499\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_499\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_499\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_499\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_499\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_499\_ p{margin:0;}#chatgpt-mermaid-\_r_499\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_499\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_499\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_499\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_499\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_499\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_499\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_499\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_499\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_499\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_499\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_499\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_499\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_499\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_499\_ .labelText,#chatgpt-mermaid-\_r_499\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_499\_ .loopText,#chatgpt-mermaid-\_r_499\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_499\_ .sectionTitle,#chatgpt-mermaid-\_r_499\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_499\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_499\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_499\_ .noteText,#chatgpt-mermaid-\_r_499\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_499\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_499\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_499\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_499\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_499\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_499\_ .actor-man circle,#chatgpt-mermaid-\_r_499\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_499\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_499\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_499\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_499\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_499\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_499\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_499\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_499\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_499\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_499\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_499\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_499\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_499\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_499\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_499\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_499\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_499\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_499\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_499\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Upload requestStore original videoVideoUploaded event202 AcceptedStart processingRead original videoTranscode and packageStore renditionsSave asset metadataVideoProcessingCompleted

`202 Accepted` is appropriate because the processing operation is asynchronous.

# 8. Subscription Service

### Responsibility

This service manages the commercial entitlement of a user account.

It owns:

- Subscription plans.
- Subscription lifecycle.
- Plan changes.
- Renewal dates.
- Subscription cancellation.
- Grace periods.
- Subscription entitlements.

An important distinction:

Subscription and Payment should be separate bounded contexts.

The Subscription Service answers:

"Does this user have an active subscription, and what features are they entitled to?"

The Payment Service answers:

"Was the transaction successfully charged, refunded, or declined?"

### Database: PostgreSQL

Why?

Subscription operations require strong consistency.

For example:

- A subscription should not be activated twice.
- A renewal should not be processed twice.
- A subscription change should be atomic.
- A payment event should not incorrectly activate multiple subscriptions.

PostgreSQL supports all these requirements.

### Schema

subscription_plans

| Column           | Type    |
| ---------------- | ------- |
| id               | UUID PK |
| name             | VARCHAR |
| price            | DECIMAL |
| currency         | VARCHAR |
| billing_interval | VARCHAR |
| max_streams      | INTEGER |
| max_resolution   | VARCHAR |
| ads_enabled      | BOOLEAN |

subscriptions

| Column               | Type      |
| -------------------- | --------- |
| id                   | UUID PK   |
| user_id              | UUID      |
| plan_id              | UUID      |
| status               | VARCHAR   |
| start_date           | TIMESTAMP |
| current_period_start | TIMESTAMP |
| current_period_end   | TIMESTAMP |
| auto_renew           | BOOLEAN   |
| version              | BIGINT    |

subscription_entitlements

| Column  | Type    |
| ------- | ------- |
| id      | UUID PK |
| plan_id | UUID    |
| feature | VARCHAR |
| value   | JSONB   |

Example:

```
{
  "maxStreams": 4,
  "maxResolution": "4K",
  "downloadsAllowed": true,
  "adsEnabled": false
}
```

### Important queries

1\. Get active subscription

```
SELECT
    s.id,
    s.status,
    s.current_period_end,
    p.name AS plan_name,
    p.max_streams,
    p.max_resolution
FROM subscriptions s
JOIN subscription_plans p
    ON p.id = s.plan_id
WHERE s.user_id = :user_id
  AND s.status = 'ACTIVE'
  AND s.current_period_end > NOW()
ORDER BY s.current_period_end DESC
LIMIT 1;
```

2\. Renew subscription safely

```
UPDATE subscriptions
SET current_period_start = :new_start,
    current_period_end = :new_end,
    version = version + 1
WHERE id = :subscription_id
  AND status = 'ACTIVE'
  AND current_period_end = :expected_old_end
RETURNING id, current_period_end;
```

The expected old end date acts as an optimistic concurrency check.

In a real renewal workflow, payment success must also be verified before extending the subscription.

3\. Get entitlements for a plan

```
SELECT feature, value
FROM subscription_entitlements
WHERE plan_id = :plan_id;
```

### Why not Redis as the primary database?

Subscription data is financially important.

Redis can cache entitlements, but PostgreSQL should remain authoritative.

For example:

```
Subscription Service
       |
       v
PostgreSQL (Source of Truth)
       |
       v
SubscriptionUpdated Event
       |
       v
Kafka
       |
       v
Entitlement Cache
```

The Playback Service can consume subscription events and maintain a local entitlement projection.

This avoids calling the Subscription Service on every playback request.

However, we must consider cache staleness. A recently cancelled subscription should not continue receiving access indefinitely.

Use versioned entitlement data, invalidation events, short expiration policies, and authoritative revalidation where required.

# 9. Payment Service

### Responsibility

- Payment initiation.
- Payment provider integration.
- Payment status.
- Refunds.
- Payment retries.
- Webhook processing.
- Transaction reconciliation.

### Database: PostgreSQL

Payments require:

- ACID transactions.
- Unique transaction references.
- Idempotency.
- Auditability.
- Strong consistency.

This makes PostgreSQL a very good fit.

We should maintain an independent database from Subscription Service.

### Schema

payments

| Column                  | Type      |
| ----------------------- | --------- |
| id                      | UUID PK   |
| user_id                 | UUID      |
| subscription_id         | UUID      |
| amount                  | DECIMAL   |
| currency                | VARCHAR   |
| provider                | VARCHAR   |
| provider_transaction_id | VARCHAR   |
| status                  | VARCHAR   |
| created_at              | TIMESTAMP |

payment_idempotency

| Column          | Type       |
| --------------- | ---------- |
| idempotency_key | VARCHAR PK |
| request_hash    | VARCHAR    |
| payment_id      | UUID       |
| response        | JSONB      |
| created_at      | TIMESTAMP  |

payment_webhook_events

| Column            | Type           |
| ----------------- | -------------- |
| id                | UUID PK        |
| provider_event_id | VARCHAR UNIQUE |
| event_type        | VARCHAR        |
| payload           | JSONB          |
| processed_at      | TIMESTAMP      |

### Important queries

1\. Create payment

```
INSERT INTO payments (
    id,
    user_id,
    subscription_id,
    amount,
    currency,
    provider,
    status,
    created_at
)
VALUES (
    :id,
    :user_id,
    :subscription_id,
    :amount,
    :currency,
    :provider,
    'PENDING',
    NOW()
)
RETURNING id;
```

2\. Update payment status

```
UPDATE payments
SET status = 'SUCCESS'
WHERE id = :payment_id
  AND status = 'PENDING';
```

3\. Prevent duplicate webhook processing

```
INSERT INTO payment_webhook_events (
    id,
    provider_event_id,
    event_type,
    payload
)
VALUES (
    :id,
    :provider_event_id,
    :event_type,
    :payload::jsonb
)
ON CONFLICT (provider_event_id)
DO NOTHING
RETURNING id;
```

If the insert returns no row, the event has already been received.

### Important consistency consideration

Payment and Subscription are separate services.

We cannot have one PostgreSQL transaction covering both databases.

We need an event-driven workflow.

SubscriptionKafkaPayment ProviderPayment DBPaymentClientSubscriptionKafkaPayment ProviderPayment DBPaymentClient#chatgpt-mermaid-\_r_4an\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4an\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4an\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4an\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4an\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4an\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4an\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4an\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4an\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4an\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4an\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4an\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4an\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4an\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4an\_ p{margin:0;}#chatgpt-mermaid-\_r_4an\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_4an\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4an\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4an\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4an\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4an\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_4an\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4an\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4an\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4an\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_4an\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4an\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4an\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4an\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_4an\_ .labelText,#chatgpt-mermaid-\_r_4an\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4an\_ .loopText,#chatgpt-mermaid-\_r_4an\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4an\_ .sectionTitle,#chatgpt-mermaid-\_r_4an\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4an\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_4an\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4an\_ .noteText,#chatgpt-mermaid-\_r_4an\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_4an\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4an\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4an\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4an\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_4an\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_4an\_ .actor-man circle,#chatgpt-mermaid-\_r_4an\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_4an\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4an\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4an\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4an\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4an\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4an\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4an\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4an\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4an\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4an\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4an\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4an\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4an\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4an\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4an\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4an\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4an\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4an\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4an\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Create subscription paymentCreate PENDING paymentCharge paymentPayment successfulMark payment SUCCESSPaymentSucceededPaymentSucceededActivate subscription

Use the transactional outbox pattern in Payment Service to reliably publish `PaymentSucceeded`.

If subscription activation fails temporarily, the event can be retried.

The event consumer must be idempotent.

Important: The payment provider may send the same webhook multiple times. We must handle duplicate events safely.

# 10. Playback Service

This is one of the most critical services in the entire architecture.

Playback is a high-throughput, latency-sensitive operation.

Every time a user presses Play, this service is involved.

### Responsibility

- Authorize playback.
- Validate content availability.
- Validate subscription entitlements.
- Check parental restrictions.
- Enforce concurrent streaming limits.
- Create streaming sessions.
- Generate signed playback authorization.
- Maintain active sessions.
- Handle heartbeats.
- End expired sessions.

### Database: Redis + PostgreSQL

This service benefits from a combination of two databases.

Why not use only PostgreSQL?

Imagine:

- 10 million active users.
- Millions of simultaneous playback sessions.
- Heartbeat requests every 30 seconds.
- Frequent session updates.
- High concurrency.

Persisting every heartbeat in PostgreSQL would create unnecessary database load.

Redis is ideal for ephemeral, frequently updated session state.

PostgreSQL is useful for durable session records and audit/history requirements.

### What belongs where?

| Data                           | Storage                  | Reason                      |
| ------------------------------ | ------------------------ | --------------------------- |
| Active sessions                | Redis                    | Fast reads/writes           |
| Session heartbeat              | Redis                    | Frequent updates            |
| Concurrent stream count        | Redis                    | Atomic operations           |
| Playback session audit         | PostgreSQL               | Durable records             |
| Historical session information | PostgreSQL               | Reporting and investigation |
| Playback authorization         | Short-lived signed token | Stateless validation        |

### Redis data model

For a user with ID `user-123`:

```
Key:
streaming:user-123:sessions

Type:
Redis Sorted Set

Members:
session-1
session-2
session-3

Score:
Last heartbeat timestamp
```

Session details can be stored separately:

```
Key:
streaming:session-1

Value:
{
  "userId": "user-123",
  "profileId": "profile-123",
  "contentId": "movie-101",
  "deviceId": "device-101",
  "status": "ACTIVE"
}

TTL:
Session expiration duration
```

We use the account ID, not profile ID, for the concurrency limit because simultaneous-stream limits typically apply to the subscription account.

### Important Redis operations

1\. Find active sessions

```
ZRANGE streaming:user-123:sessions 0 -1
```

2\. Remove expired sessions

```
ZREMRANGEBYSCORE streaming:user-123:sessions -inf <cutoff_timestamp>
```

3\. Add or update heartbeat

```
ZADD streaming:user-123:sessions <timestamp> session-1
```

These commands illustrate the data model, but production session allocation should be performed atomically.

### How to enforce concurrent stream limits?

Suppose:

```
Premium Plan:
Maximum concurrent streams = 4
```

Two devices simultaneously request the fourth available slot.

If we implement:

```
1. Read current count.
2. Check count < 4.
3. Insert session.
```

both requests could read 3 and both could insert.

Now we have 5 sessions.

This is a classic race condition.

Solution: Redis Lua script or an equivalent atomic allocation operation.

Conceptually:

```
-- KEYS[1] = user's active session sorted set
-- ARGV[1] = current timestamp
-- ARGV[2] = session ID
-- ARGV[3] = maximum streams
-- ARGV[4] = expiry cutoff

redis.call(
    'ZREMRANGEBYSCORE',
    KEYS[1],
    '-inf',
    ARGV[4]
)

local count = redis.call('ZCARD', KEYS[1])

if count >= tonumber(ARGV[3]) then
    return 0
end

redis.call(
    'ZADD',
    KEYS[1],
    ARGV[1],
    ARGV[2]
)

return 1
```

The script executes atomically on Redis.

If it returns `1`, the session was allocated.

If it returns `0`, the user has reached the concurrent-stream limit.

Production considerations:

- Session creation must be idempotent.
- Use a lease/heartbeat expiration policy.
- Account for Redis failover and lost state.
- Make session creation and renewal operations atomic.
- Avoid relying on a client-provided heartbeat timestamp.
- Consider how a user changing subscription plans affects existing sessions.

For stronger guarantees across failures, session allocation can also use a durable coordination mechanism or a transactional database. Redis-only coordination has trade-offs when failover loses recent writes.

### Important PostgreSQL queries

1\. Create durable playback session

```
INSERT INTO streaming_sessions (
    id,
    user_id,
    profile_id,
    device_id,
    content_id,
    status,
    started_at
)
VALUES (
    :session_id,
    :user_id,
    :profile_id,
    :device_id,
    :content_id,
    'ACTIVE',
    NOW()
);
```

2\. End session

```
UPDATE streaming_sessions
SET status = 'ENDED',
    ended_at = NOW()
WHERE id = :session_id
  AND status = 'ACTIVE';
```

3\. Find stale sessions

```
SELECT id, user_id, last_heartbeat_at
FROM streaming_sessions
WHERE status = 'ACTIVE'
  AND last_heartbeat_at < NOW() - INTERVAL '2 minutes';
```

A background process can reconcile stale durable records with active session state.

### Why not DynamoDB instead of Redis?

DynamoDB is excellent for durable, highly scalable key-value workloads.

But Redis has advantages for this particular use case:

- Extremely low latency.
- Atomic Lua scripts.
- Sorted sets.
- TTL.
- Fast counter operations.
- Efficient session coordination.

DynamoDB could still work using conditional writes and transactions, but the implementation would be different.

Redis is my preferred choice for active playback coordination, while PostgreSQL retains durable session records.

# 11. User Activity Service

This service tracks user viewing behavior.

It is different from Playback Service.

Playback Service handles the active session.

User Activity Service handles the history of what the user watched.

### Responsibility

- Watch history.
- Playback progress.
- Completed episodes.
- Continue watching.
- Viewing events.
- Viewing analytics.

### Database: DynamoDB

Why DynamoDB instead of PostgreSQL?

Consider the workload:

Every active viewer periodically sends playback progress.

For example:

```
User watches a movie for 2 hours.

Progress update every 30 seconds.

2 hours / 30 seconds = 240 updates.
```

For millions of users, this creates a significant write workload.

DynamoDB is a good fit for:

- High write throughput.
- Key-value access.
- Predictable query patterns.
- Horizontal scaling.
- TTL for temporary records.

However, this does not mean DynamoDB is automatically better than PostgreSQL.

PostgreSQL can handle substantial write workloads with batching, partitioning, connection pooling, and proper indexing.

DynamoDB becomes attractive when activity volume is very large and the access patterns are predictable.

### Data model

We can model activity by profile.

Partition key:

```
PK = PROFILE#profile-123
```

Sort key:

```
SK = CONTENT#movie-101
```

Example item:

```
{
  "PK": "PROFILE#profile-123",
  "SK": "CONTENT#movie-101",
  "contentId": "movie-101",
  "contentType": "MOVIE",
  "progressSeconds": 2100,
  "durationSeconds": 8880,
  "completionPercentage": 23.64,
  "lastWatchedAt": "2026-11-05T08:30:00Z"
}
```

For episode progress:

```
PK = PROFILE#profile-123
SK = EPISODE#episode-101
```

### Important access patterns

We should design the partition and sort keys around the queries we need.

1\. Get watch history for a profile

```
Query:
PK = PROFILE#profile-123

Sort:
lastWatchedAt DESC
```

For this access pattern, a GSI can use:

```
GSI1PK = PROFILE#profile-123
GSI1SK = WATCHED_AT#2026-11-05T08:30:00Z
```

2\. Get playback progress for a movie

```
GetItem:
PK = PROFILE#profile-123
SK = CONTENT#movie-101
```

3\. Update playback progress

Use an atomic `UpdateItem`:

```
UpdateExpression:

SET progressSeconds = :progress,
    lastWatchedAt = :watchedAt,
    completionPercentage = :percentage
```

For concurrent devices, use a session ID and monotonic sequence number or a conditional write to prevent older updates from overwriting newer progress.

4\. Get continue watching

Use a dedicated GSI:

```
GSI2PK = PROFILE#profile-123
GSI2SK = PROGRESS#23.64#movie-101
```

Query only records that are not completed.

In practice, the sort-key design should make the desired ordering efficient. You can also maintain a separate continue-watching projection.

### Why not MongoDB?

MongoDB could also be used.

It provides:

- Flexible documents.
- Horizontal scaling.
- Rich document queries.
- Convenient nested data.

But for this workload, we generally need a small number of predictable access patterns:

- Get one profile's progress.
- Update one content's progress.
- List recent history.
- List incomplete content.

DynamoDB is attractive because we can design directly around these access patterns and scale the write workload.

### An important improvement: Separate raw events from latest progress

There are actually two different kinds of activity data:

| Data                     | Purpose                       | Storage           |
| ------------------------ | ----------------------------- | ----------------- |
| Latest playback position | Resume playback               | DynamoDB          |
| Raw viewing events       | Analytics and recommendations | Kafka + data lake |

For example:

```
Playback Service
      |
      v
Kafka
      |
      +----> User Activity Consumer
      |              |
      |              v
      |          DynamoDB
      |
      +----> Analytics Consumer
      |              |
      |              v
      |          S3 / Data Lake
      |
      +----> Recommendation Consumer
```

We shouldn't store every heartbeat forever in the same table used for resume playback.

That would mix operational data with analytical event data.

# 12. Watchlist & Rating Service

### Responsibility

- Add content to watchlist.
- Remove content from watchlist.
- List watchlist.
- Like content.
- Dislike content.
- Change rating.

### Database: PostgreSQL

Why PostgreSQL?

These are straightforward relational operations.

A profile has many watchlist entries, and each entry references content.

We need:

- Uniqueness.
- Efficient filtering.
- Transactions.
- Simple pagination.

PostgreSQL is a better default than introducing DynamoDB for this relatively straightforward workload.

### Schema

watchlist_items

| Column     | Type      |
| ---------- | --------- |
| id         | UUID PK   |
| profile_id | UUID      |
| content_id | UUID      |
| added_at   | TIMESTAMP |

Constraint:

```
UNIQUE(profile_id, content_id)
```

content_ratings

| Column     | Type      |
| ---------- | --------- |
| id         | UUID PK   |
| profile_id | UUID      |
| content_id | UUID      |
| rating     | VARCHAR   |
| updated_at | TIMESTAMP |

Constraint:

```
UNIQUE(profile_id, content_id)
```

### Important queries

1\. Add content to watchlist

```
INSERT INTO watchlist_items (
    id,
    profile_id,
    content_id,
    added_at
)
VALUES (
    :id,
    :profile_id,
    :content_id,
    NOW()
)
ON CONFLICT (profile_id, content_id)
DO NOTHING;
```

2\. Get watchlist

```
SELECT
    content_id,
    added_at
FROM watchlist_items
WHERE profile_id = :profile_id
ORDER BY added_at DESC
LIMIT 20;
```

3\. Remove from watchlist

```
DELETE FROM watchlist_items
WHERE profile_id = :profile_id
  AND content_id = :content_id;
```

4\. Like or dislike content

```
INSERT INTO content_ratings (
    id,
    profile_id,
    content_id,
    rating,
    updated_at
)
VALUES (
    :id,
    :profile_id,
    :content_id,
    :rating,
    NOW()
)
ON CONFLICT (profile_id, content_id)
DO UPDATE SET
    rating = EXCLUDED.rating,
    updated_at = NOW();
```

### Why not DynamoDB?

Watchlist access patterns are relatively simple, and relational constraints are useful.

If the platform grows to an enormous number of interactions, DynamoDB is a viable alternative.

But PostgreSQL is the better starting point.

# 13. Recommendation Service

This service generates personalized content recommendations.

Examples:

- Because you watched Breaking Bad.
- Trending in your region.
- Recommended for you.
- Similar to Inception.
- Popular in the Thriller genre.

### Responsibility

- Personalized recommendations.
- Content similarity.
- Collaborative filtering.
- Trending content.
- User preference modeling.
- Recommendation ranking.

### Database selection: Multiple storage systems

This is a case where using multiple storage technologies makes sense.

| Technology             | Purpose                     |
| ---------------------- | --------------------------- |
| Redis                  | Cache recommendations       |
| Vector database        | Semantic content similarity |
| Data lake / warehouse  | Historical viewing data     |
| DynamoDB or PostgreSQL | Recommendation metadata     |

Let's understand why.

### Why a vector database?

Suppose a user likes:

- Inception
- Interstellar
- The Matrix

We want to find semantically similar movies.

We can generate embeddings for content descriptions and other metadata.

Example:

```
Inception → [0.12, 0.45, 0.89, ...]
Interstellar → [0.15, 0.41, 0.92, ...]
The Matrix → [0.11, 0.48, 0.85, ...]
```

A vector database can find content with similar embeddings.

Possible choices:

- Qdrant
- Milvus
- Weaviate
- OpenSearch vector search
- PostgreSQL with pgvector

My recommendation: Start with pgvector if the recommendation workload is modest and PostgreSQL is already available. Move to Qdrant or another specialized vector database when scale, indexing requirements, or retrieval performance justify it.

But embeddings alone do not make a good recommendation engine.

We also need behavioral signals:

- What users watched.
- What they completed.
- What they skipped.
- What they liked.
- What they added to their watchlist.

### Recommendation flow

\#chatgpt-mermaid-\_r_4et\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4et\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4et\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4et\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4et\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4et\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4et\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4et\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4et\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4et\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4et\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4et\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4et\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4et\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4et\_ p{margin:0;}#chatgpt-mermaid-\_r_4et\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4et\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4et\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4et\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4et\_ .label text,#chatgpt-mermaid-\_r_4et\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4et\_ .node rect,#chatgpt-mermaid-\_r_4et\_ .node circle,#chatgpt-mermaid-\_r_4et\_ .node ellipse,#chatgpt-mermaid-\_r_4et\_ .node polygon,#chatgpt-mermaid-\_r_4et\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4et\_ .rough-node .label text,#chatgpt-mermaid-\_r_4et\_ .node .label text,#chatgpt-mermaid-\_r_4et\_ .image-shape .label,#chatgpt-mermaid-\_r_4et\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4et\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4et\_ .rough-node .label,#chatgpt-mermaid-\_r_4et\_ .node .label,#chatgpt-mermaid-\_r_4et\_ .image-shape .label,#chatgpt-mermaid-\_r_4et\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4et\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4et\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4et\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4et\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4et\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4et\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4et\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4et\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4et\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4et\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4et\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4et\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4et\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4et\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4et\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4et\_ .icon-shape,#chatgpt-mermaid-\_r_4et\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4et\_ .icon-shape p,#chatgpt-mermaid-\_r_4et\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4et\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4et\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4et\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4et\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4et\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4et\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4et\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4et\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4et\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4et\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4et\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4et\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4et\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4et\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4et\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4et\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4et\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4et\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4et\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4et\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4et\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4et\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4et\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4et\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4et\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4et\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4et\_ .node rect,#chatgpt-mermaid-\_r_4et\_ .node circle,#chatgpt-mermaid-\_r_4et\_ .node ellipse,#chatgpt-mermaid-\_r_4et\_ .node polygon,#chatgpt-mermaid-\_r_4et\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4et\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4et\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4et\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4et\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4et\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User Viewing EventsKafkaFeature ProcessingData Lake / WarehouseEmbedding GenerationVector DatabaseRecommendation ModelRedis RecommendationCacheClient ApplicationRecommendation API

### Important queries

1\. Get recommendations

```
Redis:

GET recommendations:profile-123
```

Example response:

```
{
  "profileId": "profile-123",
  "contentIds": [
    "movie-101",
    "movie-102",
    "movie-103"
  ],
  "generatedAt": "2026-11-05T08:00:00Z"
}
```

2\. Find similar movies using pgvector

If using PostgreSQL with pgvector:

```
SELECT
    content_id,
    embedding <=> :query_embedding AS distance
FROM content_embeddings
ORDER BY embedding <=> :query_embedding
LIMIT 20;
```

`<=>` calculates cosine distance when the vector index and metric are configured appropriately.

3\. Find trending content

This is better computed from aggregated viewing events than by repeatedly scanning every raw event.

For example, a materialized analytics table could be:

```
SELECT
    content_id,
    SUM(watch_seconds) AS total_watch_seconds
FROM content_daily_metrics
WHERE metric_date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY content_id
ORDER BY total_watch_seconds DESC
LIMIT 20;
```

### Trade-offs

| Approach              | Advantage                     | Disadvantage                                        |
| --------------------- | ----------------------------- | --------------------------------------------------- |
| PostgreSQL + pgvector | Simple, integrated            | May have scaling limitations                        |
| Qdrant                | Specialized vector retrieval  | Additional infrastructure                           |
| Redis                 | Extremely fast cached results | Not suitable as full recommendation source of truth |
| Data warehouse        | Excellent analytical queries  | Not suitable for low-latency serving                |

# 14. Device Management Service

### Responsibility

- Register devices.
- Remove devices.
- List registered devices.
- Track device type.
- Manage device limits.
- Track last active time.

### Database: PostgreSQL

Device registration is structured data with straightforward relational access patterns.

We don't need a specialized database.

### Schema

devices

| Column            | Type      |
| ----------------- | --------- |
| id                | UUID PK   |
| user_id           | UUID      |
| device_identifier | VARCHAR   |
| device_type       | VARCHAR   |
| device_name       | VARCHAR   |
| last_active_at    | TIMESTAMP |
| registered_at     | TIMESTAMP |
| status            | VARCHAR   |

Constraint:

```
UNIQUE(user_id, device_identifier)
```

### Important queries

1\. Get registered devices

```
SELECT
    id,
    device_name,
    device_type,
    last_active_at
FROM devices
WHERE user_id = :user_id
  AND status = 'ACTIVE'
ORDER BY last_active_at DESC;
```

2\. Register device

```
INSERT INTO devices (
    id,
    user_id,
    device_identifier,
    device_type,
    device_name,
    registered_at,
    status
)
VALUES (
    :id,
    :user_id,
    :device_identifier,
    :device_type,
    :device_name,
    NOW(),
    'ACTIVE'
)
ON CONFLICT (user_id, device_identifier)
DO UPDATE SET
    last_active_at = NOW(),
    status = 'ACTIVE';
```

3\. Deactivate device

```
UPDATE devices
SET status = 'INACTIVE'
WHERE id = :device_id
  AND user_id = :user_id;
```

# 15. Notification Service

### Responsibility

- Welcome emails.
- Subscription confirmation.
- Subscription renewal reminders.
- Payment failure alerts.
- Password reset notifications.
- New content notifications.
- Push notifications.

### Database: PostgreSQL + Kafka/SQS

Notification delivery is asynchronous.

We should not make a subscription API wait for an email provider to respond.

For example, after a successful payment:

```
Payment Service
      |
      v
Kafka: PaymentSucceeded
      |
      v
Notification Service
      |
      +----> Email Provider
      |
      +----> Push Notification Provider
```

### Database: PostgreSQL

Why?

We need durable notification records and delivery tracking.

### Schema

notifications

| Column            | Type      |
| ----------------- | --------- |
| id                | UUID PK   |
| user_id           | UUID      |
| notification_type | VARCHAR   |
| channel           | VARCHAR   |
| payload           | JSONB     |
| status            | VARCHAR   |
| retry_count       | INTEGER   |
| created_at        | TIMESTAMP |
| sent_at           | TIMESTAMP |

### Important queries

1\. Fetch pending notifications

```
SELECT id, payload, channel
FROM notifications
WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 100
FOR UPDATE SKIP LOCKED;
```

This is useful when multiple workers process notification jobs concurrently.

The workers should claim rows in a transaction and mark them as processing before committing.

2\. Mark notification as sent

```
UPDATE notifications
SET status = 'SENT',
    sent_at = NOW()
WHERE id = :notification_id
  AND status = 'PROCESSING';
```

3\. Retry failed notification

```
UPDATE notifications
SET status = 'PENDING',
    retry_count = retry_count + 1
WHERE id = :notification_id
  AND status = 'FAILED';
```

Use retry limits, exponential backoff, and a dead-letter mechanism.

# 16. Database Selection – Complete Comparison

Let's consolidate our database decisions.

| Service            | Database                            | Why this choice?                                     |
| ------------------ | ----------------------------------- | ---------------------------------------------------- |
| Identity           | PostgreSQL                          | Strong consistency, unique constraints, transactions |
| User & Profile     | PostgreSQL                          | Relational data, structured preferences              |
| Content Catalog    | PostgreSQL                          | Complex relationships, joins, transactional updates  |
| Search             | OpenSearch                          | Full-text search, fuzzy matching, ranking            |
| Media Asset        | PostgreSQL + S3                     | Relational metadata + large binary files             |
| Media Processing   | DynamoDB + S3 + SQS/Step Functions  | Flexible job state + asynchronous processing         |
| Subscription       | PostgreSQL                          | Financially important lifecycle and consistency      |
| Payment            | PostgreSQL                          | ACID transactions, idempotency, auditability         |
| Playback           | Redis + PostgreSQL                  | Fast active sessions + durable records               |
| User Activity      | DynamoDB                            | High-volume, predictable key-value access            |
| Watchlist & Rating | PostgreSQL                          | Simple relational CRUD and constraints               |
| Recommendation     | Redis + pgvector/Qdrant + Data Lake | Fast serving + similarity + analytics                |
| Device Management  | PostgreSQL                          | Structured device/account relationships              |
| Notification       | PostgreSQL + Kafka/SQS              | Durable tracking + asynchronous delivery             |

## 16.1 Why are we using multiple databases?

This is called Polyglot Persistence.

The idea is that different services have different data access patterns, so we choose the database that best fits each workload.

Consider these examples:

| Requirement                          | Best fit                               | Reason                              |
| ------------------------------------ | -------------------------------------- | ----------------------------------- |
| Find user by unique email            | PostgreSQL                             | Unique indexes                      |
| Get all episodes of a season         | PostgreSQL                             | Relational queries                  |
| Search "Incepton"                    | OpenSearch                             | Fuzzy full-text search              |
| Get active playback session          | Redis                                  | Very low latency                    |
| Enforce concurrent stream limit      | Redis Lua / transactional coordination | Atomic operations                   |
| Store a 4 GB movie                   | S3                                     | Object storage                      |
| Update watch progress at high volume | DynamoDB                               | Scalable key-value writes           |
| Find semantically similar movies     | Vector DB                              | Approximate nearest-neighbor search |
| Analyze 30 days of viewing events    | Data warehouse                         | Analytical queries                  |

The key principle:

Choose a database based on access patterns, consistency requirements, query patterns, and operational complexity—not because a particular database is popular.

# 17. How Do Services Communicate?

There are two main communication patterns.

1. Synchronous communication.
2. Asynchronous communication.

We need both.

## 17.1 Synchronous communication

Use REST or gRPC when a service needs an immediate response.

Example:

A user presses Play.

The Playback Service needs to authorize the request.

```
Client
  |
  v
API Gateway
  |
  v
Playback Service
  |
  +----> Profile Service
  |
  +----> Catalog Service
  |
  +----> Subscription Service
  |
  +----> Media Asset Service
  |
  v
Playback Authorization
  |
  v
Client
```

However, calling four or five services on every playback request increases latency and creates cascading failure risks.

For this reason, I would optimize playback authorization using local read models and caches.

For example:

- Profile restrictions can be cached.
- Subscription entitlements can be consumed from Kafka.
- Content availability can be cached.
- Media manifest metadata can be cached.

The Playback Service then performs most authorization checks locally.

It should still have a way to revalidate sensitive or potentially stale information.

## 17.2 Asynchronous communication

Use Kafka or a message broker when another service needs to react to an event but the original operation should not wait for it.

Example:

When a movie is published:

```
Content Catalog Service
        |
        v
ContentPublished Event
        |
        v
      Kafka
        |
        +----> Search Service
        |
        +----> Recommendation Service
        |
        +----> Notification Service
```

The Content Catalog Service does not need to wait for every consumer.

### Important domain events

| Event                    | Producer         | Consumers                  |
| ------------------------ | ---------------- | -------------------------- |
| UserRegistered           | Identity         | Profile, Notification      |
| ProfileCreated           | Profile          | Recommendation             |
| ContentPublished         | Catalog          | Search, Recommendation     |
| ContentUpdated           | Catalog          | Search, Recommendation     |
| VideoUploaded            | Media Asset      | Media Processing           |
| VideoProcessingCompleted | Media Processing | Media Asset, Catalog       |
| SubscriptionCreated      | Subscription     | Notification, Playback     |
| SubscriptionUpdated      | Subscription     | Playback, Notification     |
| PaymentSucceeded         | Payment          | Subscription, Notification |
| PaymentFailed            | Payment          | Subscription, Notification |
| PlaybackStarted          | Playback         | Activity, Analytics        |
| PlaybackProgressUpdated  | Playback         | Activity, Analytics        |
| PlaybackCompleted        | Playback         | Activity, Recommendation   |
| ContentLiked             | Watchlist/Rating | Recommendation             |

### Event structure

For example:

```
{
  "eventId": "event-101",
  "eventType": "ContentPublished",
  "aggregateId": "movie-101",
  "aggregateVersion": 3,
  "occurredAt": "2026-11-05T08:30:00Z",
  "payload": {
    "contentId": "movie-101",
    "title": "Inception",
    "contentType": "MOVIE"
  }
}
```

Important fields:

- `eventId`: Deduplication.
- `eventType`: Identifies the event.
- `aggregateId`: Identifies the affected entity.
- `aggregateVersion`: Helps detect out-of-order updates.
- `occurredAt`: Event timestamp.

Events should be versioned so consumers can handle schema evolution.

# 18. The Database-per-Service Pattern

This is one of the most important microservices concepts.

Suppose we have:

```
Content Catalog Service
       |
       v
Content DB

Subscription Service
       |
       v
Subscription DB

Payment Service
       |
       v
Payment DB

Playback Service
       |
       v
Playback DB
```

Each service owns its data.

Now consider this requirement:

"Get movie details along with subscription availability."

A common mistake would be:

```
SELECT *
FROM content_db.contents c
JOIN subscription_db.subscriptions s
    ON s.user_id = c.user_id;
```

This is wrong architecturally.

Why?

Because:

- Services become tightly coupled.
- One service depends on another service's schema.
- Independent deployments become difficult.
- Database changes become risky.
- Service ownership is violated.

Instead, use service APIs or events.

### Approach 1: API composition

```
API Gateway / BFF
     |
     +----> Catalog Service
     |
     +----> Subscription Service
     |
     v
Combine responses
```

Good for:

- Low-volume queries.
- Operations requiring current information.
- Simple aggregation.

Disadvantages:

- More network calls.
- Increased latency.
- Possible partial failures.

### Approach 2: Local read model

Suppose Playback Service frequently needs subscription entitlements.

Instead of calling Subscription Service every time, maintain a local projection.

```
Subscription Service
        |
        v
SubscriptionUpdated
        |
        v
      Kafka
        |
        v
Playback Service Consumer
        |
        v
Playback Entitlement Store
```

For example, Playback Service could maintain:

```
user-123
    plan = PREMIUM
    maxStreams = 4
    maxResolution = 4K
    subscriptionStatus = ACTIVE
    version = 12
```

Now Playback Service can authorize most requests without a synchronous network call.

This is called Database per Service with Local Read Models.

The trade-off is eventual consistency.

If a subscription is cancelled, the Playback Service might temporarily have stale entitlement information.

We must design the invalidation and revalidation strategy accordingly.

# 19. Cross-Service Transactions and Saga Pattern

Let's examine an important real-world workflow.

A user purchases a Premium subscription.

We have two independent services:

- Payment Service
- Subscription Service

We cannot use a single database transaction across both services.

What happens if payment succeeds but subscription activation fails?

We need a distributed transaction strategy.

One practical approach is the Saga Pattern.

### Workflow

\#chatgpt-mermaid-\_r_4i0\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4i0\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4i0\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4i0\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4i0\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i0\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4i0\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4i0\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4i0\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4i0\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4i0\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4i0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4i0\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4i0\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4i0\_ p{margin:0;}#chatgpt-mermaid-\_r_4i0\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i0\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i0\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i0\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4i0\_ .label text,#chatgpt-mermaid-\_r_4i0\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i0\_ .node rect,#chatgpt-mermaid-\_r_4i0\_ .node circle,#chatgpt-mermaid-\_r_4i0\_ .node ellipse,#chatgpt-mermaid-\_r_4i0\_ .node polygon,#chatgpt-mermaid-\_r_4i0\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4i0\_ .rough-node .label text,#chatgpt-mermaid-\_r_4i0\_ .node .label text,#chatgpt-mermaid-\_r_4i0\_ .image-shape .label,#chatgpt-mermaid-\_r_4i0\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4i0\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4i0\_ .rough-node .label,#chatgpt-mermaid-\_r_4i0\_ .node .label,#chatgpt-mermaid-\_r_4i0\_ .image-shape .label,#chatgpt-mermaid-\_r_4i0\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4i0\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4i0\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4i0\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4i0\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4i0\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4i0\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4i0\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4i0\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4i0\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4i0\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4i0\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i0\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i0\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4i0\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i0\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4i0\_ .icon-shape,#chatgpt-mermaid-\_r_4i0\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4i0\_ .icon-shape p,#chatgpt-mermaid-\_r_4i0\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4i0\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4i0\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4i0\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4i0\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4i0\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4i0\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4i0\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4i0\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4i0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4i0\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4i0\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4i0\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4i0\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4i0\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4i0\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4i0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4i0\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4i0\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4i0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4i0\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4i0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4i0\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4i0\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4i0\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4i0\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4i0\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4i0\_ .node rect,#chatgpt-mermaid-\_r_4i0\_ .node circle,#chatgpt-mermaid-\_r_4i0\_ .node ellipse,#chatgpt-mermaid-\_r_4i0\_ .node polygon,#chatgpt-mermaid-\_r_4i0\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4i0\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4i0\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4i0\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4i0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4i0\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User purchases PremiumSubscription createsPENDING recordPayment Service processespaymentPayment successful?Publish PaymentSucceededPublish PaymentFailedSubscription Serviceactivates planPublishSubscriptionActivatedNotify userSubscription remains inactiveNotify payment failureYesNo

### What if subscription activation fails?

Payment has already succeeded.

We should not simply lose the event.

Instead:

1. Payment Service persists the success event in its outbox.
2. Kafka delivers it.
3. Subscription Service consumes it.
4. If activation fails, the message is retried.
5. If retries are exhausted, the event goes to a dead-letter queue.
6. Operations can investigate and replay the event.

We also need idempotency.

For example:

```
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
);
```

Within one local transaction:

```
BEGIN;

INSERT INTO processed_events(event_id, processed_at)
VALUES (:event_id, NOW())
ON CONFLICT DO NOTHING;
```

If the event was already processed, skip its business operation.

Otherwise, activate the subscription and commit.

This ensures that duplicate event delivery does not activate the same subscription multiple times.

Important: The event processing record and business update must be in the same local database transaction.

# 20. Important Production-Level Database Considerations

Now let's discuss some database concerns that are especially important in this platform.

## 20.1 Indexing strategy

Every service should create indexes based on its query patterns.

Some examples:

Content Catalog

```
CREATE INDEX idx_contents_type_release
ON contents(content_type, release_date DESC);
```

Watchlist

```
CREATE INDEX idx_watchlist_profile_added
ON watchlist_items(profile_id, added_at DESC);
```

Watch History (PostgreSQL alternative)

```
CREATE INDEX idx_watch_history_profile_time
ON watch_history(profile_id, last_watched_at DESC);
```

Subscriptions

```
CREATE INDEX idx_subscriptions_user_status
ON subscriptions(user_id, status);
```

Playback Sessions

```
CREATE INDEX idx_playback_active_sessions
ON streaming_sessions(user_id, last_heartbeat_at)
WHERE status = 'ACTIVE';
```

Notice the partial index on active sessions.

We don't need to index every historical session when most queries only care about active sessions.

## 20.2 Partitioning

Some tables can become extremely large.

For example:

- Playback events.
- Watch history.
- Payment transactions.
- Notification records.

Partitioning can help.

Example: Partition playback events by month.

```
CREATE TABLE playback_events (
    id UUID NOT NULL,
    profile_id UUID NOT NULL,
    content_id UUID NOT NULL,
    event_type VARCHAR NOT NULL,
    occurred_at TIMESTAMP NOT NULL
)
PARTITION BY RANGE (occurred_at);
```

Create monthly partitions:

```
CREATE TABLE playback_events_2026_11
PARTITION OF playback_events
FOR VALUES FROM ('2026-11-01')
TO ('2026-12-01');
```

Advantages:

- Easier data retention.
- Smaller indexes per partition.
- Faster partition pruning for time-based queries.
- Easier archival and deletion.

But partitioning adds operational complexity. We should introduce it when table size and query performance justify it.

## 20.3 Caching strategy

Redis can also cache:

| Data                      | Cache key                    | TTL example   |
| ------------------------- | ---------------------------- | ------------- |
| Content details           | `content:{id}`               | 30 minutes    |
| Profile preferences       | `profile:{id}`               | 10 minutes    |
| Subscription entitlements | `entitlement:{userId}`       | 1–5 minutes   |
| Search results            | `search:{queryHash}`         | 1–5 minutes   |
| Recommendations           | `recommendation:{profileId}` | 15–60 minutes |
| Active sessions           | `session:{sessionId}`        | Lease-based   |

These are illustrative TTLs, not universal values.

Different data has different freshness requirements.

For example, a poster image can be cached for a long time, but subscription revocation requires much faster propagation.

## 20.4 Read replicas

PostgreSQL read replicas can help services with high read-to-write ratios.

For example:

```
Content Catalog Service
          |
          v
     PostgreSQL
       /     \
      v       v
   Primary  Read Replica
   (Writes)   (Reads)
```

Use the primary for writes and consistency-sensitive reads.

Use replicas for:

- Content browsing.
- Reporting.
- Non-critical reads.

Remember that replicas are asynchronous and can lag behind the primary.

Do not immediately read from a replica when the user expects to see a change they just made.

# 21. API Ownership Across Services

Let's map the REST APIs to the microservices.

| Service        | Endpoint                                        | Purpose                      |
| -------------- | ----------------------------------------------- | ---------------------------- |
| Identity       | `POST /auth/register`                           | Register                     |
| Identity       | `POST /auth/login`                              | Login                        |
| Identity       | `POST /auth/refresh`                            | Refresh token                |
| Profile        | `GET /profiles`                                 | List profiles                |
| Profile        | `POST /profiles`                                | Create profile               |
| Profile        | `PATCH /profiles/{id}`                          | Update profile               |
| Catalog        | `GET /contents`                                 | Browse catalog               |
| Catalog        | `GET /contents/{id}`                            | Get content details          |
| Catalog        | `GET /series/{id}/seasons`                      | Get seasons                  |
| Catalog        | `GET /seasons/{id}/episodes`                    | Get episodes                 |
| Search         | `GET /search`                                   | Search catalog               |
| Search         | `GET /search/suggestions`                       | Autocomplete                 |
| Media Asset    | `GET /media-assets/{id}`                        | Get asset metadata           |
| Subscription   | `GET /plans`                                    | Get plans                    |
| Subscription   | `POST /subscriptions`                           | Create subscription          |
| Subscription   | `GET /users/me/subscription`                    | Get subscription             |
| Payment        | `POST /payments`                                | Initiate payment             |
| Payment        | `GET /payments/{id}`                            | Get payment                  |
| Playback       | `POST /playback-sessions`                       | Start playback               |
| Playback       | `POST /playback-sessions/{id}/heartbeat`        | Heartbeat                    |
| Playback       | `DELETE /playback-sessions/{id}`                | End session                  |
| Activity       | `PUT /profiles/{id}/watch-progress/{contentId}` | Update progress              |
| Activity       | `GET /profiles/{id}/watch-history`              | Get history                  |
| Activity       | `GET /profiles/{id}/continue-watching`          | Get incomplete content       |
| Watchlist      | `GET /profiles/{id}/watchlist`                  | Get watchlist                |
| Watchlist      | `PUT /profiles/{id}/watchlist/{contentId}`      | Add to watchlist             |
| Watchlist      | `DELETE /profiles/{id}/watchlist/{contentId}`   | Remove from watchlist        |
| Rating         | `PUT /profiles/{id}/ratings/{contentId}`        | Like/dislike                 |
| Recommendation | `GET /profiles/{id}/recommendations`            | Personalized recommendations |
| Device         | `GET /devices`                                  | Get registered devices       |
| Device         | `POST /devices`                                 | Register device              |

A useful REST improvement here is using `PUT` for watchlist insertion when the desired resource is uniquely identified by profile and content.

For example:

```
PUT /v1/profiles/profile-123/watchlist/movie-101
```

Repeated calls result in the same desired state.

Also, the API Gateway can expose a unified public API while the internal services maintain their own resource boundaries.

# 22. Complete Microservices Architecture

Let's put everything together.

\#chatgpt-mermaid-\_r_4js\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4js\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4js\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4js\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4js\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4js\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4js\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4js\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4js\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4js\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4js\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4js\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4js\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4js\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4js\_ p{margin:0;}#chatgpt-mermaid-\_r_4js\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4js\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4js\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4js\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4js\_ .label text,#chatgpt-mermaid-\_r_4js\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4js\_ .node rect,#chatgpt-mermaid-\_r_4js\_ .node circle,#chatgpt-mermaid-\_r_4js\_ .node ellipse,#chatgpt-mermaid-\_r_4js\_ .node polygon,#chatgpt-mermaid-\_r_4js\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4js\_ .rough-node .label text,#chatgpt-mermaid-\_r_4js\_ .node .label text,#chatgpt-mermaid-\_r_4js\_ .image-shape .label,#chatgpt-mermaid-\_r_4js\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4js\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4js\_ .rough-node .label,#chatgpt-mermaid-\_r_4js\_ .node .label,#chatgpt-mermaid-\_r_4js\_ .image-shape .label,#chatgpt-mermaid-\_r_4js\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4js\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4js\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4js\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4js\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4js\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4js\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4js\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4js\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4js\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4js\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4js\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4js\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4js\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4js\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4js\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4js\_ .icon-shape,#chatgpt-mermaid-\_r_4js\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4js\_ .icon-shape p,#chatgpt-mermaid-\_r_4js\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4js\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4js\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4js\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4js\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4js\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4js\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4js\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4js\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4js\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4js\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4js\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4js\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4js\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4js\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4js\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4js\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4js\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4js\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4js\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4js\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4js\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4js\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4js\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4js\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4js\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4js\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4js\_ .node rect,#chatgpt-mermaid-\_r_4js\_ .node circle,#chatgpt-mermaid-\_r_4js\_ .node ellipse,#chatgpt-mermaid-\_r_4js\_ .node polygon,#chatgpt-mermaid-\_r_4js\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4js\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4js\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4js\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4js\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4js\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Event InfrastructureIntelligence ServicesStreaming ServicesCore Business ServicesKafkaRecommendation ServiceNotification ServicePlayback ServiceMedia Asset ServiceMedia Processing ServiceUser Activity ServiceIdentity ServiceProfile ServiceContent Catalog ServiceSearch ServiceSubscription ServicePayment ServiceWatchlist / Rating ServiceDevice Management ServiceWeb / Mobile / Smart TVAPI Gateway / BFFPostgreSQLPostgreSQLPostgreSQLOpenSearchPostgreSQLPostgreSQLPostgreSQLPostgreSQLRedisPostgreSQLDynamoDBPostgreSQLS3Vector DBRedisCDN

This diagram represents logical ownership. In an actual deployment, we may consolidate some databases and services initially.

# 23. Important End-to-End Workflows

Let's understand how the services collaborate in three important workflows.

## Workflow 1: User opens Netflix home page

Suppose John opens the application.

The home page should display:

- Continue watching.
- Trending movies.
- Recommended movies.
- Recently released movies.
- Popular series.
- Watchlist.

### Request flow

RedisCatalogRecommendationActivityProfileGatewayClientRedisCatalogRecommendationActivityProfileGatewayClient#chatgpt-mermaid-\_r_4k3\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4k3\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4k3\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4k3\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4k3\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4k3\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4k3\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4k3\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4k3\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4k3\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4k3\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4k3\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4k3\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4k3\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4k3\_ p{margin:0;}#chatgpt-mermaid-\_r_4k3\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_4k3\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4k3\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4k3\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4k3\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4k3\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_4k3\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4k3\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4k3\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4k3\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_4k3\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4k3\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4k3\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4k3\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_4k3\_ .labelText,#chatgpt-mermaid-\_r_4k3\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4k3\_ .loopText,#chatgpt-mermaid-\_r_4k3\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4k3\_ .sectionTitle,#chatgpt-mermaid-\_r_4k3\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4k3\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_4k3\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4k3\_ .noteText,#chatgpt-mermaid-\_r_4k3\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_4k3\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4k3\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4k3\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4k3\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_4k3\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_4k3\_ .actor-man circle,#chatgpt-mermaid-\_r_4k3\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_4k3\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4k3\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4k3\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4k3\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4k3\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4k3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4k3\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4k3\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4k3\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4k3\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4k3\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4k3\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4k3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4k3\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4k3\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4k3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4k3\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4k3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4k3\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}GET /homeGet profile preferencesGet continue watchingGet recommendationsGet trending/new releasesGet cached recommendationsRecommendation IDsRecommended IDsContinue watchingContent metadataProfile detailsAggregated home response

Potential optimization:

Do not perform all these calls sequentially.

The BFF should execute independent calls in parallel.

Also, the Recommendation Service can return content IDs while the Catalog Service provides the corresponding metadata.

At scale, we could create a dedicated Home Feed Service that maintains a precomputed feed.

That reduces the number of synchronous requests when opening the home page.

## Workflow 2: User starts playing a movie

Suppose John presses Play on Inception.

ActivityCDNCatalogSubscriptionRedisPlaybackGatewayClientActivityCDNCatalogSubscriptionRedisPlaybackGatewayClient#chatgpt-mermaid-\_r_4ka\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ka\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ka\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ka\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ka\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ka\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ka\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ka\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ka\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ka\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ka\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ka\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ka\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ka\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ka\_ p{margin:0;}#chatgpt-mermaid-\_r_4ka\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_4ka\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ka\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ka\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4ka\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ka\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_4ka\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ka\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ka\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ka\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_4ka\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ka\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ka\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4ka\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_4ka\_ .labelText,#chatgpt-mermaid-\_r_4ka\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4ka\_ .loopText,#chatgpt-mermaid-\_r_4ka\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4ka\_ .sectionTitle,#chatgpt-mermaid-\_r_4ka\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4ka\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_4ka\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ka\_ .noteText,#chatgpt-mermaid-\_r_4ka\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_4ka\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4ka\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4ka\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4ka\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_4ka\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_4ka\_ .actor-man circle,#chatgpt-mermaid-\_r_4ka\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_4ka\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ka\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ka\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ka\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ka\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ka\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ka\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ka\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ka\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ka\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ka\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ka\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ka\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ka\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ka\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ka\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ka\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ka\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ka\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Start playbackPOST /playback-sessionsCheck session allocationValidate entitlementValidate content availabilityAllocate sessionGenerate playback authorizationSigned manifest URLRequest HLS manifestManifestRequest video segmentsVideo segmentsPlaybackStarted event

In the optimized architecture, subscription and content availability checks would usually use local projections/caches rather than synchronous calls on every request.

The video segments go directly through the CDN.

The Playback Service is not responsible for transferring the video bytes.

## Workflow 3: User finishes watching an episode

Suppose John finishes Episode 1 of Breaking Bad.

We need to:

1. Save playback progress.
2. Mark the episode completed.
3. Update watch history.
4. Update recommendation signals.
5. Suggest the next episode.

\#chatgpt-mermaid-\_r_4kh\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4kh\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4kh\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4kh\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4kh\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kh\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4kh\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4kh\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4kh\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4kh\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4kh\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4kh\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4kh\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4kh\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4kh\_ p{margin:0;}#chatgpt-mermaid-\_r_4kh\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kh\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kh\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kh\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4kh\_ .label text,#chatgpt-mermaid-\_r_4kh\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kh\_ .node rect,#chatgpt-mermaid-\_r_4kh\_ .node circle,#chatgpt-mermaid-\_r_4kh\_ .node ellipse,#chatgpt-mermaid-\_r_4kh\_ .node polygon,#chatgpt-mermaid-\_r_4kh\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4kh\_ .rough-node .label text,#chatgpt-mermaid-\_r_4kh\_ .node .label text,#chatgpt-mermaid-\_r_4kh\_ .image-shape .label,#chatgpt-mermaid-\_r_4kh\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4kh\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4kh\_ .rough-node .label,#chatgpt-mermaid-\_r_4kh\_ .node .label,#chatgpt-mermaid-\_r_4kh\_ .image-shape .label,#chatgpt-mermaid-\_r_4kh\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4kh\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4kh\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4kh\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4kh\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4kh\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4kh\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4kh\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4kh\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4kh\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4kh\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4kh\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kh\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kh\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4kh\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kh\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4kh\_ .icon-shape,#chatgpt-mermaid-\_r_4kh\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4kh\_ .icon-shape p,#chatgpt-mermaid-\_r_4kh\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4kh\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4kh\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4kh\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4kh\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4kh\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4kh\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4kh\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4kh\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4kh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4kh\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4kh\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4kh\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4kh\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4kh\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4kh\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4kh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4kh\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4kh\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4kh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4kh\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4kh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4kh\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4kh\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4kh\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4kh\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4kh\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4kh\_ .node rect,#chatgpt-mermaid-\_r_4kh\_ .node circle,#chatgpt-mermaid-\_r_4kh\_ .node ellipse,#chatgpt-mermaid-\_r_4kh\_ .node polygon,#chatgpt-mermaid-\_r_4kh\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4kh\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4kh\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4kh\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4kh\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4kh\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Player completes episodePlayback ServiceKafkaUser Activity ServiceRecommendation ServiceDynamoDBUpdate user preferencesContent Catalog ServiceGet next episode

The next episode can be retrieved using the episode number and season relationship, or from a cached series episode listing.

We should not make recommendation processing block the playback completion response.

# 24. Important Interview Questions and Design Trade-offs

These are the questions I would expect in a senior backend/system design interview.

### Q1. Why are you using PostgreSQL for most services?

Because the majority of business data is structured and relational.

Having microservices does not mean we need a different database technology for every service.

PostgreSQL offers:

- ACID transactions.
- Referential integrity.
- Unique constraints.
- Complex queries.
- Mature operational tooling.

Using PostgreSQL across several services is completely reasonable, provided each service owns its data independently.

### Q2. Does database-per-service mean every service must have a separate database server?

No.

It means each service owns its data and controls access to it.

For example, initially:

```
PostgreSQL Cluster
    |
    ├── Identity Database
    ├── Profile Database
    ├── Catalog Database
    ├── Subscription Database
    └── Payment Database
```

These can run on the same PostgreSQL cluster.

As the platform grows, we can move high-load services to independent clusters.

The important requirement is logical ownership and no cross-service database access.

### Q3. Why use DynamoDB for watch history instead of PostgreSQL?

Because playback progress is a high-write-volume workload with predictable access patterns.

DynamoDB provides horizontal scaling and managed key-value access.

However, PostgreSQL can also handle significant write volume with:

- Partitioning.
- Batching.
- Connection pooling.
- Proper indexes.
- Read/write scaling.

I would start with PostgreSQL if the expected workload is moderate.

DynamoDB becomes more attractive when write throughput and operational scalability justify it.

### Q4. Why use Redis for Playback Service?

Playback needs extremely fast session operations.

We need to:

- Check active sessions.
- Update heartbeats.
- Allocate concurrent streaming slots.
- Expire stale sessions.

Redis provides atomic operations, Lua scripting, sorted sets, and TTL.

But Redis should not necessarily be the only source of truth for durable session records.

### Q5. Why not use Kafka for everything?

Kafka is an event streaming platform, not a replacement for a database.

Kafka is useful for:

- Event distribution.
- Asynchronous workflows.
- Event replay.
- Analytics pipelines.
- Decoupling services.

It is not the appropriate primary store for relational business data or current subscription state.

### Q6. How do you handle a failed Kafka consumer?

Use:

- Consumer retries.
- Exponential backoff.
- Dead-letter topics.
- Idempotent consumers.
- Consumer lag monitoring.
- Event replay.

Never assume that Kafka delivers every business event exactly once to every external database.

Even with Kafka's exactly-once capabilities, an external database transaction and Kafka offset commit are not automatically one atomic operation.

Design consumers to tolerate duplicate delivery.

### Q7. How do you handle subscription cancellation immediately?

Subscription Service updates its source of truth and publishes `SubscriptionCancelled`.

Playback Service consumes this event and invalidates the entitlement.

For sensitive authorization decisions, use short-lived playback authorization and appropriate revalidation.

Do not depend solely on a long-lived cache.

### Q8. Why not use MongoDB for Content Catalog?

MongoDB is a valid alternative.

However, the catalog contains relationships among:

- Series.
- Seasons.
- Episodes.
- Genres.
- Actors.
- Directors.
- Localizations.

PostgreSQL provides a natural relational model.

MongoDB could work if we intentionally model content as documents, but embedding large season and episode structures can introduce document size, update, and consistency challenges.

### Q9. What if OpenSearch is unavailable?

Catalog data remains available in PostgreSQL.

We can:

- Temporarily disable advanced search.
- Use PostgreSQL full-text search as a fallback.
- Retry indexing events.
- Rebuild the search index from catalog data/events.

Search unavailability should not mean that all movie details become inaccessible.

### Q10. What happens if Redis goes down?

Playback Service may lose access to its active-session coordination layer.

We should fail safely rather than blindly allow unlimited playback.

Possible strategies:

- Redis replication and managed failover.
- Short-lived session leases.
- Durable session reconciliation.
- Controlled degradation.
- A transactional database fallback for session allocation if required.

The exact behavior depends on how strictly we must enforce simultaneous-stream limits.

# 25. My Recommended Implementation Strategy

If I were building this system from scratch, I would not immediately deploy 15 independent services and 8 different database technologies.

That creates unnecessary operational complexity.

I would evolve the architecture in stages.

### Phase 1: Initial production architecture

Start with these services:

| Service                       | Database                               |
| ----------------------------- | -------------------------------------- |
| Identity + Profile            | PostgreSQL                             |
| Content Catalog               | PostgreSQL                             |
| Search                        | PostgreSQL Full Text Search            |
| Subscription + Payment        | PostgreSQL, separate logical ownership |
| Playback                      | Redis + PostgreSQL                     |
| User Activity + Watchlist     | PostgreSQL                             |
| Media Management + Processing | PostgreSQL + S3                        |

Infrastructure:

- Redis
- S3
- CDN
- SQS
- API Gateway

At this stage, a modular monolith is also a reasonable implementation choice for some domains.

### Phase 2: Growing platform

As traffic increases:

| Change                         | Reason                             |
| ------------------------------ | ---------------------------------- |
| Extract Search Service         | Search workload grows              |
| Introduce OpenSearch           | Advanced full-text search          |
| Introduce Kafka                | Multiple asynchronous consumers    |
| Extract User Activity          | High write throughput              |
| Move Activity to DynamoDB      | Predictable, high-volume access    |
| Extract Recommendation Service | Independent processing and scaling |
| Introduce vector search        | Content similarity                 |

### Phase 3: Large-scale streaming platform

At this stage:

- Independently scale Playback Service.
- Introduce regional playback authorization.
- Use Redis clusters for session coordination.
- Introduce multi-region catalog/search projections.
- Use a dedicated data lake and analytics warehouse.
- Build sophisticated recommendation pipelines.
- Introduce regional content licensing enforcement.
- Optimize CDN delivery and DRM integration.

# 26. Final Takeaways – What You Should Remember

For a system like Netflix, the database choice should be driven by the service's workload.

| Database          | Remember it for                                    |
| ----------------- | -------------------------------------------------- |
| PostgreSQL        | Transactional business data and relational queries |
| Redis             | Caching, ephemeral state, session coordination     |
| DynamoDB          | High-scale predictable key-value access            |
| OpenSearch        | Full-text search and filtering                     |
| S3                | Large binary objects and media files               |
| Kafka             | Event streaming and asynchronous communication     |
| Qdrant / pgvector | Vector similarity and semantic retrieval           |
| Data Warehouse    | Analytics and reporting                            |

There are five particularly important architectural decisions in this design:

1. Database ownership: Every microservice owns its data. No direct cross-service database queries.
2. Polyglot persistence: Choose storage based on access patterns rather than forcing everything into one database.
3. Event-driven architecture: Use Kafka for asynchronous communication and local read models.
4. Playback scalability: Use Redis for active session coordination and CDN for video delivery.
5. Distributed consistency: Use Saga, transactional outbox, idempotency, and eventual consistency where cross-service transactions are required.

My most important advice for the interview:

Don't just say:

"We will use PostgreSQL for one service, Redis for another, and DynamoDB for another."

Explain the reasoning:

- What data are we storing?
- What are the most frequent queries?
- What is the read/write ratio?
- Do we need ACID transactions?
- Do we need joins?
- Is the workload predictable?
- What consistency guarantees are required?
- What happens when the database is unavailable?
- What operational complexity are we introducing?

When you can answer these questions for every service, you're not just selecting databases. You're demonstrating actual distributed systems design judgment.
