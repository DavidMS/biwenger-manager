package io.github.davidms.biwenger.client.model;

/** A manager's place in the standings, team value and current lineup. */
public record Standing(
    long id,
    String name,
    int points,
    int position,
    long teamValue,
    long teamValueInc,
    Lineup lineup) {}
