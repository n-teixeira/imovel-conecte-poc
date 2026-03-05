package br.com.imovelconecte.service;

import br.com.imovelconecte.domain.entity.IdempotencyKey;
import br.com.imovelconecte.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository repository;

    @InjectMocks
    private IdempotencyService service;

    @Test
    @DisplayName("Deve retornar false quando evento não foi processado")
    void isAlreadyProcessed_whenNotProcessed_returnsFalse() {
        when(repository.findById("event-123")).thenReturn(java.util.Optional.empty());
        assertThat(service.isAlreadyProcessed("event-123")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar true quando evento já foi processado e não expirou")
    void isAlreadyProcessed_whenProcessedAndNotExpired_returnsTrue() {
        var key = IdempotencyKey.builder()
                .eventId("event-123")
                .processedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(repository.findById("event-123")).thenReturn(java.util.Optional.of(key));
        assertThat(service.isAlreadyProcessed("event-123")).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false quando chave expirou")
    void isAlreadyProcessed_whenExpired_returnsFalse() {
        var key = IdempotencyKey.builder()
                .eventId("event-123")
                .processedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();
        when(repository.findById("event-123")).thenReturn(java.util.Optional.of(key));
        assertThat(service.isAlreadyProcessed("event-123")).isFalse();
    }

    @Test
    @DisplayName("Deve salvar chave ao marcar como processado")
    void markProcessed_savesKey() {
        service.markProcessed("event-456", "hash123");
        verify(repository).save(any(IdempotencyKey.class));
    }

    @Test
    @DisplayName("Deve gerar hash SHA-256 consistente")
    void hashContent_returnsConsistentHash() {
        String content = "test-content";
        String hash1 = IdempotencyService.hashContent(content);
        String hash2 = IdempotencyService.hashContent(content);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
        assertThat(hash1).matches("^[a-f0-9]+$");
    }
}
