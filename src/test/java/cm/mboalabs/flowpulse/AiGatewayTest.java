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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
        return token(email, "demo-test-pass");
    }

    private String token(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
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

    @Test
    void lAdministrateurAAccesATout() throws Exception {
        when(ai.post(eq("/ai/watcher/run"), any())).thenReturn(json.readTree("{\"trace_id\":\"T-2\",\"alertes\":[]}"));
        when(ai.post(eq("/ai/recommendations/R-2/decision"), any())).thenReturn(json.readTree("{\"id\":\"R-2\",\"statut\":\"APPLIQUEE\"}"));
        when(ai.post(eq("/ai/assistant"), any())).thenReturn(json.readTree("{\"trace_id\":\"T-3\",\"reponse\":\"ok\"}"));
        String admin = token("admin@test.local", "admin-test-pass");

        mvc.perform(post("/api/alerts/run").header("Authorization", admin)).andExpect(status().isOk());
        mvc.perform(post("/api/recommendations/R-2/decision").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"appliquer\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/assistant").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Etat du trafic ?\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void lUrbanisteSimuleEtLOperateurNonEtLActionEstJournalisee() throws Exception {
        when(ai.post(eq("/ai/simulations"), any())).thenReturn(json.readTree("{\"id\":\"S-1\",\"libelle\":\"Fermeture\"}"));
        String scenario = "{\"type\":\"fermeture\",\"zone\":\"Grand Dakar\",\"debut\":\"08:00\",\"fin\":\"12:00\"}";

        mvc.perform(post("/api/scenarios").header("Authorization", token("operateur@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content(scenario))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/scenarios").header("Authorization", token("urbaniste@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content(scenario.replace("08:00", "8h")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/scenarios").header("Authorization", token("urbaniste@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content(scenario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("S-1"));
        assertThat(auditLogs.findAll()).anyMatch(l -> l.getAction().equals("SIMULATION_LANCEE"));

        // Lieu réel : transmis au service IA ; un identifiant mal formé est refusé
        String surLieu = scenario.replace("}", ",\"lieu\":\"route-boulevard-du-20-mai\"}");
        mvc.perform(post("/api/scenarios").header("Authorization", token("urbaniste@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content(surLieu))
                .andExpect(status().isOk());
        verify(ai).post(eq("/ai/simulations"), argThat(m -> "route-boulevard-du-20-mai".equals(((java.util.Map<?, ?>) m).get("lieu"))));
        mvc.perform(post("/api/scenarios").header("Authorization", token("urbaniste@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content(surLieu.replace("route-boulevard", "Route Boulevard")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alerteAcquitteeEtTransmiseAvecLAuteurEtJournalisee() throws Exception {
        when(ai.post(eq("/ai/alerts/A-1/action"), any())).thenReturn(json.readTree("{\"id\":\"A-1\",\"zone\":\"Grand Dakar\",\"statut\":\"TRANSMISE\"}"));
        mvc.perform(post("/api/alerts/A-1/ack").header("Authorization", token("operateur@flowpulse.local")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/alerts/A-1/transmit").header("Authorization", token("operateur@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"destinataire\":\"Police\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/alerts/A-1/transmit").header("Authorization", token("gestionnaire@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"destinataire\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/alerts/A-1/transmit").header("Authorization", token("gestionnaire@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"destinataire\":\"Police municipale\"}"))
                .andExpect(status().isOk());
        verify(ai).post(eq("/ai/alerts/A-1/action"), eq(Map.of("action", "acquitter", "par", "operateur@flowpulse.local")));
        assertThat(auditLogs.findAll()).anyMatch(l -> l.getAction().equals("ALERTE_TRANSMISE") && l.getDetails().contains("Police municipale"));
    }

    @Test
    void leJournalDAuditEstReserveALAdministrateurEtExportable() throws Exception {
        mvc.perform(get("/api/admin/audit").header("Authorization", token("gestionnaire@flowpulse.local")))
                .andExpect(status().isForbidden());
        String admin = token("admin@test.local", "admin-test-pass");
        mvc.perform(get("/api/admin/audit").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entrees").isArray())
                .andExpect(jsonPath("$.total").isNumber());
        mvc.perform(get("/api/admin/audit/export").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("date;utilisateur;action")));
    }

    @Test
    void seulLAdministrateurRelanceLIndexation() throws Exception {
        when(ai.post(eq("/ai/kb/reindex"), any())).thenReturn(json.readTree("{\"etat\":\"en_cours\"}"));
        mvc.perform(post("/api/kb/reindex").header("Authorization", token("urbaniste@flowpulse.local")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/kb/reindex").header("Authorization", token("admin@test.local", "admin-test-pass")))
                .andExpect(status().isOk());
    }

    @Test
    void lHorlogeSeRegleParLeGestionnaireEtEstJournalisee() throws Exception {
        when(ai.post(eq("/ai/horloge"), any())).thenReturn(json.readTree("{\"source\":\"d4d\",\"t\":\"2013-10-15T12:00\"}"));
        mvc.perform(post("/api/horloge").header("Authorization", token("operateur@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"t\":\"2013-10-15T12:00\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/horloge").header("Authorization", token("gestionnaire@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"t\":\"demain\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/horloge").header("Authorization", token("gestionnaire@flowpulse.local"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"t\":\"2013-10-15T12:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.t").value("2013-10-15T12:00"));
        assertThat(auditLogs.findAll()).anyMatch(l -> l.getAction().equals("HORLOGE_REGLEE"));
    }

    @Test
    void laSerieDUneZoneEstRelayeeAvecSonNom() throws Exception {
        when(ai.get(eq("/ai/zones/serie?zone={zone}&heures={heures}"), eq("Grand Dakar"), eq(12)))
                .thenReturn(json.readTree("[{\"t\":\"2013-11-12T18:00\",\"observe\":91}]"));
        mvc.perform(get("/api/zones/serie").param("zone", "Grand Dakar").param("heures", "12")
                        .header("Authorization", token("gestionnaire@flowpulse.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].observe").value(91));
    }
}
