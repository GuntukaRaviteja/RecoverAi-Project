package com.recoverai.backend.repository;

import com.recoverai.backend.entity.RecoveryAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RecoveryAttemptRepository
        extends JpaRepository<RecoveryAttempt, Long> {


    boolean existsByPaymentIdAndStatus(
            Long paymentId,
            String status
    );

    boolean existsByPaymentIdAndStatusIn(
            Long paymentId,
            List<String> statuses
    );

    long countByPaymentId(Long paymentId);

    long countByStatus(String status);

    List<RecoveryAttempt> findByPaymentIdOrderByAttemptedAtDesc(
            Long paymentId
    );

    Page<RecoveryAttempt> findByPaymentId(
            Long paymentId,
            Pageable pageable
    );

    List<RecoveryAttempt> findAllByOrderByAttemptedAtAsc();

    List<RecoveryAttempt> findByStatusOrderByAttemptedAtAsc(
            String status
    );

    List<RecoveryAttempt> findByStatusAndScheduledRetryAtLessThanEqualOrderByScheduledRetryAtAsc(
            String status,
            LocalDateTime scheduledRetryAt
    );


}
