package br.com.imovelconecte.repository;

import br.com.imovelconecte.domain.entity.WebhookFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

public interface WebhookFailureRepository extends JpaRepository<WebhookFailure, Long> {

    List<WebhookFailure> findByResolvedFalseAndNextRetryAtBeforeAndRetryCountLessThan(
            Instant before, int maxRetries);

    boolean existsByEventId(String eventId);

    Optional<WebhookFailure> findByEventId(String eventId);
}
