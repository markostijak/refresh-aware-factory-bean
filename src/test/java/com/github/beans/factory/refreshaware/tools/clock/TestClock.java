package com.github.beans.factory.refreshaware.tools.clock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class TestClock extends Clock {

    private Instant instant;

    public TestClock(Instant instant) {
        this.instant = instant;
    }

    public TestClock(int epochSeconds) {
        this.instant = Instant.ofEpochSecond(epochSeconds);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return null;
    }

    @Override
    public Instant instant() {
        return instant;
    }

    public void tickSeconds(int seconds) {
        this.instant = instant.plusSeconds(seconds);
    }

    public void tickMinutes(int minutes) {
        this.instant = instant.plus(minutes, ChronoUnit.MINUTES);
    }

}
