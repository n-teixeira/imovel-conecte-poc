package br.com.imovelconecte.service;

import br.com.imovelconecte.domain.entity.WebhookFailure;
import br.com.imovelconecte.repository.WebhookFailureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookFailureServiceTest {

    @Mock
    private WebhookFailureRepository repository;

    @InjectMocks
    private WebhookFailureService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxRetries", 5);
        ReflectionTestUtils.setField(service, "retryDelayMs", 60000L);
    }

    @Test
    @DisplayName("Deve registrar falha de webhook")
    void recordFailure_savesWebhookFailure() {
        when(repository.findByEventId("evt-1")).thenReturn(Optional.empty());
        when(repository.save(any(WebhookFailure.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.recordFailure("evt-1", "{\"pay\":\"load\"}", "Connection timeout");

        assertThat(result.getEventId()).isEqualTo("evt-1");
        assertThat(result.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve incrementar retry em falha existente")
    void recordFailure_incrementsRetryForExisting() {
        var existing = WebhookFailure.builder()
                .eventId("evt-2")
                .retryCount(2)
                .maxRetries(5)
                .resolved(false)
                .build();
        when(repository.findByEventId("evt-2")).thenReturn(Optional.of(existing));
        when(repository.save(any(WebhookFailure.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.recordFailure("evt-2", "{}", "Error");

        assertThat(result.getRetryCount()).isEqualTo(3);
    }
}
