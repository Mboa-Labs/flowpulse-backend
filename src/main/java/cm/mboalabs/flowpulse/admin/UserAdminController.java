package cm.mboalabs.flowpulse.admin;

import cm.mboalabs.flowpulse.audit.AuditService;
import cm.mboalabs.flowpulse.auth.AuthenticatedUser;
import cm.mboalabs.flowpulse.user.Role;
import cm.mboalabs.flowpulse.user.User;
import cm.mboalabs.flowpulse.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** F4.1 : gestion des comptes par l'administrateur. */
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    public record UserView(UUID id, String nom, String email, Role role, boolean actif) {
        static UserView of(User u) {
            return new UserView(u.getId(), u.getNom(), u.getEmail(), u.getRole(), u.isActif());
        }
    }

    public record CreateUser(@NotBlank String nom, @NotBlank @Email String email,
                             @NotBlank @Size(min = 8) String password, @NotNull Role role) {}

    public record UpdateUser(String nom, Role role, Boolean actif) {}

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserAdminController(UserRepository users, PasswordEncoder passwordEncoder, AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @GetMapping
    public List<UserView> list() {
        return users.findAll().stream().map(UserView::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody CreateUser body, @AuthenticationPrincipal AuthenticatedUser admin) {
        if (users.existsByEmailIgnoreCase(body.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
        }
        User user = users.save(new User(body.nom(), body.email(), passwordEncoder.encode(body.password()), body.role()));
        audit.log(admin.id(), "UTILISATEUR_CREE", "utilisateur:" + user.getId(), body.role().name());
        return UserView.of(user);
    }

    @PatchMapping("/{id}")
    @Transactional
    public UserView update(@PathVariable UUID id, @RequestBody UpdateUser body, @AuthenticationPrincipal AuthenticatedUser admin) {
        User user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (body.nom() != null) user.setNom(body.nom());
        if (body.role() != null) user.setRole(body.role());
        if (body.actif() != null) user.setActif(body.actif());
        audit.log(admin.id(), "UTILISATEUR_MODIFIE", "utilisateur:" + id, body.toString());
        return UserView.of(user);
    }
}
