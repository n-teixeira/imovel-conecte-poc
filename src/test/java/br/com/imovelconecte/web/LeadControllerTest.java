package br.com.imovelconecte.web;

import br.com.imovelconecte.dto.LeadDto;
import br.com.imovelconecte.service.HubSpotApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(br.com.imovelconecte.TestContainersConfig.class)
class LeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HubSpotApiService hubSpotApiService;

    @Test
    @DisplayName("Deve listar leads vazios")
    void listLeads_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Deve criar lead com validação")
    void createLead_validatesAndCreates() throws Exception {
        var dto = LeadDto.builder()
                .email("novo@lead.com")
                .fullName("Lead Novo")
                .phoneNumber("11999998888")
                .build();

        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("novo@lead.com"))
                .andExpect(jsonPath("$.fullName").value("Lead Novo"));
    }

    @Test
    @DisplayName("Deve rejeitar lead sem email")
    void createLead_withoutEmail_returnsBadRequest() throws Exception {
        var dto = LeadDto.builder()
                .fullName("Sem Email")
                .build();

        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
