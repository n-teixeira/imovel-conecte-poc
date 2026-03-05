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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeadWebhookConsumer {

    private final ObjectMapper objectMapper;
    private final LeadService leadService;
    private final IdempotencyService idempotencyService;
    private final WebhookFailureService webhookFailureService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.LEADS_QUEUE)
    public void processWebhook(@Payload String messageBody, Message message) {
        String eventId = null;
        try {
            JsonNode root = objectMapper.readTree(messageBody);
            eventId = root.path("eventId").asText();
            String rawPayload = root.path("rawPayload").asText();

            if (idempotencyService.isAlreadyProcessed(eventId)) {
                log.debug("Evento já processado (idempotência): {}", eventId);
                return;
            }

            var payload = objectMapper.readValue(rawPayload, HubSpotWebhookPayload.class);
            var lead = leadService.createOrUpdateFromWebhook(payload);

            String responseHash = IdempotencyService.hashContent(
                    lead.getId() != null ? lead.getId().toString() : "created");
            idempotencyService.markProcessed(eventId, responseHash);

            webhookFailureService.markResolved(eventId);
            log.info("Lead processado com sucesso: eventId={}, leadId={}", eventId, lead.getId());

        } catch (Exception e) {
            log.error("Erro ao processar webhook: eventId={}, error={}", eventId, e.getMessage());
            if (eventId != null) {
                webhookFailureService.recordFailure(eventId, messageBody, e.getMessage());
                sendToContingency(messageBody, message);
            }
            throw new RuntimeException(e);
        }
    }

    private void sendToContingency(String messageBody, Message originalMessage) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.LEADS_EXCHANGE,
                    "lead.contingency",
                    messageBody
            );
            log.info("Mensagem enviada para fila de contingência");
        } catch (Exception e) {
            log.error("Falha ao enviar para contingência: {}", e.getMessage());
        }
    }
}
