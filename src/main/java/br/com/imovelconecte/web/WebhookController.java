package br.com.imovelconecte.web;

import br.com.imovelconecte.config.HubSpotConfig;
import br.com.imovelconecte.config.RabbitMQConfig;
import br.com.imovelconecte.dto.HubSpotWebhookPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/webhooks/hubspot")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final HubSpotConfig hubSpotConfig;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(@RequestParam("hub.challenge") String challenge) {
        return ResponseEntity.ok(challenge);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveWebhook(@RequestBody String rawPayload) {
        try {
            java.util.List<HubSpotWebhookPayload> eventList;
            var tree = objectMapper.readTree(rawPayload);
            if (tree.isArray()) {
                eventList = objectMapper.convertValue(tree,
                        objectMapper.getTypeFactory().constructCollectionType(
                                java.util.List.class, HubSpotWebhookPayload.class));
            } else {
                // Fallback: payload único (compatibilidade)
                eventList = java.util.List.of(objectMapper.treeToValue(tree, HubSpotWebhookPayload.class));
            }

            if (eventList == null || eventList.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "accepted", "processed", 0));
            }

            int enqueued = 0;
            for (var payload : eventList) {
                String eventId = generateEventId(payload);
                var message = Map.of(
                        "eventId", eventId,
                        "rawPayload", objectMapper.writeValueAsString(payload)
                );
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.LEADS_EXCHANGE,
                        RabbitMQConfig.LEADS_ROUTING_KEY,
                        objectMapper.writeValueAsString(message)
                );
                enqueued++;
                log.debug("Webhook enfileirado: eventId={}", eventId);
            }

            log.info("Webhook processado: {} evento(s) enfileirado(s)", enqueued);
            return ResponseEntity.ok(Map.of(
                    "status", "accepted",
                    "processed", enqueued
            ));
        } catch (Exception e) {
            log.error("Erro ao processar webhook: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private String generateEventId(HubSpotWebhookPayload payload) {
        String composite = String.format("%s-%s-%s-%s",
                payload.getObjectType() != null ? payload.getObjectType() : "unknown",
                payload.getObjectId() != null ? payload.getObjectId() : "0",
                payload.getPortalId() != null ? payload.getPortalId() : "0",
                payload.getOccurredAt() != null ? payload.getOccurredAt() : System.currentTimeMillis());
        return UUID.nameUUIDFromBytes(composite.getBytes()).toString();
    }
}
