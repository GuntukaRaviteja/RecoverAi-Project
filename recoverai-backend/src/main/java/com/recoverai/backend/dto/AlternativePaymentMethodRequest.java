package com.recoverai.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AlternativePaymentMethodRequest {


    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Payment method reference is required")
    private String paymentMethodReference;

    public AlternativePaymentMethodRequest() {
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentMethodReference() {
        return paymentMethodReference;
    }

    public void setPaymentMethodReference(
            String paymentMethodReference
    ) {
        this.paymentMethodReference =
                paymentMethodReference;
    }


}
