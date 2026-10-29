package cm.mboalabs.flowpulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "flowpulse")
public record FlowPulseProperties(Jwt jwt, Cors cors, AiService aiService, BootstrapAdmin bootstrapAdmin, DemoUsers demoUsers) {

    public record Jwt(String secret, long accessMinutes, long refreshDays) {}

    public record Cors(List<String> allowedOrigins) {}

    public record AiService(String url) {}

    public record BootstrapAdmin(String email, String password) {}

    /** Comptes de démonstration (un par rôle), désactivés par défaut. */
    public record DemoUsers(boolean enabled, String password) {}
}
