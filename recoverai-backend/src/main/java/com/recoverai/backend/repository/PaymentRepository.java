

package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByStatus(String status);

    long countByStatus(String status);

    List<Payment> findByFailureCategoryIsNullAndFailureReasonIsNotNull();

    Optional<Payment> findByPaymentId(String paymentId);
}