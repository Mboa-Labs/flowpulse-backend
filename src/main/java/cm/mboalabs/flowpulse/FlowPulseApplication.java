package cm.mboalabs.flowpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Pas d'utilisateur en mémoire généré par Spring : l'authentification passe uniquement par les JWT
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class FlowPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowPulseApplication.class, args);
    }
}
