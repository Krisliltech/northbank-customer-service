package com.northbank.customerservice.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class StatusTest {

    @ParameterizedTest(name = "{0} -> {1} should be {2}")
    @CsvSource({
            "ACTIVE, DEACTIVATED, true",
            "ACTIVE, SUSPENDED, true",
            "ACTIVE, CLOSED, true",
            "ACTIVE, ACTIVE, false",
            "DEACTIVATED, ACTIVE, true",
            "DEACTIVATED, DEACTIVATED, false",
            "DEACTIVATED, SUSPENDED, false",
            "DEACTIVATED, CLOSED, true",
            "SUSPENDED, ACTIVE, true",
            "SUSPENDED, SUSPENDED, false",
            "SUSPENDED, DEACTIVATED, false",
            "SUSPENDED, CLOSED, true",
            "CLOSED, ACTIVE, false",
            "CLOSED, SUSPENDED, false",
            "CLOSED, DEACTIVATED, false",
            "CLOSED, CLOSED, false",
    })
    void canTransitionTo_followsTheTransitionTable(Status from, Status to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @Test
    void transitionTableCoversEveryStatus() {
        assertThat(Status.values()).hasSize(4);
    }
}
