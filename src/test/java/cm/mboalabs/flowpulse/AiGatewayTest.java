package cm.mboalabs.flowpulse;

import cm.mboalabs.flowpulse.ai.AiClient;
import cm.mboalabs.flowpulse.audit.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiGatewayTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuditLogRepository auditLogs;
    @MockBean AiClient ai;

    private String token(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"demo-test-pass\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("accessToken").asText();
    }

    @Test
    void leGestionnaireLanceLaSurveillanceEtLActionEstJournalisee() throws Exception {
        when(ai.post(eq("/ai/watcher/run"), any())).thenReturn(json.readTree("{\"trace_id\":\"T-1\",\"alertes\":[]}"));
        long before = auditLogs.count();
        mvc.perform(post("/api/alerts/run").header("Authorization", token("gestionnaire@flowpulse.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trace_id").value("T-1"));
        assertThat(auditLogs.count()).isGreaterThan(before);
    }

    @Test
    void lUrbanisteNePeutPasLancerLaSurveillance() throws Exception {
        mvc.perform(post("/api/alerts/run").header("Authorization", token("urbaniste@flowpulse.local")))
                .andExpect(status().isForbidden());
    }

    @Test
    void seulLOperateurDecideDUneRecommandation() throws Exception {
        when(ai.post(eq("/ai/recommendations/R-1/decision"), any())).thenReturn(json.readTree("{\"id\":\"R-1\",\"statut\":\"APPLIQUEE\"}"));
        String body = "{\"decision\":\"appliquer\"}";

        mvc.perform(post("/api/recommendations/R-1/decision").header("Authorization", token("gestionnaire@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/recommendations/R-1/decision").header("Authorization", token("operateur@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("APPLIQUEE"));
        verify(ai).post(eq("/ai/recommendations/R-1/decision"), any());
    }

    @Test
    void decisionInvalideRefusee() throws Exception {
        mvc.perform(post("/api/recommendations/R-1/decision").header("Authorization", token("operateur@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"supprimer\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lesZonesSontRelayees() throws Exception {
        when(ai.get("/ai/zones")).thenReturn(json.readTree("[{\"zone\":\"Grand Dakar\",\"niveau\":\"SATURE\"}]"));
        mvc.perform(get("/api/zones").header("Authorization", token("urbaniste@flowpulse.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zone").value("Grand Dakar"));
    }
}
