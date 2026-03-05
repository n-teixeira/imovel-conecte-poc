package br.com.imovelconecte.web;

import br.com.imovelconecte.TestContainersConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(TestContainersConfig.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("Deve retornar challenge na verificação do webhook")
    void verifyWebhook_returnsChallenge() throws Exception {
        mockMvc.perform(get("/webhooks/hubspot")
                        .param("hub.challenge", "test-challenge-123"))
                .andExpect(status().isOk())
                .andExpect(content().string("test-challenge-123"));
    }

    @Test
    @DisplayName("Deve aceitar webhook (array) e retornar quantidade processada")
    void receiveWebhook_acceptsArrayAndReturnsProcessed() throws Exception {
        // HubSpot envia array de eventos
        String payload = """
                [
                    {
                        "objectType": "contact",
                        "objectId": 12345,
                        "portalId": 12345678,
                        "occurredAt": 1609459200000,
                        "propertyValues": [
                            {"name": "email", "value": "webhook@test.com"},
                            {"name": "firstname", "value": "Test"}
                        ]
                    }
                ]
                """;

        mockMvc.perform(post("/webhooks/hubspot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.processed").value(1));
    }
}
