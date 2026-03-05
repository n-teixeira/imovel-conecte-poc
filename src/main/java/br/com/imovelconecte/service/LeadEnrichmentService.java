package br.com.imovelconecte.service;

import br.com.imovelconecte.domain.entity.Lead;
import br.com.imovelconecte.dto.HubSpotWebhookPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadEnrichmentService {

    private final ObjectMapper objectMapper;

    public Map<String, String> extractPropertiesFromPayload(HubSpotWebhookPayload payload) {
        Map<String, String> props = new HashMap<>();
        if (payload.getPropertyValues() != null) {
            for (var pv : payload.getPropertyValues()) {
                if (pv != null && pv.getName() != null) {
                    props.put(pv.getName(), pv.getValue() != null ? pv.getValue() : "");
                }
            }
        }
        if (payload.getPropertyName() != null && payload.getPropertyValue() != null) {
            props.put(payload.getPropertyName(), payload.getPropertyValue());
        }
        return props;
    }

    public void enrichLead(Lead lead, String portalId) {
        try {
            var enrichment = new HashMap<String, Object>();
            enrichment.put("enrichedAt", System.currentTimeMillis());
            enrichment.put("source", "webhook");
            enrichment.put("portalId", portalId);
            if (lead.getHubspotContactId() != null) {
                enrichment.put("hubspotContactId", lead.getHubspotContactId());
            }
            lead.setEnrichmentData(objectMapper.writeValueAsString(enrichment));
        } catch (JsonProcessingException e) {
            log.warn("Erro ao enriquecer lead: {}", e.getMessage());
        }
    }
}
