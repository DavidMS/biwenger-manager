-- The La Liga clubs, as the latest catalog had them (#30): the market's team filter and any page
-- that shows a player's club join player.team_id on it. No foreign key from player, for the same
-- reason snapshots have none: a club relegated at the end of the season leaves the catalog.
CREATE TABLE team (
    id          BIGINT      PRIMARY KEY,
    name        TEXT        NOT NULL,
    slug        TEXT        NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
