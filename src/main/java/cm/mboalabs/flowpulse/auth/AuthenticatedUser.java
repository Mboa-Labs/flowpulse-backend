package cm.mboalabs.flowpulse.auth;

import java.util.UUID;

/** Utilisateur courant, reconstruit à partir du jeton d'accès. */
public record AuthenticatedUser(UUID id, String email, String role) {}
