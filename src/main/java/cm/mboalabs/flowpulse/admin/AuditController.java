package cm.mboalabs.flowpulse.admin;

import cm.mboalabs.flowpulse.audit.AuditLog;
import cm.mboalabs.flowpulse.audit.AuditLogRepository;
import cm.mboalabs.flowpulse.user.User;
import cm.mboalabs.flowpulse.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Consultation et export du journal d'audit (F0.2) : qui a fait quoi, quand, sur quel objet. */
@RestController
@RequestMapping("/api/admin/audit")
public class AuditController {

    public record AuditView(UUID id, Instant date, String utilisateur, String action, String objet, String details) {}

    public record AuditPage(List<AuditView> entrees, long total, int page, int taille) {}

    private final AuditLogRepository logs;
    private final UserRepository users;

    public AuditController(AuditLogRepository logs, UserRepository users) {
        this.logs = logs;
        this.users = users;
    }

    @GetMapping
    public AuditPage list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int taille) {
        int size = Math.min(Math.max(taille, 1), 200);
        Page<AuditLog> result = logs.findAllByOrderByDateHeureDesc(PageRequest.of(Math.max(page, 0), size));
        return new AuditPage(views(result.getContent()), result.getTotalElements(), result.getNumber(), size);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export() {
        StringBuilder csv = new StringBuilder("date;utilisateur;action;objet;details\n");
        for (AuditView v : views(logs.findAllByOrderByDateHeureDesc(PageRequest.of(0, 10_000)).getContent())) {
            csv.append(String.join(";", v.date().toString(), cell(v.utilisateur()), cell(v.action()), cell(v.objet()), cell(v.details())))
                    .append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"journal_audit_flowpulse.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private List<AuditView> views(List<AuditLog> entries) {
        List<UUID> ids = entries.stream().map(AuditLog::getUtilisateurId).filter(Objects::nonNull).distinct().toList();
        Map<UUID, String> emails = users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getEmail, (a, b) -> a));
        Function<UUID, String> who = id -> id == null ? "système" : emails.getOrDefault(id, id.toString());
        return entries.stream()
                .map(l -> new AuditView(l.getId(), l.getDateHeure(), who.apply(l.getUtilisateurId()), l.getAction(), l.getObjetConcerne(), l.getDetails()))
                .toList();
    }

    /** Cellule CSV : guillemets doublés, et pas de formule interprétée par un tableur. */
    private static String cell(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"").replace("\n", " ");
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        return "\"" + v + "\"";
    }
}
