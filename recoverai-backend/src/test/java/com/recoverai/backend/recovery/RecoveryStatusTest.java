package com.recoverai.backend.recovery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryStatusTest {

    @Test
    void onlyPermitsSupportedLifecycleTransitions() {
        assertTrue(RecoveryStatus.WAITING_FOR_CUSTOMER.canTransitionTo(RecoveryStatus.SCHEDULED));
        assertTrue(RecoveryStatus.WAITING_FOR_PAYMENT_METHOD.canTransitionTo(RecoveryStatus.PROCESSING));
        assertTrue(RecoveryStatus.PROCESSING.canTransitionTo(RecoveryStatus.SUCCESS));
        assertFalse(RecoveryStatus.SUCCESS.canTransitionTo(RecoveryStatus.PROCESSING));
        assertFalse(RecoveryStatus.SCHEDULED.canTransitionTo(RecoveryStatus.SUCCESS));
    }
}
