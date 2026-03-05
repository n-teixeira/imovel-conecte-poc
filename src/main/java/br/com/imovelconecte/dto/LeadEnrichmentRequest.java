package br.com.imovelconecte.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeadEnrichmentRequest {

    private String eventId;
    private String objectType;
    private Long objectId;
    private Long portalId;
    private Map<String, String> properties;

    public static LeadEnrichmentRequest fromWebhook(String eventId, HubSpotWebhookPayload payload) {
        return LeadEnrichmentRequest.builder()
                .eventId(eventId)
                .objectType(payload.getObjectType())
                .objectId(payload.getObjectId())
                .portalId(payload.getPortalId())
                .build();
    }
}
