package br.com.imovelconecte.service;

import br.com.imovelconecte.domain.entity.Lead;
import br.com.imovelconecte.dto.HubSpotWebhookPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LeadEnrichmentServiceTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LeadEnrichmentService service;

    @Test
    @DisplayName("Deve extrair propriedades do payload com propertyValues")
    void extractProperties_fromPropertyValues() {
        var payload = new HubSpotWebhookPayload();
        payload.setPropertyValues(List.of(
                new HubSpotWebhookPayload.PropertyChange("email", "test@email.com"),
                new HubSpotWebhookPayload.PropertyChange("firstname", "Maria")
        ));

        Map<String, String> props = service.extractPropertiesFromPayload(payload);

        assertThat(props).containsEntry("email", "test@email.com");
        assertThat(props).containsEntry("firstname", "Maria");
    }

    @Test
    @DisplayName("Deve extrair propriedades do payload com propertyName/Value")
    void extractProperties_fromPropertyNameValue() {
        var payload = new HubSpotWebhookPayload();
        payload.setPropertyName("email");
        payload.setPropertyValue("direct@test.com");

        Map<String, String> props = service.extractPropertiesFromPayload(payload);

        assertThat(props).containsEntry("email", "direct@test.com");
    }

    @Test
    @DisplayName("Deve enriquecer lead com metadata")
    void enrichLead_setsEnrichmentData() {
        var lead = Lead.builder()
                .id(1L)
                .email("lead@test.com")
                .hubspotContactId("999")
                .build();

        service.enrichLead(lead, "12345");

        assertThat(lead.getEnrichmentData()).isNotNull();
        assertThat(lead.getEnrichmentData()).contains("12345");
        assertThat(lead.getEnrichmentData()).contains("999");
    }
}
