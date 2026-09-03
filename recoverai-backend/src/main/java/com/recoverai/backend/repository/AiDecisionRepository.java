package com.recoverai.backend.repository;

import com.recoverai.backend.entity.AiDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiDecisionRepository
        extends JpaRepository<AiDecision, Long> {

    List<AiDecision> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    boolean existsByPaymentId(Long paymentId);
}