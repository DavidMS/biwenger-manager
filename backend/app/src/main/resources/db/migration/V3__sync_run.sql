-- Every sync run (#25): when it ran, who started it, and how it ended. The latest row is the
-- status GET /api/sync/status shows.
CREATE TABLE sync_run (
    id               BIGSERIAL   PRIMARY KEY,
    sync_trigger     TEXT        NOT NULL CHECK (sync_trigger IN ('SCHEDULED', 'MANUAL')),
    started_at       TIMESTAMPTZ NOT NULL,
    finished_at      TIMESTAMPTZ,
    result           TEXT        NOT NULL CHECK (result IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    snapshot_date    DATE,
    players          INTEGER,
    market_listings  INTEGER,
    standings        INTEGER,
    error            TEXT,
    -- A finished run has an end; a successful one has its counts; a failed one says why.
    CHECK ((result = 'RUNNING') = (finished_at IS NULL)),
    CHECK (result <> 'SUCCEEDED' OR num_nulls(snapshot_date, players, market_listings, standings) = 0),
    CHECK ((result = 'FAILED') = (error IS NOT NULL))
);

CREATE INDEX sync_run_by_start ON sync_run (started_at DESC);
