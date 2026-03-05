package br.com.imovelconecte.repository;

import br.com.imovelconecte.domain.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByHubspotContactId(String hubspotContactId);

    Optional<Lead> findByEmail(String email);

    boolean existsByHubspotContactId(String hubspotContactId);

    boolean existsByEmail(String email);
}
