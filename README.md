# FlowPulse — Backend

Backend métier de [FlowPulse](https://github.com/Mboa-Labs/flowpulse) : Spring Boot 3, Java 21, PostgreSQL.

- Authentification JWT (jeton d'accès 15 min, jeton de rafraîchissement 7 jours), mots de passe BCrypt
- Rôles : `GESTIONNAIRE`, `OPERATEUR`, `URBANISTE`, `ADMIN` ; premier administrateur créé au démarrage
- Journal d'audit des actions critiques
- Administration des comptes (`/api/admin/users`)

## Lancer

```bash
mvn spring-boot:run          # nécessite PostgreSQL (voir docker-compose du dépôt flowpulse)
mvn test                     # tests sur base H2 en mémoire
```

Variables principales : `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `AI_SERVICE_URL`, `CORS_ORIGINS`.

Documentation de l'API : <http://localhost:8080/swagger-ui.html>
