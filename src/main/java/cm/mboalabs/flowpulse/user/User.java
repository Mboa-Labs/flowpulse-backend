package cm.mboalabs.flowpulse.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "utilisateur")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "mot_de_passe_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation = Instant.now();

    @Column(name = "derniere_connexion")
    private Instant derniereConnexion;

    protected User() {}

    public User(String nom, String email, String passwordHash, Role role) {
        this.nom = nom;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID getId() { return id; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActif() { return actif; }
    public Instant getDateCreation() { return dateCreation; }
    public Instant getDerniereConnexion() { return derniereConnexion; }

    public void setNom(String nom) { this.nom = nom; }
    public void setRole(Role role) { this.role = role; }
    public void setActif(boolean actif) { this.actif = actif; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setDerniereConnexion(Instant derniereConnexion) { this.derniereConnexion = derniereConnexion; }
}
