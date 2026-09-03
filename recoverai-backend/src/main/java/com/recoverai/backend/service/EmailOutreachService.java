package com.recoverai.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends recovery emails only when explicitly enabled through configuration.
 */
@Service
@ConditionalOnProperty(
        name = "recoverai.outreach.email.enabled",
        havingValue = "true"
)
public class EmailOutreachService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailOutreachService(
            JavaMailSender mailSender,
            @Value("${recoverai.outreach.email.from-address}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendRecoveryMessage(
            String recipient,
            Long attemptId,
            String reason
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(
                "Action needed: complete your RecoverAI payment recovery"
        );

        message.setText(
                "Your payment recovery attempt " + attemptId
                        + " needs attention. "
                        + reason
                        + "\n\n"
                        + "Open RecoverAI to choose a retry time, "
                        + "promise to pay, or another payment method."
        );

        mailSender.send(message);
    }
}