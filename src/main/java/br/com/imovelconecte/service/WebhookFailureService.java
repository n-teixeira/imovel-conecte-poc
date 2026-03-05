package br.com.imovelconecte.service;

import br.com.imovelconecte.domain.entity.WebhookFailure;
import br.com.imovelconecte.repository.WebhookFailureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookFailureService {

    private final WebhookFailureRepository repository;

    @Value("${imovelconecte.contingency.max-retries:5}")
    private int maxRetries;

    @Value("${imovelconecte.contingency.retry-delay-ms:60000}")
    private long retryDelayMs;

    public WebhookFailure recordFailure(String eventId, String payload, String errorMessage) {
        var failure = repository.findByEventId(eventId).orElse(
                WebhookFailure.builder()
                        .eventId(eventId)
                        .payload(payload)
                        .retryCount(0)
                        .maxRetries(maxRetries)
                        .createdAt(Instant.now())
                        .resolved(false)
                        .build()
        );
        // Garante maxRetries em registros antigos criados sem o valor
        if (failure.getMaxRetries() <= 0) {
            failure.setMaxRetries(maxRetries);
        }
        failure.setErrorMessage(errorMessage);
        failure.setRetryCount(failure.getRetryCount() + 1);
        failure.setLastAttemptAt(Instant.now());
        failure.setNextRetryAt(Instant.now().plusMillis(retryDelayMs));
        return repository.save(failure);
    }

    public void markResolved(String eventId) {
        repository.findByEventId(eventId).ifPresent(f -> {
            f.setResolved(true);
            repository.save(f);
            log.info("Falha resolvida: {}", eventId);
        });
    }

    public List<WebhookFailure> getPendingRetries() {
        return repository.findByResolvedFalseAndNextRetryAtBeforeAndRetryCountLessThan(
                Instant.now(), maxRetries);
    }

    public boolean exists(String eventId) {
        return repository.existsByEventId(eventId);
    }
}
