package br.com.imovelconecte.repository;

import br.com.imovelconecte.domain.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :before")
    int deleteExpiredKeys(Instant before);
}
