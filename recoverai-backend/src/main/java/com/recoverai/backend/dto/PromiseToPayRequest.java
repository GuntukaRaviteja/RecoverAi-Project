package com.recoverai.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class PromiseToPayRequest {
    @NotNull(message = "A promise-to-pay deadline is required")
    @Future(message = "The promise-to-pay deadline must be in the future")
    private LocalDateTime promiseToPayDeadline;

    public LocalDateTime getPromiseToPayDeadline() { return promiseToPayDeadline; }
    public void setPromiseToPayDeadline(LocalDateTime promiseToPayDeadline) {
        this.promiseToPayDeadline = promiseToPayDeadline;
    }
}
