package cm.mboalabs.flowpulse.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Journal des actions critiques (F0.2) : connexions, décisions sur les alertes et recommandations, ajouts à la KB. */
@Entity
@Table(name = "journal_audit")
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Column(nullable = false)
    private String action;

    @Column(name = "objet_concerne")
    private String objetConcerne;

    @Column(length = 2000)
    private String details;

    @Column(name = "date_heure", nullable = false)
    private Instant dateHeure = Instant.now();

    protected AuditLog() {}

    public AuditLog(UUID utilisateurId, String action, String objetConcerne, String details) {
        this.utilisateurId = utilisateurId;
        this.action = action;
        this.objetConcerne = objetConcerne;
        this.details = details;
    }

    public UUID getId() { return id; }
    public UUID getUtilisateurId() { return utilisateurId; }
    public String getAction() { return action; }
    public String getObjetConcerne() { return objetConcerne; }
    public String getDetails() { return details; }
    public Instant getDateHeure() { return dateHeure; }
}
