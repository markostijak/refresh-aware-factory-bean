package com.github.mscode.beans.factory.refreshaware.tools.clock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Mutable Clock class to simulate time passing in unit test.
 */
public class MutableClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public MutableClock(Instant instant, ZoneId zone) {
        this.zone = zone;
        this.instant = instant;
    }

    public MutableClock(Instant instant) {
        this(instant, ZoneOffset.UTC);
    }

    public MutableClock(int epochSecond) {
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

        return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    /**
     * Move clock forward for {@code seconds} seconds
     *
     * @param seconds amount of seconds
     */
    public void tickSeconds(int seconds) {
        this.instant = instant.plusSeconds(seconds);
    }

}
