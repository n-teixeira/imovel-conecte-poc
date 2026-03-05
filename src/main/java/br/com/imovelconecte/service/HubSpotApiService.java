package br.com.imovelconecte.service;

import br.com.imovelconecte.config.HubSpotConfig;
import br.com.imovelconecte.domain.entity.Lead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HubSpotApiService {

    private final HubSpotConfig config;
    private final HubSpotOAuthService oauthService;
    private final WebClient.Builder webClientBuilder;

    public void syncLeadToHubSpot(Lead lead, String portalId) {
        String token = oauthService.getValidAccessToken(portalId);
        String baseUrl = config.getApi().getBaseUrl();

        Map<String, Object> properties = Map.of(
                "email", lead.getEmail(),
                "firstname", lead.getFullName() != null ? lead.getFullName().split(" ")[0] : "",
                "lastname", lead.getFullName() != null && lead.getFullName().contains(" ")
                        ? lead.getFullName().substring(lead.getFullName().indexOf(" ") + 1) : "",
                "phone", lead.getPhoneNumber() != null ? lead.getPhoneNumber() : ""
        );

        var response = webClientBuilder.build()
                .post()
                .uri(baseUrl + "/crm/v3/objects/contacts")
                .header("Authorization", "Bearer " + token)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("properties", properties))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("id")) {
            lead.setHubspotContactId(String.valueOf(response.get("id")));
            lead.setSyncedToHubspot(true);
            log.info("Lead sincronizado com HubSpot: contactId={}", response.get("id"));
        }
    }

    public Map<String, Object> fetchContactFromHubSpot(String contactId, String portalId) {
        String token = oauthService.getValidAccessToken(portalId);
        String baseUrl = config.getApi().getBaseUrl();

        return webClientBuilder.build()
                .get()
                .uri(baseUrl + "/crm/v3/objects/contacts/" + contactId + "?properties=email,firstname,lastname,phone")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    /**
     * Busca contato no HubSpot e preenche o mapa de propriedades.
     * Usado quando webhook (ex: contact.creation) não traz email/nome.
     */
    public void fetchAndMergeContactProperties(String contactId, String portalId, Map<String, String> props) {
        try {
            var contact = fetchContactFromHubSpot(contactId, portalId);
            if (contact == null) return;
            @SuppressWarnings("unchecked")
            var properties = (Map<String, Object>) contact.get("properties");
            if (properties == null) return;
            for (var key : new String[]{"email", "firstname", "lastname", "phone"}) {
                if (properties.containsKey(key) && properties.get(key) != null) {
                    Object v = properties.get(key);
                    if (v instanceof String) {
                        props.put(key, (String) v);
                    } else if (v instanceof Map<?, ?> m && m.containsKey("value")) {
                        props.put(key, String.valueOf(m.get("value")));
                    } else {
                        props.put(key, String.valueOf(v));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar contato {} do HubSpot: {}", contactId, e.getMessage());
        }
    }
}
