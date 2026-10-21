package cm.mboalabs.flowpulse.auth;

import cm.mboalabs.flowpulse.audit.AuditService;
import cm.mboalabs.flowpulse.auth.AuthDtos.LoginRequest;
import cm.mboalabs.flowpulse.auth.AuthDtos.RefreshRequest;
import cm.mboalabs.flowpulse.auth.AuthDtos.TokenResponse;
import cm.mboalabs.flowpulse.auth.AuthDtos.UserInfo;
import cm.mboalabs.flowpulse.user.User;
import cm.mboalabs.flowpulse.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService audit;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService, AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.audit = audit;
    }

    @PostMapping("/login")
    @Transactional
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
                .filter(User::isActif)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> {
                    audit.log(null, "LOGIN_ECHEC", "utilisateur", request.email());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
                });
        user.setDerniereConnexion(Instant.now());
        audit.log(user.getId(), "LOGIN_OK", "utilisateur", null);
        return tokens(user);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        User user = jwtService.parse(request.refreshToken(), JwtService.TYPE_REFRESH)
                .flatMap(claims -> users.findById(UUID.fromString(claims.getSubject())))
                .filter(User::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Jeton de rafraîchissement invalide"));
        return tokens(user);
    }

    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return users.findById(principal.id())
                .map(AuthController::info)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private TokenResponse tokens(User user) {
        return new TokenResponse(
                jwtService.accessToken(user), jwtService.refreshToken(user), jwtService.accessTtlSeconds(), info(user));
    }

    static UserInfo info(User user) {
        return new UserInfo(user.getId(), user.getNom(), user.getEmail(), user.getRole().name());
    }
}
