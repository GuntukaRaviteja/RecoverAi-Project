package com.recoverai.backend.recovery;

import java.util.Locale;
import java.util.Set;

/** Central definition of persisted recovery lifecycle states. */
public enum RecoveryStatus {
    PENDING,
    PROCESSING,
    WAITING_FOR_CUSTOMER,
    SCHEDULED,
    WAITING_FOR_PAYMENT_METHOD,
    SUCCESS,
    FAILED;

    private static final Set<RecoveryStatus> ACTIVE = Set.of(
            PENDING, PROCESSING, WAITING_FOR_CUSTOMER,
            SCHEDULED, WAITING_FOR_PAYMENT_METHOD
    );

    public static RecoveryStatus from(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unknown recovery status: " + value, exception);
        }
    }

    public boolean isActive() {
        return ACTIVE.contains(this);
    }

    public boolean canTransitionTo(RecoveryStatus target) {
        return switch (this) {
            case PENDING, SCHEDULED -> target == PROCESSING;
            case PROCESSING -> target == SUCCESS || target == FAILED || target == WAITING_FOR_CUSTOMER;
            case WAITING_FOR_CUSTOMER -> target == SCHEDULED || target == WAITING_FOR_PAYMENT_METHOD;
            case WAITING_FOR_PAYMENT_METHOD -> target == PROCESSING;
            case SUCCESS, FAILED -> false;
        };
    }
}
