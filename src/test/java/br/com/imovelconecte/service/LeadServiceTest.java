package br.com.imovelconecte.service;

import br.com.imovelconecte.domain.entity.Lead;
import br.com.imovelconecte.dto.HubSpotWebhookPayload;
import br.com.imovelconecte.dto.LeadDto;
import br.com.imovelconecte.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadEnrichmentService enrichmentService;

    @Mock
    private HubSpotApiService hubSpotApiService;

    @InjectMocks
    private LeadService leadService;

    private HubSpotWebhookPayload webhookPayload;

    @BeforeEach
    void setUp() {
        webhookPayload = new HubSpotWebhookPayload();
        webhookPayload.setObjectType("contact");
        webhookPayload.setObjectId(12345L);
        webhookPayload.setPortalId(12345678L);
        webhookPayload.setPropertyValues(List.of(
                new HubSpotWebhookPayload.PropertyChange("email", "lead@test.com"),
                new HubSpotWebhookPayload.PropertyChange("firstname", "João"),
                new HubSpotWebhookPayload.PropertyChange("lastname", "Silva"),
                new HubSpotWebhookPayload.PropertyChange("phone", "+5511999999999")
        ));
    }

    @Test
    @DisplayName("Deve criar novo lead quando não existir")
    void createOrUpdateFromWebhook_whenLeadNotExists_createsNew() {
        when(enrichmentService.extractPropertiesFromPayload(any())).thenReturn(Map.of(
                "email", "lead@test.com", "firstname", "João", "lastname", "Silva", "phone", "+5511999999999"));
        when(leadRepository.findByHubspotContactId("12345")).thenReturn(Optional.empty());
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
            Lead l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        var result = leadService.createOrUpdateFromWebhook(webhookPayload);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("lead@test.com");
        assertThat(result.getFullName()).isEqualTo("João Silva");
        assertThat(result.getPhoneNumber()).isEqualTo("+5511999999999");
        assertThat(result.getHubspotContactId()).isEqualTo("12345");
    }

    @Test
    @DisplayName("Deve atualizar lead existente")
    void createOrUpdateFromWebhook_whenLeadExists_updates() {
        when(enrichmentService.extractPropertiesFromPayload(any())).thenReturn(Map.of(
                "email", "lead@test.com", "firstname", "João", "lastname", "Silva", "phone", "+5511999999999"));
        var existing = Lead.builder()
                .id(1L)
                .hubspotContactId("12345")
                .email("old@test.com")
                .fullName("Old Name")
                .build();
        when(leadRepository.findByHubspotContactId("12345")).thenReturn(Optional.of(existing));
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = leadService.createOrUpdateFromWebhook(webhookPayload);

        assertThat(result.getEmail()).isEqualTo("lead@test.com");
        assertThat(result.getFullName()).isEqualTo("João Silva");
    }

    @Test
    @DisplayName("Deve listar todos os leads")
    void listAll_returnsAllLeads() {
        var leads = List.of(
                Lead.builder().id(1L).email("a@test.com").build(),
                Lead.builder().id(2L).email("b@test.com").build()
        );
        when(leadRepository.findAll()).thenReturn(leads);

        var result = leadService.listAll();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Deve criar lead manual e sincronizar com HubSpot")
    void createLead_syncsToHubSpot() {
        var dto = LeadDto.builder()
                .email("new@test.com")
                .fullName("Novo Lead")
                .phoneNumber("11999998888")
                .build();
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
            Lead l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        var result = leadService.createLead(dto, "default");

        assertThat(result.getEmail()).isEqualTo("new@test.com");
        verify(hubSpotApiService).syncLeadToHubSpot(any(Lead.class), eq("default"));
    }
}
