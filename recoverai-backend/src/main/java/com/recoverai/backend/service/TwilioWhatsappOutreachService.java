package com.recoverai.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Twilio's WhatsApp Messages API adapter. */
@Service
@ConditionalOnProperty(name = "recoverai.outreach.whatsapp.enabled", havingValue = "true")
public class TwilioWhatsappOutreachService {

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String contentSid;

    public TwilioWhatsappOutreachService(
            RestClient.Builder restClientBuilder,
            @Value("${recoverai.outreach.whatsapp.account-sid}") String accountSid,
            @Value("${recoverai.outreach.whatsapp.auth-token}") String authToken,
            @Value("${recoverai.outreach.whatsapp.from-number}") String fromNumber,
            @Value("${recoverai.outreach.whatsapp.content-sid}") String contentSid
    ) {
        this.restClient = restClientBuilder.baseUrl("https://api.twilio.com").build();
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.contentSid = contentSid;
    }

    public void sendRecoveryMessage(String recipient, Long attemptId, String reason) {

        String form = "From=" + encode("whatsapp:" + fromNumber)
                + "&To=" + encode("whatsapp:" + recipient)
                + "&ContentSid=" + encode(contentSid);

        restClient.post()
                .uri("/2010-04-01/Accounts/{accountSid}/Messages.json", accountSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", basicAuthorization())
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    private String basicAuthorization() {
        String credentials = accountSid + ":" + authToken;

        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}