package br.com.imovelconecte.service;

import br.com.imovelconecte.domain.entity.IdempotencyKey;
import br.com.imovelconecte.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    @Value("${imovelconecte.idempotency.ttl-minutes:1440}")
    private int ttlMinutes;

    public boolean isAlreadyProcessed(String eventId) {
        return repository.findById(eventId).map(k -> !k.isExpired()).orElse(false);
    }

    public void markProcessed(String eventId, String responseHash) {
        Instant now = Instant.now();
        var key = IdempotencyKey.builder()
                .eventId(eventId)
                .responseHash(responseHash)
                .processedAt(now)
                .expiresAt(now.plusSeconds(ttlMinutes * 60L))
                .build();
        repository.save(key);
        log.debug("Evento marcado como processado: {}", eventId);
    }

    public String getResponseHash(String eventId) {
        return repository.findById(eventId)
                .map(IdempotencyKey::getResponseHash)
                .orElse(null);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredKeys() {
        try {
            int deleted = repository.deleteExpiredKeys(Instant.now());
            if (deleted > 0) {
                log.info("Chaves de idempotência expiradas removidas: {}", deleted);
            }
        } catch (Exception e) {
            log.warn("Erro ao limpar chaves expiradas: {}", e.getMessage());
        }
    }

    public static String hashContent(String content) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return content;
        }
    }
}
