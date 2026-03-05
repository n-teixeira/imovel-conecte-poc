package br.com.imovelconecte.service;

import br.com.imovelconecte.domain.entity.Lead;
import br.com.imovelconecte.dto.HubSpotWebhookPayload;
import br.com.imovelconecte.dto.LeadDto;
import br.com.imovelconecte.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadEnrichmentService enrichmentService;
    private final HubSpotApiService hubSpotApiService;

    @Transactional
    public Lead createOrUpdateFromWebhook(HubSpotWebhookPayload payload) {
        String contactId = payload.getObjectId() != null ? String.valueOf(payload.getObjectId()) : null;
        String portalId = payload.getPortalId() != null ? String.valueOf(payload.getPortalId()) : "default";

        Optional<Lead> existing = contactId != null
                ? leadRepository.findByHubspotContactId(contactId)
                : Optional.empty();

        Map<String, String> props = enrichmentService.extractPropertiesFromPayload(payload);

        Lead lead;
        if (existing.isPresent()) {
            lead = existing.get();
            updateLeadFromProperties(lead, props);
        } else {
            // contact.creation não traz email/name no webhook - busca na API se possível
            if ((props.isEmpty() || !props.containsKey("email")) && contactId != null) {
                hubSpotApiService.fetchAndMergeContactProperties(contactId, portalId, props);
            }
            String fullName = buildFullName(props.getOrDefault("firstname", ""), props.getOrDefault("lastname", ""));
            String email = props.getOrDefault("email", "");
            if (email.isEmpty() && contactId != null) {
                email = "pending-" + contactId + "@hubspot"; // placeholder até enriquecimento
            }
            lead = Lead.builder()
                    .hubspotContactId(contactId)
                    .email(email)
                    .fullName(fullName.isBlank() ? null : fullName.trim())
                    .phoneNumber(props.get("phone"))
                    .propertyInterested(props.get("property_interested"))
                    .source("hubspot_webhook")
                    .syncedToHubspot(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        lead.setUpdatedAt(Instant.now());
        lead = leadRepository.save(lead);

        enrichmentService.enrichLead(lead, portalId);
        return leadRepository.save(lead);
    }

    private String buildFullName(String first, String last) {
        return (first + " " + last).trim();
    }

    public Optional<Lead> findById(Long id) {
        return leadRepository.findById(id);
    }

    public Optional<Lead> findByHubspotContactId(String contactId) {
        return leadRepository.findByHubspotContactId(contactId);
    }

    public List<LeadDto> listAll() {
        return leadRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Lead createLead(LeadDto dto, String portalId) {
        var lead = Lead.builder()
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .phoneNumber(dto.getPhoneNumber())
                .propertyInterested(dto.getPropertyInterested())
                .source(dto.getSource() != null ? dto.getSource() : "manual")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .syncedToHubspot(false)
                .build();
        lead = leadRepository.save(lead);
        hubSpotApiService.syncLeadToHubSpot(lead, portalId);
        return leadRepository.save(lead);
    }

    private void updateLeadFromProperties(Lead lead, Map<String, String> props) {
        if (props.containsKey("email")) lead.setEmail(props.get("email"));
        if (props.containsKey("firstname") || props.containsKey("lastname")) {
            String full = buildFullName(props.getOrDefault("firstname", ""), props.getOrDefault("lastname", ""));
            lead.setFullName(full.isBlank() ? null : full);
        }
        if (props.containsKey("phone")) lead.setPhoneNumber(props.get("phone"));
        if (props.containsKey("property_interested")) lead.setPropertyInterested(props.get("property_interested"));
    }

    private LeadDto toDto(Lead lead) {
        return LeadDto.builder()
                .id(lead.getId())
                .hubspotContactId(lead.getHubspotContactId())
                .email(lead.getEmail())
                .fullName(lead.getFullName())
                .phoneNumber(lead.getPhoneNumber())
                .propertyInterested(lead.getPropertyInterested())
                .source(lead.getSource())
                .enrichmentData(lead.getEnrichmentData())
                .syncedToHubspot(lead.isSyncedToHubspot())
                .build();
    }
}
