package com.northbank.customerservice.outbox;

import java.time.Duration;

final class DurationFormat {

    private DurationFormat() {}

    static String humanReadableFormat(Duration d) {
        Duration safe = d.isNegative() ? Duration.ZERO : d;
        if (safe.toSeconds() < 1) {
            return safe.toMillis() + " ms";
        }
        long days = safe.toDays();
        String time = "%dh %dm %ds".formatted(safe.toHoursPart(), safe.toMinutesPart(), safe.toSecondsPart());
        return days > 0 ? days + "d " + time : time;
    }
}
