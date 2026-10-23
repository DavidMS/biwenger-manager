-- Finished rounds whose match reports have been refreshed for every player (#78). The daily sync
-- refreshes the reports when it finds a finished round that isn't here yet.
CREATE TABLE points_refresh (
    round_id     BIGINT      PRIMARY KEY,
    refreshed_at TIMESTAMPTZ NOT NULL,
    players      INTEGER     NOT NULL CHECK (players >= 0)
);
