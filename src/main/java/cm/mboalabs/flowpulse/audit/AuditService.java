package cm.mboalabs.flowpulse.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** Transaction séparée : l'entrée d'audit reste écrite même si l'action appelante échoue ensuite. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID utilisateurId, String action, String objetConcerne, String details) {
        repository.save(new AuditLog(utilisateurId, action, objetConcerne, details));
    }
}
