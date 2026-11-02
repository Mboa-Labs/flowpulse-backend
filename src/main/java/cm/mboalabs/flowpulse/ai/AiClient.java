package cm.mboalabs.flowpulse.ai;

import cm.mboalabs.flowpulse.config.FlowPulseProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/** Client du service IA (réseau interne). Les réponses sont relayées telles quelles au frontend. */
@Component
public class AiClient {

    private final RestClient rest;

    public AiClient(FlowPulseProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        // Le superviseur peut mettre plus d'une minute à répondre sur un serveur sans carte graphique
        factory.setReadTimeout(Duration.ofSeconds(240));
        this.rest = RestClient.builder().baseUrl(properties.aiService().url()).requestFactory(factory).build();
    }

    public JsonNode get(String path) {
        return call(() -> rest.get().uri(path).retrieve().body(JsonNode.class));
    }

    /** GET avec variables d'adresse encodées par RestClient (noms de zones avec espaces ou accents). */
    public JsonNode get(String template, Object... variables) {
        return call(() -> rest.get().uri(template, variables).retrieve().body(JsonNode.class));
    }

    public JsonNode post(String path, Object body) {
        return call(() -> rest.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body == null ? "{}" : body)
                .retrieve().body(JsonNode.class));
    }

    private JsonNode call(java.util.function.Supplier<JsonNode> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service IA indisponible : " + e.getMessage());
        }
    }
}
