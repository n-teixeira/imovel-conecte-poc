package br.com.imovelconecte.repository;

import br.com.imovelconecte.domain.entity.HubSpotToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HubSpotTokenRepository extends JpaRepository<HubSpotToken, String> {
}
