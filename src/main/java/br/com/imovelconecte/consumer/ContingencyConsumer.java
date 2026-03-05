package br.com.imovelconecte.consumer;

import br.com.imovelconecte.config.RabbitMQConfig;
import br.com.imovelconecte.dto.HubSpotWebhookPayload;
import br.com.imovelconecte.service.IdempotencyService;
import br.com.imovelconecte.service.LeadService;
import br.com.imovelconecte.service.WebhookFailureService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContingencyConsumer {

    private final ObjectMapper objectMapper;
    private final LeadService leadService;
    private final IdempotencyService idempotencyService;
    private final WebhookFailureService webhookFailureService;

    @RabbitListener(queues = RabbitMQConfig.CONTINGENCY_QUEUE)
    public void retryFromContingency(String messageBody) {
        try {
            JsonNode root = objectMapper.readTree(messageBody);
            String eventId = root.path("eventId").asText();
            String rawPayload = root.path("rawPayload").asText();

            if (idempotencyService.isAlreadyProcessed(eventId)) {
                log.debug("Evento já processado na contingência (idempotência): {}", eventId);
                return;
            }

            var payload = objectMapper.readValue(rawPayload, HubSpotWebhookPayload.class);
            var lead = leadService.createOrUpdateFromWebhook(payload);

            String responseHash = IdempotencyService.hashContent(
                    lead.getId() != null ? lead.getId().toString() : "created");
            idempotencyService.markProcessed(eventId, responseHash);

            webhookFailureService.markResolved(eventId);
            log.info("Lead reprocessado com sucesso da contingência: eventId={}", eventId);

        } catch (Exception e) {
            log.error("Erro ao reprocessar da contingência: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
