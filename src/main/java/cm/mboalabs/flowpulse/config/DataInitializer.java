package cm.mboalabs.flowpulse.config;

import cm.mboalabs.flowpulse.user.Role;
import cm.mboalabs.flowpulse.user.User;
import cm.mboalabs.flowpulse.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crée le premier administrateur si aucun compte n'existe, et les comptes de démonstration si demandés (pas d'inscription libre). */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final FlowPulseProperties properties;

    public DataInitializer(UserRepository users, PasswordEncoder passwordEncoder, FlowPulseProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() == 0) {
            var admin = properties.bootstrapAdmin();
            users.save(new User("Administrateur", admin.email(), passwordEncoder.encode(admin.password()), Role.ADMIN));
            log.warn("Compte administrateur initial créé : {} (changez le mot de passe)", admin.email());
        }
        var demo = properties.demoUsers();
        if (demo != null && demo.enabled()) {
            createDemo("Gestionnaire démo", "gestionnaire@flowpulse.local", Role.GESTIONNAIRE, demo.password());
            createDemo("Opérateur démo", "operateur@flowpulse.local", Role.OPERATEUR, demo.password());
            createDemo("Urbaniste démo", "urbaniste@flowpulse.local", Role.URBANISTE, demo.password());
        }
    }

    private void createDemo(String nom, String email, Role role, String password) {
        if (!users.existsByEmailIgnoreCase(email)) {
            users.save(new User(nom, email, passwordEncoder.encode(password), role));
            log.warn("Compte de démonstration créé : {} ({})", email, role);
        }
    }
}
