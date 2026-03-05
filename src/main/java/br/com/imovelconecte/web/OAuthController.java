package br.com.imovelconecte.web;

import br.com.imovelconecte.service.HubSpotOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final HubSpotOAuthService oauthService;

    @GetMapping("/authorize")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        return ResponseEntity.ok(Map.of(
                "authorization_url", oauthService.buildAuthorizationUrl()
        ));
    }

    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> handleCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "portal_id", required = false) String portalId) {
        oauthService.exchangeCodeForTokens(code, portalId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Token HubSpot configurado com sucesso"
        ));
    }
}
