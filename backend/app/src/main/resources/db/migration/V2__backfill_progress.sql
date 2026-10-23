-- Which players the backfill (#24) has finished, per season. Written in the same transaction as a
-- player's history, so an interrupted backfill resumes after the last player it completed.
CREATE TABLE backfill_progress (
    season       TEXT        NOT NULL,
    player_id    BIGINT      NOT NULL,
    prices       INTEGER     NOT NULL CHECK (prices >= 0),
    matches      INTEGER     NOT NULL CHECK (matches >= 0),
    completed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (season, player_id)
);
