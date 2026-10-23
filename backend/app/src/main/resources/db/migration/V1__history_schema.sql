-- History that Biwenger doesn't keep (ADR 0005). Money is whole euros in BIGINT. A snapshot's day
-- is the calendar day in Europe/Madrid; its natural key makes a second sync on the same day an
-- update, not a duplicate. Snapshots have no foreign key to player: a player who leaves La Liga
-- leaves the catalog, but his history stays.

-- Every player we have seen, as the latest catalog had him.
CREATE TABLE player (
    id          BIGINT      PRIMARY KEY,
    name        TEXT        NOT NULL,
    slug        TEXT        NOT NULL,
    team_id     BIGINT      NOT NULL,
    position    TEXT        NOT NULL CHECK (position IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD', 'COACH')),
    status      TEXT        NOT NULL,
    price       BIGINT      NOT NULL CHECK (price >= 0),
    updated_at  TIMESTAMPTZ NOT NULL
);

-- A player's value on one day. The daily sync fills every column; the backfill from the price
-- history (3.4) only knows the price, so status and points are then NULL.
CREATE TABLE player_price_snapshot (
    player_id     BIGINT NOT NULL,
    snapshot_date DATE   NOT NULL,
    price         BIGINT NOT NULL CHECK (price >= 0),
    price_change  BIGINT,
    status        TEXT,
    points        INTEGER,
    source        TEXT   NOT NULL CHECK (source IN ('DAILY_SYNC', 'BACKFILL')),
    PRIMARY KEY (player_id, snapshot_date)
);

-- A player's report for one club match, played or not. Points are in one scoring system, the
-- league's at the time, so the system is stored with them.
CREATE TABLE player_points_report (
    player_id      BIGINT      NOT NULL,
    match_id       BIGINT      NOT NULL,
    round_id       BIGINT      NOT NULL,
    match_date     TIMESTAMPTZ NOT NULL,
    home           BOOLEAN     NOT NULL,
    played         BOOLEAN     NOT NULL,
    score_id       INTEGER     NOT NULL,
    points         INTEGER,
    minutes_played INTEGER     NOT NULL CHECK (minutes_played >= 0),
    absence        TEXT,
    PRIMARY KEY (player_id, match_id),
    -- Points only when he played; a reason only when he didn't.
    CHECK (played = (points IS NOT NULL)),
    CHECK (NOT played OR absence IS NULL)
);

-- Who sold whom in the market on one day. A free player has no seller and no ownership data.
CREATE TABLE market_snapshot (
    snapshot_date       DATE        NOT NULL,
    player_id           BIGINT      NOT NULL,
    seller_manager_id   BIGINT,
    seller_name         TEXT,
    price               BIGINT      NOT NULL CHECK (price >= 0),
    listed_at           TIMESTAMPTZ NOT NULL,
    closes_at           TIMESTAMPTZ NOT NULL,
    clause              BIGINT,
    purchase_price      BIGINT,
    acquired_at         TIMESTAMPTZ,
    clause_unlocks_at   TIMESTAMPTZ,
    PRIMARY KEY (snapshot_date, player_id),
    CHECK ((seller_manager_id IS NULL) = (seller_name IS NULL)),
    CHECK ((seller_manager_id IS NULL) = (clause IS NULL)),
    CHECK (closes_at >= listed_at)
);

-- The league table on one day.
CREATE TABLE league_standing_snapshot (
    snapshot_date      DATE    NOT NULL,
    manager_id         BIGINT  NOT NULL,
    league_id          BIGINT  NOT NULL,
    manager_name       TEXT    NOT NULL,
    position           INTEGER NOT NULL CHECK (position >= 1),
    points             INTEGER NOT NULL,
    team_value         BIGINT  NOT NULL,
    team_value_change  BIGINT  NOT NULL,
    PRIMARY KEY (snapshot_date, manager_id)
);

-- Per-player reads use the primary keys. These serve "every player on one day" and "every report
-- of one round".
CREATE INDEX player_price_snapshot_by_date ON player_price_snapshot (snapshot_date);
CREATE INDEX player_points_report_by_round ON player_points_report (round_id);
