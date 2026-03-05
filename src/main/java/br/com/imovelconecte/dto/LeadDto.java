package br.com.imovelconecte.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadDto {

    private Long id;
    private String hubspotContactId;
    private String hubspotDealId;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    private String fullName;
    private String phoneNumber;
    private String propertyInterested;
    private String source;
    private String enrichmentData;
    private boolean syncedToHubspot;
}
