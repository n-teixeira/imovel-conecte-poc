package br.com.imovelconecte.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "hubspot")
public class HubSpotConfig {

    private OAuth oauth = new OAuth();
    private Api api = new Api();

    public OAuth getOauth() {
        return oauth;
    }

    public void setOauth(OAuth oauth) {
        this.oauth = oauth;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public static class OAuth {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String tokenUrl = "https://api.hubapi.com/oauth/v1/token";
        private String authorizeUrl = "https://app.hubspot.com/oauth/authorize";
        private String scopes = "crm.objects.contacts.read,crm.objects.contacts.write";

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
        public String getTokenUrl() { return tokenUrl; }
        public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }
        public String getAuthorizeUrl() { return authorizeUrl; }
        public void setAuthorizeUrl(String authorizeUrl) { this.authorizeUrl = authorizeUrl; }
        public String getScopes() { return scopes; }
        public void setScopes(String scopes) { this.scopes = scopes; }
    }

    public static class Api {
        private String baseUrl = "https://api.hubapi.com";
        private Webhook webhook = new Webhook();

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public Webhook getWebhook() { return webhook; }
        public void setWebhook(Webhook webhook) { this.webhook = webhook; }
    }

    public static class Webhook {
        private String verificationToken;
        private String secret;

        public String getVerificationToken() { return verificationToken; }
        public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }
}
