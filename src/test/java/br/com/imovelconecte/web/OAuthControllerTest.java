package br.com.imovelconecte.web;

import br.com.imovelconecte.service.HubSpotOAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(br.com.imovelconecte.TestContainersConfig.class)
class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HubSpotOAuthService oauthService;

    @Test
    @DisplayName("Deve retornar URL de autorização")
    void getAuthorizationUrl_returnsUrl() throws Exception {
        when(oauthService.buildAuthorizationUrl()).thenReturn("https://app.hubspot.com/oauth/authorize?client_id=test");

        mockMvc.perform(get("/oauth/authorize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorization_url").value("https://app.hubspot.com/oauth/authorize?client_id=test"));
    }
}
