package cm.mboalabs.flowpulse.ai;

import cm.mboalabs.flowpulse.audit.AuditService;
import cm.mboalabs.flowpulse.auth.AuthenticatedUser;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Passerelle vers le service IA : le frontend ne parle qu'au backend, qui contrôle les rôles
 * et journalise les actions critiques (lancement de surveillance, décision sur une recommandation).
 */
@RestController
@RequestMapping("/api")
public class AiGatewayController {

    public record AskRequest(@NotBlank @Size(max = 1000) String question) {}

    public record DecisionRequest(@NotBlank @Pattern(regexp = "appliquer|rejeter") String decision, @Size(max = 500) String motif) {}

    private static final String ID = "[A-Za-z0-9-]{1,40}";

    private final AiClient ai;
    private final AuditService audit;

    public AiGatewayController(AiClient ai, AuditService audit) {
        this.ai = ai;
        this.audit = audit;
    }

    @GetMapping("/zones")
    public JsonNode zones() {
        return ai.get("/ai/zones");
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE','OPERATEUR','ADMIN')")
    public JsonNode alerts() {
        return ai.get("/ai/watcher/last");
    }

    @PostMapping("/alerts/run")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE','ADMIN')")
    public JsonNode runWatcher(@AuthenticationPrincipal AuthenticatedUser user) {
        JsonNode result = ai.post("/ai/watcher/run", null);
        audit.log(user.id(), "SURVEILLANCE_LANCEE", "trace:" + result.path("trace_id").asText(), null);
        return result;
    }

    @GetMapping("/recommendations")
    public JsonNode recommendations() {
        return ai.get("/ai/recommendations");
    }

    @GetMapping("/recommendations/{id:" + ID + "}")
    public JsonNode recommendation(@PathVariable String id) {
        return ai.get("/ai/recommendations/" + id);
    }

    @PostMapping("/recommendations/{id:" + ID + "}/decision")
    @PreAuthorize("hasAnyRole('OPERATEUR','ADMIN')")
    public JsonNode decide(@PathVariable String id, @Valid @RequestBody DecisionRequest body,
                           @AuthenticationPrincipal AuthenticatedUser user) {
        JsonNode result = ai.post("/ai/recommendations/" + id + "/decision", body);
        audit.log(user.id(), body.decision().equals("appliquer") ? "RECOMMANDATION_APPLIQUEE" : "RECOMMANDATION_REJETEE",
                "recommandation:" + id, body.motif());
        return result;
    }

    @PostMapping("/assistant")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE','OPERATEUR','URBANISTE','ADMIN')")
    public JsonNode ask(@Valid @RequestBody AskRequest body, @AuthenticationPrincipal AuthenticatedUser user) {
        JsonNode result = ai.post("/ai/assistant", Map.of("question", body.question()));
        audit.log(user.id(), "QUESTION_SUPERVISEUR", "trace:" + result.path("trace_id").asText(), body.question());
        return result;
    }

    @GetMapping("/traces/{id:" + ID + "}")
    public JsonNode trace(@PathVariable String id) {
        return ai.get("/ai/traces/" + id);
    }

    @GetMapping("/gaps")
    public JsonNode gaps() {
        return ai.get("/ai/gaps");
    }
}
