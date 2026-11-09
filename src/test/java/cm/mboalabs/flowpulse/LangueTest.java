package cm.mboalabs.flowpulse;

import cm.mboalabs.flowpulse.ai.AiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/** La langue de l'interface (Accept-Language) est transmise au service IA, réduite à en ou fr. */
class LangueTest {

    @AfterEach
    void nettoyer() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void requete(String acceptLanguage) {
        var r = new MockHttpServletRequest();
        if (acceptLanguage != null) r.addHeader("Accept-Language", acceptLanguage);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(r));
    }

    @Test
    void anglais_reconnu() {
        requete("en-GB,en;q=0.9");
        assertThat(AiClient.langueDemandee()).isEqualTo("en");
    }

    @Test
    void francais_par_defaut_pour_une_autre_langue() {
        requete("fr-FR");
        assertThat(AiClient.langueDemandee()).isEqualTo("fr");
    }

    @Test
    void rien_sans_entete_ni_requete() {
        requete(null);
        assertThat(AiClient.langueDemandee()).isNull();
        RequestContextHolder.resetRequestAttributes();
        assertThat(AiClient.langueDemandee()).isNull();
    }
}
