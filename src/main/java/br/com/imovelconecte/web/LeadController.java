package br.com.imovelconecte.web;

import br.com.imovelconecte.dto.LeadDto;
import br.com.imovelconecte.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public ResponseEntity<List<LeadDto>> listLeads() {
        return ResponseEntity.ok(leadService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadDto> getLead(@PathVariable Long id) {
        return leadService.findById(id)
                .map(lead -> ResponseEntity.ok(LeadDto.builder()
                        .id(lead.getId())
                        .hubspotContactId(lead.getHubspotContactId())
                        .email(lead.getEmail())
                        .fullName(lead.getFullName())
                        .phoneNumber(lead.getPhoneNumber())
                        .propertyInterested(lead.getPropertyInterested())
                        .source(lead.getSource())
                        .syncedToHubspot(lead.isSyncedToHubspot())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<LeadDto> createLead(
            @Valid @RequestBody LeadDto dto,
            @RequestParam(value = "portal_id", defaultValue = "default") String portalId) {
        var lead = leadService.createLead(dto, portalId);
        return ResponseEntity.ok(LeadDto.builder()
                .id(lead.getId())
                .hubspotContactId(lead.getHubspotContactId())
                .email(lead.getEmail())
                .fullName(lead.getFullName())
                .phoneNumber(lead.getPhoneNumber())
                .propertyInterested(lead.getPropertyInterested())
                .source(lead.getSource())
                .syncedToHubspot(lead.isSyncedToHubspot())
                .build());
    }
}
