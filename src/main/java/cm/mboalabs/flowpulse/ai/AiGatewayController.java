package cm.mboalabs.flowpulse.ai;

import cm.mboalabs.flowpulse.audit.AuditService;
import cm.mboalabs.flowpulse.auth.AuthenticatedUser;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Passerelle vers le service IA : le frontend ne parle qu'au backend, qui contrôle les rôles
 * et journalise les actions critiques (surveillance, décision sur une recommandation, simulation).
 */
@RestController
@RequestMapping("/api")
public class AiGatewayController {

    public record AskRequest(@NotBlank @Size(max = 1000) String question) {}

    public record DecisionRequest(@NotBlank @Pattern(regexp = "appliquer|rejeter") String decision, @Size(max = 500) String motif) {}

    public record ScenarioRequest(
            @NotBlank @Pattern(regexp = "fermeture|evenement|renfort") String type,
            @NotBlank @Size(max = 80) String zone,
            @Pattern(regexp = HEURE) String debut,
            @Pattern(regexp = HEURE) String fin,
            @Min(0) @Max(200_000) Integer participants,
            @Min(0) @Max(50) Integer nbBus) {}

    private static final String HEURE = "([01][0-9]|2[0-3]):[0-5][0-9]";

    private static final String ID ="[A-Za-z0-9-]{1,40}";

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

    @PostMapping("/scenarios")
    @PreAuthorize("hasAnyRole('URBANISTE','ADMIN')")
    public JsonNode simulate(@Valid @RequestBody ScenarioRequest body, @AuthenticationPrincipal AuthenticatedUser user) {
        Map<String, Object> scenario = new HashMap<>();
        scenario.put("type", body.type());
        scenario.put("zone", body.zone());
        if (body.debut() != null) scenario.put("debut", body.debut());
        if (body.fin() != null) scenario.put("fin", body.fin());
        scenario.put("participants", body.participants() == null ? 0 : body.participants());
        scenario.put("nb_bus", body.nbBus() == null ? 0 : body.nbBus());
        JsonNode result = ai.post("/ai/simulations", scenario);
        audit.log(user.id(), "SIMULATION_LANCEE", "simulation:" + result.path("id").asText(), result.path("libelle").asText());
        return result;
    }

    @GetMapping("/scenarios")
    @PreAuthorize("hasAnyRole('URBANISTE','ADMIN')")
    public JsonNode simulations() {
        return ai.get("/ai/simulations");
    }

    @GetMapping("/scenarios/{id:" + ID + "}")
    @PreAuthorize("hasAnyRole('URBANISTE','ADMIN')")
    public JsonNode simulation(@PathVariable String id) {
        return ai.get("/ai/simulations/" + id);
    }

    @GetMapping("/gaps")
    public JsonNode gaps() {
        return ai.get("/ai/gaps");
    }
}
