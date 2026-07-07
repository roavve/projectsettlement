package com.example.mssqll.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WebhookNotifierService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${slack.webhook.url}")
    private String webhookUrl;

    @Async
    public void sendExceptionNotification(Exception ex) {
        log.info("Sending exception notification to Slack webhook for: {} (by {})", ex.getClass().getName(), getCurrentUsername());
        StringBuilder sb = new StringBuilder();
        sb.append("```");
        sb.append("Exception: ").append(ex.getClass().getName()).append("\n");
        sb.append("Message: ").append(ex.getMessage()).append("\n");
        sb.append("```").append("\n");

        if (ex.getStackTrace().length > 0) {
            StackTraceElement origin = ex.getStackTrace()[0];
            sb.append("*Occurred at:* ").append(origin.toString()).append("\n\n");
        }

        sb.append("Full stack trace:\n```");
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        sb.append("```");
        Map<String, String> payload = new HashMap<>();
        payload.put("text", sb.toString());

        try {
            restTemplate.postForEntity(webhookUrl, payload, String.class);
            log.debug("Successfully sent exception notification to Slack webhook (by {})", getCurrentUsername());
        } catch (Exception e) {
//            log.error("Error sending to webhook: {} (by {})", e.getMessage(), getCurrentUsername(), e);
        }

    }

}
