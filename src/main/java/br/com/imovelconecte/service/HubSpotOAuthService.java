package br.com.imovelconecte.service;

import br.com.imovelconecte.config.HubSpotConfig;
import br.com.imovelconecte.domain.entity.HubSpotToken;
import br.com.imovelconecte.repository.HubSpotTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HubSpotOAuthService {

    private final HubSpotConfig config;
    private final HubSpotTokenRepository tokenRepository;
    private final WebClient.Builder webClientBuilder;

    public String buildAuthorizationUrl() {
        var oauth = config.getOauth();
        return String.format("%s?client_id=%s&redirect_uri=%s&scope=%s",
                oauth.getAuthorizeUrl(),
                oauth.getClientId(),
                oauth.getRedirectUri(),
                oauth.getScopes().replace(",", "%20"));
    }

    public void exchangeCodeForTokens(String code, String portalId) {
        var oauth = config.getOauth();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", oauth.getRedirectUri());
        params.add("client_id", oauth.getClientId());
        params.add("client_secret", oauth.getClientSecret());

        var response = webClientBuilder.build()
                .post()
                .uri(oauth.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null) {
            var token = HubSpotToken.builder()
                    .portalId(portalId != null ? portalId : "default")
                    .accessToken((String) response.get("access_token"))
                    .refreshToken((String) response.get("refresh_token"))
                    .expiresAt(Instant.now().plusSeconds(getExpiresIn(response)))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            tokenRepository.save(token);
            log.info("Tokens HubSpot salvos para portal {}", token.getPortalId());
        }
    }

    public String getValidAccessToken(String portalId) {
        var token = tokenRepository.findById(portalId != null ? portalId : "default")
                .orElseThrow(() -> new IllegalStateException("Token HubSpot não encontrado. Execute o fluxo OAuth primeiro."));

        if (token.isExpired()) {
            refreshToken(token);
            token = tokenRepository.findById(token.getPortalId()).orElseThrow();
        }
        return token.getAccessToken();
    }

    private void refreshToken(HubSpotToken token) {
        var oauth = config.getOauth();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", token.getRefreshToken());
        params.add("client_id", oauth.getClientId());
        params.add("client_secret", oauth.getClientSecret());

        var response = webClientBuilder.build()
                .post()
                .uri(oauth.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null) {
            token.setAccessToken((String) response.get("access_token"));
            token.setExpiresAt(Instant.now().plusSeconds(getExpiresIn(response)));
            token.setUpdatedAt(Instant.now());
            if (response.containsKey("refresh_token")) {
                token.setRefreshToken((String) response.get("refresh_token"));
            }
            tokenRepository.save(token);
            log.info("Token HubSpot renovado para portal {}", token.getPortalId());
        }
    }

    @SuppressWarnings("unchecked")
    private long getExpiresIn(Map<String, Object> response) {
        Object exp = response.get("expires_in");
        if (exp instanceof Number) return ((Number) exp).longValue();
        if (exp instanceof String) return Long.parseLong((String) exp);
        return 1800; // 30 min default
    }
}
