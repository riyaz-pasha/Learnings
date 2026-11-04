# System Design: Live Sports Score System



System Design Interview

Database + REST API



A Live Sports Score System provides real-time scores, match status, team details, player statistics, and match events to users.

Examples of such systems include:

- ESPN
- Cricbuzz
- SofaScore

We will design a system that supports multiple sports such as Cricket, Football, Basketball, and Tennis.

The design will focus on:

1. Core database entities and schema.
2. Relationships between entities.
3. REST API design.
4. API request and response examples.
5. Best practices for live score updates.

## 1. Requirements

### Functional requirements

The system should allow users to:

- View all live matches.
- View upcoming and completed matches.
- View live scores for a particular match.
- View match details, teams, players, and statistics.
- View a chronological list of match events.
- View tournaments, leagues, and standings.
- Get score updates without refreshing the page manually.

### Non-functional requirements

- Low latency for live score updates.
- High availability.
- Read-heavy workload support.
- Data consistency for scores and match events.
- Scalability to support millions of concurrent users.
- Efficient caching of frequently accessed match data.

## 2. High-Level Architecture

[Best Practices for Real-Time Sports Data Processing](https://images.openai.com/static-rsc-4/TqmuMACEp_6O9MdUD_KtJVETRIemVXdmuZM991lc3bSXINnHhiPeUrboj9JkmBCwf5Nq94zd78KmxVKisahDNtL9M8FamXyb0UT44D-UlhhGpujVP3dUS9bSPlttM2eK0jVvfnTkSwDQksvaoF1KBjGzebSDpfLuE8e0yea9u4o?purpose=inline)

[blog.statpro.io](https://blog.statpro.io/best-practices-real-time-sports-data-processing/?utm_source=chatgpt.com)

The core flow:

1. A sports data provider sends score updates.
2. The ingestion service validates the updates.
3. The match service updates the score and match status.
4. The database stores the latest score and historical events.
5. The cache stores frequently requested live match data.
6. The API serves match details to clients.
7. WebSocket or Server-Sent Events (SSE) push updates to users.

For database design, we will use a relational database such as PostgreSQL.

Why PostgreSQL?

- Strong consistency.
- Foreign key constraints.
- Support for transactions.
- Good relational modeling.
- Efficient indexing.
- Suitable for structured match and player data.

For caching, we can use Redis.

For real-time delivery, we can use WebSockets or SSE.

# 3. Database Design

Let's design the database around the following core entities.

[database design - Soccer league ER diagram (implementing a many to many relationship) - Database Administrators Stack Exchange](https://images.openai.com/static-rsc-4/PgS33YiAAFzBSiQKBR4Ox6-cW37ROiBlzeEmCb-aeOqvJXrTRNARzuX6x72QWvcF7I1V4WH2b6_PjJcHXV-AprOyDwkrxMxv4MUev_8Zt7TZ1O7tN4IteDs0FYEY1DC4Jqu316dFscpwP6T1Ml6TMFHE_DQz4J55wZFmLQGvQrE?purpose=inline)

[dba.stackexchange.com](https://dba.stackexchange.com/questions/90588/soccer-league-er-diagram-implementing-a-many-to-many-relationship?utm_source=chatgpt.com)

## 3.1 Core Entities

| Entity             | Purpose                                        |
| ------------------ | ---------------------------------------------- |
| Sport              | Represents a sport such as Cricket or Football |
| Tournament         | Represents a league or competition             |
| Season             | Represents a specific season of a tournament   |
| Team               | Represents a participating team                |
| Player             | Represents a player                            |
| Match              | Represents a single match                      |
| Match Team         | Connects teams to matches                      |
| Match Score        | Stores the latest score                        |
| Match Event        | Stores individual match events                 |
| Player Match Stats | Stores player performance in a match           |
| Venue              | Represents the stadium or location             |

We will now define the basic schema for each entity.

## 3.2 Database Schema

The following schema uses PostgreSQL.

### A. Sport

Stores the types of sports supported by the system.

```
CREATE TABLE sports (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

Example records:

| id | name       | code       |
| -- | ---------- | ---------- |
| 1  | Cricket    | CRICKET    |
| 2  | Football   | FOOTBALL   |
| 3  | Basketball | BASKETBALL |

### B. Tournament

A tournament represents a competition such as IPL, FIFA World Cup, or NBA.

```
CREATE TABLE tournaments (
    id BIGSERIAL PRIMARY KEY,
    sport_id BIGINT NOT NULL REFERENCES sports(id),
    name VARCHAR(150) NOT NULL,
    short_name VARCHAR(50),
    country VARCHAR(100),
    format VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

Relationships:

- One sport can have many tournaments.
- One tournament belongs to one sport.

### C. Season

A tournament can have multiple seasons.

```
CREATE TABLE seasons (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL
        REFERENCES tournaments(id),

    name VARCHAR(100) NOT NULL,
    start_date DATE,
    end_date DATE,

    status VARCHAR(20) NOT NULL
        CHECK (status IN ('UPCOMING', 'ACTIVE', 'COMPLETED')),

    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

Example:

- Tournament: IPL
- Season: IPL 2026

### D. Team

Stores team information.

```
CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    sport_id BIGINT NOT NULL REFERENCES sports(id),

    name VARCHAR(150) NOT NULL,
    short_name VARCHAR(20),
    logo_url TEXT,
    country VARCHAR(100),

    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

Examples:

- India
- Royal Challengers Bengaluru
- Manchester United
- Los Angeles Lakers

A team can participate in multiple matches.

### E. Player

Stores player information.

```
CREATE TABLE players (
    id BIGSERIAL PRIMARY KEY,
    sport_id BIGINT NOT NULL REFERENCES sports(id),

    name VARCHAR(150) NOT NULL,
    date_of_birth DATE,
    nationality VARCHAR(100),
    role VARCHAR(50),
    image_url TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

Examples of roles:

- Cricket: Batter, Bowler, All-rounder, Wicketkeeper
- Football: Goalkeeper, Defender, Midfielder, Forward
- Basketball: Guard, Forward, Center

### F. Team Players

A player can play for different teams over time. We should not directly store `team_id` in the player table if we want to preserve historical team membership.

```
CREATE TABLE team_players (
    id BIGSERIAL PRIMARY KEY,

    team_id BIGINT NOT NULL REFERENCES teams(id),
    player_id BIGINT NOT NULL REFERENCES players(id),

    start_date DATE,
    end_date DATE,

    jersey_number VARCHAR(10),

    UNIQUE (team_id, player_id, start_date)
);
```

This table maintains the relationship between players and teams.

### G. Venue

Stores stadium or match location details.

```
CREATE TABLE venues (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(150) NOT NULL,
    city VARCHAR(100),
    country VARCHAR(100),

    capacity INT,

    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### H. Match

This is one of the most important entities in the system.

```
CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,

    season_id BIGINT NOT NULL REFERENCES seasons(id),
    venue_id BIGINT REFERENCES venues(id),

    match_number VARCHAR(50),

    scheduled_start TIMESTAMPTZ NOT NULL,
    actual_start TIMESTAMPTZ,
    actual_end TIMESTAMPTZ,

    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED'
        CHECK (
            status IN (
                'SCHEDULED',
                'LIVE',
                'HALFTIME',
                'PAUSED',
                'COMPLETED',
                'POSTPONED',
                'CANCELLED',
                'ABANDONED'
            )
        ),

    result_description TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

Examples of match status:

| Status    | Meaning                     |
| --------- | --------------------------- |
| SCHEDULED | Match has not started       |
| LIVE      | Match is in progress        |
| HALFTIME  | Break between halves        |
| PAUSED    | Temporarily paused          |
| COMPLETED | Match finished              |
| POSTPONED | Match moved to a later time |
| CANCELLED | Match cancelled             |

### I. Match Teams

A match can have multiple participating teams in some sports, but for the common two-team match model, we can use this table.

```
CREATE TABLE match_teams (
    id BIGSERIAL PRIMARY KEY,

    match_id BIGINT NOT NULL REFERENCES matches(id),
    team_id BIGINT NOT NULL REFERENCES teams(id),

    side VARCHAR(20) NOT NULL,
    is_winner BOOLEAN DEFAULT FALSE,

    UNIQUE (match_id, team_id),

    CHECK (side IN ('HOME', 'AWAY'))
);
```

Example:

| match_id | team_id | side |
| -------- | ------- | ---- |
| 101      | 1       | HOME |
| 101      | 2       | AWAY |

This allows us to identify the home team and away team.

For Cricket, these can instead be represented as Team 1 and Team 2, or the schema can use a sport-neutral participant position.

### J. Match Score

Stores the latest score of a match.

For a generic multi-sport design, the common score fields can be stored in columns and sport-specific details can be stored in JSONB.

```
CREATE TABLE match_scores (
    match_id BIGINT PRIMARY KEY REFERENCES matches(id),

    home_score INT NOT NULL DEFAULT 0,
    away_score INT NOT NULL DEFAULT 0,

    period VARCHAR(50),
    clock VARCHAR(50),

    sport_specific_data JSONB DEFAULT '{}'::JSONB,

    version BIGINT NOT NULL DEFAULT 1,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Example for Football:

```
{
  "home_score": 2,
  "away_score": 1,
  "period": "SECOND_HALF",
  "clock": "78:32",
  "sport_specific_data": {
    "home_shots": 10,
    "away_shots": 6,
    "home_possession": 58,
    "away_possession": 42
  }
}
```

Example for Cricket:

```
{
  "home_score": 185,
  "away_score": 0,
  "period": "INNINGS_1",
  "clock": "18.4",
  "sport_specific_data": {
    "runs": 185,
    "wickets": 4,
    "overs": "18.4",
    "target": null,
    "extras": 8
  }
}
```

Note: Cricket uses innings-based scoring, so home_score and away_score are not always sufficient to represent the complete match score. The sport-specific data and innings tables below handle that.

### K. Match Events

This table stores every important event during the match.

Examples:

- Goal scored
- Wicket taken
- Yellow card
- Red card
- Boundary
- Six
- Substitution

```
CREATE TABLE match_events (
    id BIGSERIAL PRIMARY KEY,

    match_id BIGINT NOT NULL REFERENCES matches(id),

    event_type VARCHAR(50) NOT NULL,

    team_id BIGINT REFERENCES teams(id),
    player_id BIGINT REFERENCES players(id),

    event_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    period VARCHAR(50),
    game_clock VARCHAR(50),

    sequence_number BIGINT NOT NULL,

    details JSONB DEFAULT '{}'::JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (match_id, sequence_number)
);
```

Example event:

```
{
  "event_type": "GOAL",
  "team_id": 1,
  "player_id": 25,
  "period": "SECOND_HALF",
  "game_clock": "78:32",
  "sequence_number": 150,
  "details": {
    "assist_player_id": 30,
    "goal_type": "NORMAL"
  }
}
```

The `sequence_number` is important because events need a stable ordering.

### L. Player Match Stats

Stores individual player performance for a match.

```
CREATE TABLE player_match_stats (
    id BIGSERIAL PRIMARY KEY,

    match_id BIGINT NOT NULL REFERENCES matches(id),
    player_id BIGINT NOT NULL REFERENCES players(id),
    team_id BIGINT NOT NULL REFERENCES teams(id),

    stats JSONB NOT NULL DEFAULT '{}'::JSONB,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (match_id, player_id)
);
```

Example Cricket stats:

```
{
  "runs": 75,
  "balls_faced": 42,
  "fours": 8,
  "sixes": 3,
  "strike_rate": 178.57
}
```

Example Football stats:

```
{
  "goals": 1,
  "assists": 1,
  "shots": 4,
  "passes_completed": 35,
  "yellow_cards": 0
}
```

For a production system, frequently queried statistics should be stored in typed columns or sport-specific tables instead of relying entirely on JSONB.

## 3.3 Additional Cricket-Specific Entities

Because a generic match score table is not sufficient for detailed Cricket scoring, we should add these entities.

### Match Innings

```
CREATE TABLE match_innings (
    id BIGSERIAL PRIMARY KEY,

    match_id BIGINT NOT NULL REFERENCES matches(id),
    innings_number INT NOT NULL,

    batting_team_id BIGINT NOT NULL REFERENCES teams(id),
    bowling_team_id BIGINT NOT NULL REFERENCES teams(id),

    runs INT NOT NULL DEFAULT 0,
    wickets INT NOT NULL DEFAULT 0,
    legal_balls INT NOT NULL DEFAULT 0,

    status VARCHAR(20) DEFAULT 'IN_PROGRESS',

    UNIQUE (match_id, innings_number)
);
```

### Cricket Ball Events

```
CREATE TABLE cricket_ball_events (
    id BIGSERIAL PRIMARY KEY,

    innings_id BIGINT NOT NULL REFERENCES match_innings(id),

    over_number INT NOT NULL,
    ball_number INT NOT NULL,

    batter_id BIGINT NOT NULL REFERENCES players(id),
    bowler_id BIGINT NOT NULL REFERENCES players(id),

    runs_batter INT NOT NULL DEFAULT 0,
    runs_extras INT NOT NULL DEFAULT 0,
    total_runs INT NOT NULL DEFAULT 0,

    is_wicket BOOLEAN DEFAULT FALSE,
    wicket_type VARCHAR(50),

    details JSONB DEFAULT '{}'::JSONB,

    UNIQUE (innings_id, over_number, ball_number)
);
```

This enables us to calculate:

- Total runs.
- Wickets.
- Overs.
- Batter scorecards.
- Bowler statistics.
- Ball-by-ball commentary.
- Fall of wickets.

## 3.4 Database Relationships

\#chatgpt-mermaid-\_r_bn\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_bn\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_bn\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_bn\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_bn\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bn\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_bn\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_bn\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_bn\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_bn\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_bn\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_bn\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bn\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bn\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_bn\_ p{margin:0;}#chatgpt-mermaid-\_r_bn\_ .entityBox{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_bn\_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_bn\_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-\_r_bn\_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-\_r_bn\_ .edgeLabel{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_bn\_ .edgeLabel .label rect{fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_bn\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bn\_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-\_r_bn\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bn\_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-\_r_bn\_ .node rect,#chatgpt-mermaid-\_r_bn\_ .node circle,#chatgpt-mermaid-\_r_bn\_ .node ellipse,#chatgpt-mermaid-\_r_bn\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_bn\_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-\_r_bn\_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-\_r_bn\_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-\_r_bn\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_bn\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_bn\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_bn\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_bn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bn\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_bn\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_bn\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_bn\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bn\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_bn\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_bn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bn\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_bn\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_bn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bn\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_bn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bn\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}SPORTSTOURNAMENTSSEASONSMATCHESVENUESTEAMSPLAYERSTEAM_PLAYERSMATCH_TEAMSMATCH_SCORESMATCH_EVENTSPLAYER_MATCH_STATSMATCH_INNINGSCRICKET_BALL_EVENTShascontainsscheduleshostscategorizescategorizesincludesjoinsincludesparticipateshasgeneratestracksperformscontainscontains

# 4. REST API Design

We will design APIs following REST principles.

## 4.1 API Design Best Practices

Our API should follow these rules:

| Best Practice              | Description                                          |
| -------------------------- | ---------------------------------------------------- |
| Resource-based URLs        | Use nouns such as `/matches`, `/teams`, `/players`   |
| HTTP methods               | GET, POST, PUT, PATCH, DELETE based on operation     |
| Stateless                  | Each request contains the necessary context          |
| Versioning                 | Prefix API with `/api/v1`                            |
| Pagination                 | Use pagination for large lists                       |
| Filtering                  | Support filtering by status, tournament, sport, date |
| Proper status codes        | Return appropriate HTTP status codes                 |
| Consistent response format | Use predictable JSON structures                      |
| Authentication             | Protect administrative and ingestion APIs            |
| Caching                    | Use ETags and Cache-Control where appropriate        |

Base URL:

```
https://api.sports.com/api/v1
```

## 4.2 API Endpoint Overview

Public read APIs

| Method | Endpoint                                | Purpose                  |
| ------ | --------------------------------------- | ------------------------ |
| GET    | `/sports`                               | Get all sports           |
| GET    | `/sports/{sportId}`                     | Get sport details        |
| GET    | `/tournaments`                          | Get tournaments          |
| GET    | `/tournaments/{tournamentId}`           | Get tournament details   |
| GET    | `/seasons/{seasonId}`                   | Get season details       |
| GET    | `/seasons/{seasonId}/matches`           | Get season matches       |
| GET    | `/matches`                              | Get matches with filters |
| GET    | `/matches/{matchId}`                    | Get match details        |
| GET    | `/matches/{matchId}/score`              | Get current score        |
| GET    | `/matches/{matchId}/events`             | Get match events         |
| GET    | `/matches/{matchId}/stats`              | Get match statistics     |
| GET    | `/matches/{matchId}/teams`              | Get participating teams  |
| GET    | `/matches/{matchId}/players`            | Get match players        |
| GET    | `/matches/{matchId}/innings`            | Get match innings        |
| GET    | `/matches/{matchId}/scorecard`          | Get detailed scorecard   |
| GET    | `/teams/{teamId}`                       | Get team details         |
| GET    | `/teams/{teamId}/matches`               | Get team matches         |
| GET    | `/players/{playerId}`                   | Get player details       |
| GET    | `/players/{playerId}/stats`             | Get player statistics    |
| GET    | `/tournaments/{tournamentId}/standings` | Get tournament standings |

## 4.3 Live Matches API



GET

`/api/v1/matches?status=LIVE`



This endpoint returns all currently live matches.

### Request

```
GET /api/v1/matches?status=LIVE&sport=CRICKET&page=1&limit=10
```

### Response

```
{
  "data": [
    {
      "id": 101,
      "sport": {
        "id": 1,
        "name": "Cricket"
      },
      "tournament": {
        "id": 10,
        "name": "IPL"
      },
      "teams": [
        {
          "id": 1,
          "name": "Team A",
          "shortName": "A"
        },
        {
          "id": 2,
          "name": "Team B",
          "shortName": "B"
        }
      ],
      "score": {
        "teamA": "185/4",
        "teamB": "142/6"
      },
      "status": "LIVE",
      "startTime": "2026-11-04T14:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 10,
    "total": 1,
    "totalPages": 1
  }
}
```

Why is this API useful?

The home page of a sports application usually displays many live matches. This API allows the frontend to retrieve them in a single request.

## 4.4 Get Match Details API



GET

`/api/v1/matches/{matchId}`



### Request

```
GET /api/v1/matches/101
```

### Response

```
{
  "data": {
    "id": 101,
    "sport": "Cricket",
    "tournament": "IPL",
    "season": "2026",
    "venue": {
      "id": 5,
      "name": "M. Chinnaswamy Stadium",
      "city": "Bengaluru"
    },
    "teams": [
      {
        "id": 1,
        "name": "Team A",
        "shortName": "A",
        "isWinner": false
      },
      {
        "id": 2,
        "name": "Team B",
        "shortName": "B",
        "isWinner": false
      }
    ],
    "status": "LIVE",
    "scheduledStart": "2026-11-04T14:00:00Z"
  }
}
```

This endpoint returns relatively stable information about the match.

The live score should be fetched from the score endpoint or included in an appropriate representation.

## 4.5 Get Live Score API



GET

`/api/v1/matches/{matchId}/score`



### Request

```
GET /api/v1/matches/101/score
```

### Response

```
{
  "data": {
    "matchId": 101,
    "status": "LIVE",
    "score": {
      "innings": 2,
      "runs": 142,
      "wickets": 6,
      "overs": "16.3",
      "target": 186,
      "requiredRuns": 44,
      "requiredBalls": 21
    },
    "currentBatters": [
      {
        "playerId": 25,
        "name": "Batter A",
        "runs": 35,
        "balls": 20
      },
      {
        "playerId": 26,
        "name": "Batter B",
        "runs": 10,
        "balls": 8
      }
    ],
    "currentBowler": {
      "playerId": 40,
      "name": "Bowler A",
      "overs": "3.3",
      "wickets": 2,
      "runsConceded": 25
    },
    "version": 245,
    "updatedAt": "2026-11-04T14:35:30Z"
  }
}
```

The `version` field helps the frontend identify whether the score has changed.

## 4.6 Get Match Events API



GET

`/api/v1/matches/{matchId}/events`



### Request

```
GET /api/v1/matches/101/events?limit=20
```

### Response

```
{
  "data": [
    {
      "id": 5001,
      "sequenceNumber": 245,
      "type": "SIX",
      "player": {
        "id": 25,
        "name": "Batter A"
      },
      "teamId": 1,
      "period": "INNINGS_2",
      "gameClock": "16.3",
      "description": "Batter A hits a six",
      "createdAt": "2026-11-04T14:35:30Z"
    },
    {
      "id": 5000,
      "sequenceNumber": 244,
      "type": "FOUR",
      "player": {
        "id": 26,
        "name": "Batter B"
      },
      "teamId": 1,
      "period": "INNINGS_2",
      "gameClock": "16.2",
      "description": "Batter B hits a four",
      "createdAt": "2026-11-04T14:35:15Z"
    }
  ]
}
```

The events are returned in descending order of sequence number so that the newest event appears first.

## 4.7 Get Match Statistics API



GET

`/api/v1/matches/{matchId}/stats`



### Request

```
GET /api/v1/matches/101/stats
```

### Response

```
{
  "data": {
    "matchId": 101,
    "teamStats": [
      {
        "teamId": 1,
        "possession": 58,
        "shots": 10,
        "shotsOnTarget": 5
      },
      {
        "teamId": 2,
        "possession": 42,
        "shots": 6,
        "shotsOnTarget": 3
      }
    ]
  }
}
```

The response structure may vary depending on the sport.

## 4.8 Get Cricket Scorecard API



GET

`/api/v1/matches/{matchId}/scorecard`



### Request

```
GET /api/v1/matches/101/scorecard
```

### Response

```
{
  "data": {
    "matchId": 101,
    "innings": [
      {
        "inningsNumber": 1,
        "teamId": 1,
        "totalRuns": 185,
        "wickets": 4,
        "overs": "20.0",
        "batting": [
          {
            "playerId": 25,
            "name": "Batter A",
            "runs": 75,
            "balls": 42,
            "fours": 8,
            "sixes": 3,
            "strikeRate": 178.57
          }
        ],
        "bowling": [
          {
            "playerId": 40,
            "name": "Bowler A",
            "overs": "4.0",
            "runs": 35,
            "wickets": 2,
            "economy": 8.75
          }
        ]
      }
    ]
  }
}
```

## 4.9 Get Tournament Standings API



GET

`/api/v1/tournaments/{tournamentId}/standings`



### Request

```
GET /api/v1/tournaments/10/standings?seasonId=2026
```

### Response

```
{
  "data": [
    {
      "rank": 1,
      "teamId": 1,
      "teamName": "Team A",
      "played": 10,
      "won": 8,
      "lost": 2,
      "points": 16
    },
    {
      "rank": 2,
      "teamId": 2,
      "teamName": "Team B",
      "played": 10,
      "won": 7,
      "lost": 3,
      "points": 14
    }
  ]
}
```

Standings should be computed according to the tournament's rules, not a universal formula. Cricket, Football, and Basketball may use different ranking criteria.

# 5. How to Handle Live Score Updates

This is one of the most important parts of the system design interview.

Suppose a Cricket match is happening. A batter hits a six.

The system needs to:

1. Receive the event from the sports data provider.
2. Validate the event.
3. Update the match score.
4. Store the match event.
5. Notify all connected clients.
6. Ensure the frontend displays the updated score.

## 5.1 REST API vs WebSocket

REST APIs are suitable for fetching match data, but repeatedly polling them for every score update can be inefficient.

REST Polling

Client requests score every 5 seconds.

Simple to implement

More requests and potentially stale scores

WebSocket

Server pushes updates as soon as they arrive.

Low latency

Requires connection management

### Recommended approach

Use REST for initial data fetching and WebSocket for real-time updates.

The frontend flow:

```
1. GET /api/v1/matches/101
2. GET /api/v1/matches/101/score
3. Connect to WebSocket
4. Subscribe to match 101
5. Receive score updates
6. Update the UI
```

### WebSocket design

Connection:

```
wss://api.sports.com/ws
```

Subscribe message:

```
{
  "action": "subscribe",
  "matchId": 101
}
```

Server sends:

```
{
  "type": "SCORE_UPDATE",
  "matchId": 101,
  "version": 246,
  "score": {
    "runs": 148,
    "wickets": 6,
    "overs": "17.0"
  },
  "event": {
    "type": "SIX",
    "description": "Batter A hits a six"
  },
  "updatedAt": "2026-11-04T14:36:00Z"
}
```

The frontend can compare the incoming version with the currently displayed version.

If the incoming version is greater, it updates the score.

For example:

```
if (incomingUpdate.version > currentScore.version) {
  setCurrentScore(incomingUpdate);
}
```

This prevents older updates from overwriting newer ones.

# 6. API Best Practices and Edge Cases

## 6.1 HTTP Status Codes

| Status Code               | Usage                                   |
| ------------------------- | --------------------------------------- |
| 200 OK                    | Successful GET or update                |
| 201 Created               | Resource successfully created           |
| 204 No Content            | Successful deletion                     |
| 400 Bad Request           | Invalid request parameters              |
| 401 Unauthorized          | Authentication required                 |
| 403 Forbidden             | User lacks permission                   |
| 404 Not Found             | Match or resource not found             |
| 409 Conflict              | Version conflict or duplicate operation |
| 422 Unprocessable Entity  | Request validation failed               |
| 429 Too Many Requests     | Rate limit exceeded                     |
| 500 Internal Server Error | Unexpected server error                 |
| 503 Service Unavailable   | Service temporarily unavailable         |

## 6.2 Consistent Error Response

All errors should follow a common format.

```
{
  "error": {
    "code": "MATCH_NOT_FOUND",
    "message": "Match with ID 101 does not exist",
    "details": null,
    "requestId": "req-abc123"
  }
}
```

## 6.3 Pagination

For APIs returning large datasets, use pagination.

Example:

```
GET /api/v1/matches?page=1&limit=20
```

For high-volume event streams or large match histories, cursor-based pagination is better.

Example:

```
GET /api/v1/matches/101/events?limit=20&cursor=eyJzZXEiOjI0NX0
```

The cursor represents the last event sequence that was returned.

Cursor pagination avoids some of the problems with offset pagination when new events are continuously added.

## 6.4 Filtering and Sorting

Example:

```
GET /api/v1/matches?sport=CRICKET&status=LIVE
```

Example:

```
GET /api/v1/matches?date=2026-11-04&tournamentId=10
```

Example:

```
GET /api/v1/matches?status=COMPLETED&sort=-scheduledStart
```

Here, `-scheduledStart` means descending order.

## 6.5 Caching

Use Redis for frequently accessed live match scores.

Example cache key:

```
match:101:score
```

Example value:

```
{
  "runs": 148,
  "wickets": 6,
  "overs": "17.0",
  "version": 246
}
```

Recommended strategy:

- Cache match details for longer durations.
- Cache live scores for short durations or update them on each event.
- Cache tournament standings.
- Invalidate or update cache after score changes.
- Avoid caching outdated scores for long periods.

For live score endpoints, HTTP cache headers could be:

```
Cache-Control: public, max-age=1, stale-while-revalidate=2
ETag: "match-101-score-v246"
```

The server should use appropriate cache controls depending on whether the data is public and how fresh it must be.

# 7. Data Ingestion APIs

In a real sports score system, the score updates will typically come from a third-party sports data provider or an authorized scoring application.

These APIs should be separate from public APIs.

| Method | Endpoint                                    | Purpose                  |
| ------ | ------------------------------------------- | ------------------------ |
| POST   | `/api/v1/provider/matches/{matchId}/events` | Ingest a new match event |
| PATCH  | `/api/v1/provider/matches/{matchId}/score`  | Update match score       |
| PATCH  | `/api/v1/provider/matches/{matchId}/status` | Update match status      |

These endpoints require authentication and authorization.

### Example event ingestion

```
POST /api/v1/provider/matches/101/events
Authorization: Bearer <provider-token>
Idempotency-Key: provider-event-789
Content-Type: application/json
```

Request body:

```
{
  "providerEventId": "provider-event-789",
  "sequenceNumber": 246,
  "eventType": "SIX",
  "playerId": 25,
  "teamId": 1,
  "gameClock": "17.0",
  "details": {
    "runs": 6
  }
}
```

The backend should:

- Validate the event.
- Check whether it has already been processed.
- Store the event.
- Update the match score.
- Increment the score version.
- Publish the update to connected clients.

The processing should be transactional where possible.

A duplicate provider event must not add the score twice.

# 8. Important System Design Considerations

These are the areas an interviewer may ask you to explain further.

| Problem                               | Solution                                              |
| ------------------------------------- | ----------------------------------------------------- |
| Millions of users reading live scores | Redis cache and horizontally scalable API servers     |
| High number of WebSocket connections  | Dedicated WebSocket gateway                           |
| Duplicate score events                | Idempotency keys and unique provider event IDs        |
| Events arriving out of order          | Sequence numbers and event reconciliation             |
| Database write load                   | Event batching, partitioning, and optimized indexes   |
| Lost WebSocket updates                | Reconnection and replay using event sequence numbers  |
| Cache inconsistency                   | Cache update after successful database commit         |
| Provider failure                      | Retry with backoff and fallback provider if available |
| Match history growing large           | Partition event tables by time or match               |
| Incorrect score                       | Audit trail and correction events                     |

## Recommended Database Indexes

```
CREATE INDEX idx_matches_status_start
ON matches(status, scheduled_start);

CREATE INDEX idx_matches_season
ON matches(season_id);

CREATE INDEX idx_match_events_match_sequence
ON match_events(match_id, sequence_number DESC);

CREATE INDEX idx_match_events_match_time
ON match_events(match_id, event_time DESC);

CREATE INDEX idx_match_teams_team
ON match_teams(team_id, match_id);

CREATE INDEX idx_player_stats_match
ON player_match_stats(match_id);
```

These indexes help optimize common queries such as:

- Fetch all live matches.
- Fetch matches in a tournament.
- Fetch the latest events for a match.
- Fetch matches for a particular team.
- Fetch statistics for a match.

# 9. Final Interview Summary

Recommended Design

Database

- PostgreSQL for structured data and transactional updates.
- Redis for caching live scores.
- Match Events table for historical event tracking.
- Sport-specific tables for detailed scoring.

  API
- REST APIs for fetching matches, scores, teams, players, and statistics.
- WebSocket for real-time updates.
- Versioned endpoints under `/api/v1`.
- Cursor pagination for match events.

  Scalability
- Horizontally scalable API servers.
- Redis caching.
- Event-driven architecture.
- Dedicated WebSocket gateways.
- Partition large event tables as needed.

The most important design decision: Store match events as historical records and maintain a separate latest-score representation for fast reads. This provides both a reliable history and fast access to the current score.

For a system design interview, I would next explain the event-driven architecture using Kafka, Redis, PostgreSQL, and WebSocket gateways, including how to handle millions of concurrent users.

















Live Sports Score System: SQL vs Document DB vs Columnar DB
System Design Deep Dive
For a Live Sports Score System, I would recommend a combination of databases rather than relying on a single database for everything.

My primary recommendation is:
- PostgreSQL (SQL) — Primary database for core entities and transactional data.
- Redis (Key-Value Store) — For caching live scores and serving low-latency reads.
- MongoDB (Document DB) — Optional, for flexible sport-specific data and rich commentary payloads.
- ClickHouse (Columnar DB) — For analytical queries, historical statistics, and large-scale reporting.
However, I would not introduce all four databases from day one.

The key is understanding what kind of data we have, how it is accessed, and what guarantees are required.
1. Why Do We Need Different Databases?
Let's consider the data in our sports system.

Core application data

Matches, teams, players, tournaments, seasons, schedules
PostgreSQL




Frequently accessed live data

Current scores, live match summaries, user subscriptions
Redis




Flexible and variable data

Sport-specific payloads, commentary metadata, provider responses
MongoDB (optional)




Historical analytics

Player trends, team performance, tournament analysis, millions of events
ClickHouse



Each database is optimized for a different workload.

The mistake would be to select a database based only on the fact that the system has a lot of data.

Instead, we should select it based on how we access that data.

2. SQL Database — PostgreSQL
Recommended as the primary database
Why SQL is the best choice for core sports data
Our core data is highly relational.

For example:
- A tournament belongs to a sport.
- A season belongs to a tournament.
- A match belongs to a season.
- A match has participating teams.
- A player belongs to a team during a particular period.
- A match event references a match, player, and team.
These relationships are naturally represented using relational tables.
Example relationship
#chatgpt-mermaid-_r_j8_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_j8_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_j8_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_j8_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_j8_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_j8_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_j8_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_j8_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_j8_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_j8_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_j8_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_j8_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_j8_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_j8_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-_r_j8_ p{margin:0;}#chatgpt-mermaid-_r_j8_ .entityBox{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_j8_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-_r_j8_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-_r_j8_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_j8_ .edgeLabel{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-_r_j8_ .edgeLabel .label rect{fill:rgb(0, 0, 0);}#chatgpt-mermaid-_r_j8_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_j8_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-_r_j8_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_j8_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-_r_j8_ .node rect,#chatgpt-mermaid-_r_j8_ .node circle,#chatgpt-mermaid-_r_j8_ .node ellipse,#chatgpt-mermaid-_r_j8_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_j8_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-_r_j8_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-_r_j8_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-_r_j8_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_j8_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_j8_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_j8_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_j8_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_j8_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_j8_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_j8_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_j8_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_j8_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_j8_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_j8_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_j8_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_j8_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_j8_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_j8_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_j8_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_j8_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}SPORTSTOURNAMENTSSEASONSMATCHESMATCH_TEAMSTEAMSMATCH_EVENTSPLAYERShascontainsschedulesincludesparticipatesgeneratesinvolved_in





This structure is easy to maintain using SQL foreign keys and constraints.
Why PostgreSQL specifically?
A. Strong data consistency
Imagine a football goal event.

When a goal is scored, we need to:
1. Store the goal event.
2. Update the match score.
3. Increment the score version.
These changes should not partially succeed.

We can use a transaction:
BEGIN;

INSERT INTO match_events (
    match_id,
    event_type,
    sequence_number,
    details
)
VALUES (
    101,
    'GOAL',
    246,
    '{"scorer_id": 25}'
);

UPDATE match_scores
SET home_score = home_score + 1,
    version = version + 1,
    updated_at = NOW()
WHERE match_id = 101;

COMMIT;


If an operation fails, the transaction can be rolled back.

This example assumes the scorer belongs to the home team and that the event is new. Production processing must validate the team and deduplicate the provider event before incrementing the score.

This is important because an incorrect score is much worse than a slightly delayed score.
B. Relationships and constraints
Consider this requirement:
A match should never reference a team that does not exist.

With PostgreSQL:
team_id BIGINT REFERENCES teams(id)


The database itself enforces this rule.

In a document database, this kind of cross-document relationship is usually enforced by application code instead.
C. Complex queries
Suppose we want:
Get all live matches for a particular tournament, along with team names and current scores.

SQL can do this naturally:
SELECT
    m.id AS match_id,
    t1.name AS home_team,
    t2.name AS away_team,
    s.home_score,
    s.away_score,
    m.status
FROM matches m
JOIN match_scores s
    ON s.match_id = m.id
JOIN match_teams mt1
    ON mt1.match_id = m.id
   AND mt1.side = 'HOME'
JOIN teams t1
    ON t1.id = mt1.team_id
JOIN match_teams mt2
    ON mt2.match_id = m.id
   AND mt2.side = 'AWAY'
JOIN teams t2
    ON t2.id = mt2.team_id
WHERE m.status = 'LIVE'
  AND m.season_id = 2026;


This is a common operation in a sports application.
D. PostgreSQL supports JSONB
A common misconception is:
If we use SQL, every field must be fixed and structured.

That is not true.

PostgreSQL supports JSONB, which lets us store flexible data while retaining relational features.

For example:
CREATE TABLE match_scores (
    match_id BIGINT PRIMARY KEY REFERENCES matches(id),

    score_data JSONB NOT NULL,

    version BIGINT NOT NULL DEFAULT 1,

    updated_at TIMESTAMPTZ DEFAULT NOW()
);


For Football:
{
  "home": {
    "goals": 2,
    "shots": 10,
    "possession": 58
  },
  "away": {
    "goals": 1,
    "shots": 6,
    "possession": 42
  }
}


For Cricket:
{
  "innings": 2,
  "battingTeamId": 2,
  "runs": 185,
  "wickets": 4,
  "overs": "18.4"
}


This gives us flexibility without immediately adding another database.
When PostgreSQL is sufficient
For an MVP and even a substantial production system, PostgreSQL may be enough for:
- Matches.
- Teams.
- Players.
- Tournaments.
- Match events.
- Scorecards.
- Match statistics.
- Match schedules.
We should scale PostgreSQL first with proper indexing, read replicas, partitioning, and query optimization before introducing more storage systems.

3. Document Database — MongoDB
Optional: Add when flexible data becomes a real need
What is a Document Database?
A document database stores data as documents, usually in JSON-like format.

Instead of having many relational tables, a document can contain nested objects and arrays.

For example, a match document might look like:

{
  "_id": "match_101",
  "sport": "CRICKET",
  "tournament": {
    "id": "tournament_10",
    "name": "IPL"
  },
  "teams": [
    {
      "id": "team_1",
      "name": "Team A"
    },
    {
      "id": "team_2",
      "name": "Team B"
    }
  ],
  "currentScore": {
    "innings": 2,
    "runs": 185,
    "wickets": 4,
    "overs": "18.4"
  },
  "recentEvents": [
    {
      "type": "SIX",
      "player": "Batter A",
      "over": "18.3"
    },
    {
      "type": "FOUR",
      "player": "Batter B",
      "over": "18.4"
    }
  ]
}



This structure is convenient when the data is naturally nested.
Where MongoDB can help in our sports system
A. Sport-specific data varies significantly
Consider different sports.

Football:
{
  "score": {
    "homeGoals": 2,
    "awayGoals": 1
  },
  "statistics": {
    "possession": [58, 42],
    "shots": [10, 6],
    "corners": [5, 3]
  }
}


Cricket:
{
  "score": {
    "runs": 185,
    "wickets": 4,
    "overs": "18.4"
  },
  "statistics": {
    "runRate": 9.91,
    "requiredRunRate": 8.2
  }
}


Basketball:
{
  "score": {
    "homePoints": 102,
    "awayPoints": 98
  },
  "statistics": {
    "quarter": 4,
    "timeRemaining": "02:15"
  }
}


The structure is different for every sport.

MongoDB makes it easy to store these different structures without requiring every sport to have the same set of columns.
B. Rich commentary payloads
Sports commentary may contain varying fields.

For example:
- Text commentary.
- Player mentions.
- Video references.
- Images.
- Event metadata.
- Provider-specific attributes.
A document might be:
{
  "matchId": 101,
  "eventType": "WICKET",
  "commentary": "Batter A is caught at long-on!",
  "media": {
    "videoUrl": "https://example.com/highlight.mp4"
  },
  "metadata": {
    "wicketType": "CAUGHT",
    "fielderId": 42,
    "bowlerId": 40
  }
}


This is a natural document-oriented use case.
C. Provider data ingestion
Suppose multiple data providers send different payloads.

Provider A sends:
{
  "event": "GOAL",
  "player": 25,
  "minute": 78
}


Provider B sends:
{
  "type": "SCORE_CHANGE",
  "scorer": {
    "id": 25
  },
  "time": {
    "minute": 78,
    "second": 32
  }
}


MongoDB can store raw provider payloads without forcing them into a fixed schema.

This can be useful for:
- Debugging provider issues.
- Auditing.
- Reprocessing historical data.
- Developing new integrations.
But should MongoDB store our main match data?
Not my first choice
There are some important disadvantages.
Problem	Explanation
Relationships	Teams, players, matches, and tournaments are highly relational
Duplication	Team names and tournament information may be repeated in many documents
Data consistency	Maintaining references across documents can be more difficult
Transactions	Multi-document transactions exist, but relational modeling is still more natural in SQL
Score updates	Atomic document updates are useful, but complex cross-document updates need more care

My recommendation for MongoDB
Use MongoDB for:
- Raw provider payloads.
- Flexible commentary documents.
- Rich sport-specific representations.
- Data that changes shape frequently.
Do not use it simply because we have JSON data.

PostgreSQL JSONB can already handle a large amount of flexible data.

MongoDB should be introduced only when the flexible document workload becomes sufficiently important to justify another database.

4. Columnar Database — ClickHouse
Recommended when analytics volume grows
What is a Columnar Database?
Traditional relational databases such as PostgreSQL generally store data row-wise.

Columnar databases store data column-wise.

Let's consider this table:
Match ID	Team	Runs	Wickets	Overs
101	India	185	4	20
102	Australia	210	6	20
103	England	165	8	20
104	India	190	5	20

Row-oriented storage









A row-oriented database stores the values of a record together.

This is great when we need a particular match record or when we need to update a score.
Column-oriented storage









A columnar database stores values from the same column together.

This is great when we need to scan millions of records and aggregate only a few columns.
Why ClickHouse is useful for sports analytics
Imagine that we want to answer:
- What is the average run rate across 10 million Cricket overs?
- Which player has the highest strike rate in the last five seasons?
- What is the average possession of a team across all matches?
- How many goals were scored in the last 10 years?
- Which teams perform best in the final 15 minutes?
- What are the most common wicket types?
These queries scan a lot of historical data.

ClickHouse is optimized for this kind of analytical workload.
Example analytical table
CREATE TABLE cricket_ball_analytics
(
    match_id UInt64,
    innings_number UInt8,
    batting_team_id UInt64,
    batter_id UInt64,
    bowler_id UInt64,

    over_number UInt8,
    ball_number UInt8,

    runs UInt8,
    wickets UInt8,

    event_time DateTime
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(event_time)
ORDER BY (
    batting_team_id,
    event_time,
    match_id
);


This table is designed for analytics, not for the primary transactional workflow.
Example: Average runs per team
SELECT
    batting_team_id,
    avg(runs) AS average_runs_per_ball,
    sum(runs) AS total_runs
FROM cricket_ball_analytics
GROUP BY batting_team_id
ORDER BY total_runs DESC;


Example: Most successful batters
SELECT
    batter_id,
    sum(runs) AS total_runs,
    count() AS balls_faced,
    sum(runs) * 100.0 / count() AS strike_rate
FROM cricket_ball_analytics
GROUP BY batter_id
HAVING count() >= 100
ORDER BY strike_rate DESC
LIMIT 10;


The example assumes the table has one row per ball and counts each row as a ball faced. In a production analytics model, wides and other non-batter balls must be handled according to Cricket scoring rules.
Why not use ClickHouse as the primary database?
Because its strength is analytical reads, not general transactional application workflows.
Requirement	PostgreSQL	ClickHouse
Update a match score	Excellent	Not ideal
Transactional multi-table updates	Excellent	Not designed for OLTP transactions
Foreign key constraints	Excellent	Limited/not typical
Fetch a match by ID	Excellent	Possible, but not its primary strength
Aggregate billions of events	Good, depending on workload	Excellent
Historical trends and analytics	Good	Excellent
Real-time analytics	Good	Excellent with proper ingestion

ClickHouse can receive data in near real time, but I would not make it the authoritative source for match scores.

PostgreSQL is the source of truth; ClickHouse is the analytical store.

5. Redis — Key-Value Database for Live Scores
Highly recommended
Redis is not a SQL, document, or columnar database. It is an in-memory key-value store.

It is particularly useful for low-latency reads and caching.
Why Redis is important in a live sports system
Suppose:
- A Cricket match is live.
- 2 million users are viewing the match.
- The score changes every few seconds.
- Every user wants the latest score.
If every request goes directly to PostgreSQL, the database may become overloaded.

Instead, we can store the current score in Redis.
Redis data structure
A simple Redis Hash:
Key: match:101:score


Fields:
runs       = 185
wickets    = 4
overs      = 18.4
status     = LIVE
version    = 245


Example Redis commands:
HSET match:101:score \
    runs 185 \
    wickets 4 \
    overs 18.4 \
    status LIVE \
    version 245


Fetch the score:
HGETALL match:101:score


The score is available without querying PostgreSQL for every read.
What should Redis store?
Data	Redis usage
Current live scores	Yes
Live match summary	Yes
Match status	Yes
Popular tournament matches	Yes
User's followed live matches	Potentially
Full historical scorecards	Usually no
Permanent match events	No, use durable storage

Redis should generally be treated as a cache, not as the only permanent storage for scores.

If Redis loses data, the system should be able to reconstruct its cache from PostgreSQL or a durable event stream.
Redis Pub/Sub vs Kafka
Redis can also distribute updates.

However, I would distinguish between:
- Redis as a cache.
- Redis Pub/Sub as a lightweight notification mechanism.
- Kafka as a durable event streaming platform.
For a high-scale system, Kafka is useful because events can be retained and replayed, consumers can process events independently, and ingestion can be decoupled from downstream services.

6. Recommended Combination: PostgreSQL + Redis + Kafka + ClickHouse
For a large production sports score platform, this would be my recommended architecture.
Recommended production architecture
#chatgpt-mermaid-_r_nh_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_nh_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_nh_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_nh_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-_r_nh_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-_r_nh_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_nh_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_nh_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_nh_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_nh_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_nh_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_nh_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_nh_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_nh_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-_r_nh_ p{margin:0;}#chatgpt-mermaid-_r_nh_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_nh_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_nh_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_nh_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-_r_nh_ .label text,#chatgpt-mermaid-_r_nh_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_nh_ .node rect,#chatgpt-mermaid-_r_nh_ .node circle,#chatgpt-mermaid-_r_nh_ .node ellipse,#chatgpt-mermaid-_r_nh_ .node polygon,#chatgpt-mermaid-_r_nh_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-_r_nh_ .rough-node .label text,#chatgpt-mermaid-_r_nh_ .node .label text,#chatgpt-mermaid-_r_nh_ .image-shape .label,#chatgpt-mermaid-_r_nh_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-_r_nh_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-_r_nh_ .rough-node .label,#chatgpt-mermaid-_r_nh_ .node .label,#chatgpt-mermaid-_r_nh_ .image-shape .label,#chatgpt-mermaid-_r_nh_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-_r_nh_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-_r_nh_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_nh_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-_r_nh_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-_r_nh_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-_r_nh_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-_r_nh_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-_r_nh_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-_r_nh_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-_r_nh_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-_r_nh_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_nh_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-_r_nh_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-_r_nh_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-_r_nh_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-_r_nh_ .icon-shape,#chatgpt-mermaid-_r_nh_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-_r_nh_ .icon-shape p,#chatgpt-mermaid-_r_nh_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-_r_nh_ .icon-shape .label rect,#chatgpt-mermaid-_r_nh_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-_r_nh_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-_r_nh_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-_r_nh_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-_r_nh_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_nh_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_nh_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_nh_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_nh_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_nh_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_nh_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_nh_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_nh_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-_r_nh_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_nh_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_nh_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_nh_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_nh_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_nh_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_nh_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_nh_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-_r_nh_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-_r_nh_ .node tspan[font-weight="normal"],#chatgpt-mermaid-_r_nh_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-_r_nh_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-_r_nh_ .node rect,#chatgpt-mermaid-_r_nh_ .node circle,#chatgpt-mermaid-_r_nh_ .node ellipse,#chatgpt-mermaid-_r_nh_ .node polygon,#chatgpt-mermaid-_r_nh_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-_r_nh_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-_r_nh_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-_r_nh_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-_r_nh_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-_r_nh_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Sports Data ProviderIngestion ServiceKafka: Match EventsScore ProcessorPostgreSQLRedis: Live ScoresKafka: Score UpdatesWebSocket GatewayMillions of UsersAnalytics ConsumerClickHouseAnalytics APIREST API





What each component does
Component	Responsibility
PostgreSQL	Authoritative match data, scores, teams, players, events
Redis	Fast cache for current scores
Kafka	Durable event distribution and replay
ClickHouse	Historical analytics and aggregate statistics
REST API	Fetch match data
WebSocket Gateway	Push real-time updates
Ingestion Service	Receive and validate provider events

Step-by-step data flow
Let's take the example of a Football goal.
1. Sports provider sends a goal event
   
   The provider sends an event indicating that a goal has been scored.
2. Ingestion Service publishes to Kafka
   
   Kafka receives the event with a unique provider event ID and match ID.
3. Score Processor consumes the event
   
   The processor validates the event, checks for duplicates, and determines which team scored.
4. PostgreSQL is updated
   
   The match event is persisted, and the current score is updated transactionally.
5. Redis is updated
   
   The latest score is written to the cache.
6. Kafka publishes the score update
   
   A score-update event is distributed to WebSocket consumers.
7. WebSocket Gateway pushes to clients
   
   Users watching the match receive the new score.
8. ClickHouse receives the event
   
   An analytics consumer stores the event for historical analysis.
Why Kafka is important here
Kafka is not a database replacement.

It is a durable event streaming system that sits between services.

It provides:
- Decoupling between ingestion and score processing.
- Event retention and replay.
- Multiple independent consumers.
- Buffering during traffic spikes.
- Scalable event processing.
For example, the same goal event can be consumed by:
- Score processor.
- WebSocket notification service.
- Analytics pipeline.
- Commentary service.
- Notification service.
Without Kafka, the ingestion service might have to directly call each downstream service, making it harder to scale and recover.
Important consistency detail
I would use Kafka as the event distribution backbone, but PostgreSQL remains the authoritative source of truth.

For reliability, I would use the Transactional Outbox Pattern:
1. Process the incoming event.
2. In one PostgreSQL transaction, store the event, update the score, and write an outbox record.
3. A publisher sends the outbox record to Kafka.
4. Consumers update Redis, WebSockets, and ClickHouse.
This avoids the classic problem where PostgreSQL commits successfully but the Kafka publish fails.

Consumers must still be idempotent because Kafka delivery and downstream processing can involve retries.

7. Should We Use All These Databases From Day One?
No. This is an important system design decision.

Using multiple databases creates operational complexity:
- More infrastructure to manage.
- More monitoring and alerting.
- More failure scenarios.
- Data synchronization problems.
- More difficult debugging.
- Increased infrastructure cost.
I would introduce databases incrementally.
Phase 1 — MVP
PostgreSQL + Redis
Start here.
- PostgreSQL stores everything.
- Redis caches live scores.
- REST APIs serve the data.
- WebSockets push score updates.
- Use PostgreSQL JSONB for sport-specific fields.
  
  Phase 2 — Growing traffic
  Add Kafka
- Decouple provider ingestion from score processing.
- Distribute updates to multiple services.
- Handle event replay and traffic spikes.
- Use the transactional outbox pattern.
  
  Phase 3 — Analytics at scale
  Add ClickHouse
- Store historical match events.
- Run large analytical queries.
- Generate player and team performance reports.
- Build advanced statistics and dashboards.
  
  Phase 4 — Specialized flexibility
  Add MongoDB if justified
- Store raw provider payloads.
- Store rich, variable commentary documents.
- Handle data that is difficult to model efficiently in PostgreSQL.

8. Final Database Decision Matrix
Database	Use in our system	Priority
PostgreSQL	Core entities, match scores, durable events, transactions	Essential
Redis	Live score cache, low-latency reads	Recommended
Kafka	Event distribution, buffering, replay	Scale phase
ClickHouse	Historical analytics and large aggregations	Analytics phase
MongoDB	Flexible documents, raw provider data	Optional

9. Interview Answer: What Would I Say?
If the interviewer asks:

"Would you use SQL, NoSQL, or a combination of databases for the Live Sports Score System?"

I would answer:
I would use a polyglot persistence approach, where each database is selected based on its workload.

I would choose PostgreSQL as the primary database because the core entities, such as matches, teams, players, tournaments, and match events, are highly relational. PostgreSQL provides transactions, strong consistency, foreign key constraints, and efficient joins.

I would use Redis to cache frequently accessed live scores because millions of users may request the same score. This reduces the load on PostgreSQL and improves read latency.

For a high-scale architecture, I would introduce Kafka to distribute match events reliably to score processing, WebSocket gateways, and analytics consumers.

I would use ClickHouse for historical analytics, such as player performance, team statistics, and aggregations over millions of match events.

I would not introduce MongoDB initially because PostgreSQL JSONB already provides flexibility for sport-specific data. I would add MongoDB only if we have a clear requirement for storing large volumes of flexible documents, such as raw provider payloads or rich commentary.

So my initial architecture would be PostgreSQL and Redis, and my scalable architecture would be PostgreSQL, Redis, Kafka, and ClickHouse.

The key principle: Do not choose multiple databases because the system is large. Choose them because different workloads have fundamentally different requirements.

The next important design topic is how to guarantee that score updates are processed exactly once logically, even when Kafka retries events, providers send duplicates, or updates arrive out of order.

