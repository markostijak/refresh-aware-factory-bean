package com.github.beans.factory.refreshaware.tools.clock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Mutable Clock class to simulate time passing in unit test.
 */
public class TestClock extends Clock {

    private ZoneId zone;
    private Instant instant;

    public TestClock(Instant instant, ZoneId zone) {
        this.zone = zone;
        this.instant = instant;
    }

    public TestClock(Instant instant) {
        this(instant, ZoneOffset.UTC);
    }

    public TestClock(int epochSecond) {
        this(Instant.ofEpochSecond(epochSecond));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        if (zone.equals(this.zone)) {
            return this;
        }

        return new TestClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    /**
     * Move clock forward for {@code seconds} seconds
     * @param seconds amount of seconds
     */
    public void tickSeconds(int seconds) {
        this.instant = instant.plusSeconds(seconds);
    }

    /**
     * Move clock forward for {@code minutes} minutes
     * @param minutes amount of minutes
     */
    public void tickMinutes(int minutes) {
        this.instant = instant.plus(minutes, ChronoUnit.MINUTES);
    }

}
