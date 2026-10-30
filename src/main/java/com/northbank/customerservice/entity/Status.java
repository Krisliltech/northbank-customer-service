package com.northbank.customerservice.entity;

public enum Status {
    ACTIVE,
    DEACTIVATED,
    SUSPENDED,
    CLOSED;

    public boolean canTransitionTo(Status target) {
        if (target == null) {
            return false;
        }

        return switch (this) {
            case ACTIVE ->
                    target == DEACTIVATED
                            || target == SUSPENDED
                            || target == CLOSED;

            case DEACTIVATED, SUSPENDED ->
                    target == ACTIVE
                            || target == CLOSED;

            case CLOSED -> false;
        };
    }
}
