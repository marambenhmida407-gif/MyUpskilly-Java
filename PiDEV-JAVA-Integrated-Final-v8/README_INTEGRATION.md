# Guide d'intégration — Gestion Utilisateurs (Maraam → EspritMédical)

## Étape 1 — Base de données

Exécuter **une seule fois** dans MySQL Workbench ou phpMyAdmin :
```
database_migration.sql
```
Ajoute 8 colonnes à votre table `users` existante. Aucune donnée existante n'est supprimée.

---

## Étape 2 — pom.xml

Remplacer votre `pom.xml` par le fichier fourni.
Seul ajout : `com.warrenstrange:googleauth:1.5.0` (lib 2FA).
Faire Maven → Reload Project dans IntelliJ.

---

## Étape 3 — Fichiers Java à copier

Placer dans votre projet (respecter les chemins) :

| Fichier fourni | Destination dans src/main/java/ |
|---|---|
| `models/User.java` | `models/User.java` ← **nouveau** |
| `utils/Session.java` | `utils/Session.java` ← **remplace** |
| `utils/TotpUtil.java` | `utils/TotpUtil.java` ← **nouveau** |
| `services/yessine/UserService.java` | `services/yessine/UserService.java` ← **remplace** |
| `controllers/yessine/LoginController.java` | `controllers/yessine/LoginController.java` ← **remplace** |
| `controllers/yessine/RegisterController.java` | `controllers/yessine/RegisterController.java` ← **nouveau** |
| `controllers/yessine/Setup2FAController.java` | `controllers/yessine/Setup2FAController.java` ← **nouveau** |
| `controllers/yessine/Verify2FAController.java` | `controllers/yessine/Verify2FAController.java` ← **nouveau** |
| `controllers/yessine/UserManagementController.java` | `controllers/yessine/UserManagementController.java` ← **nouveau** |

---

## Étape 4 — Fichiers FXML à copier

Placer dans `src/main/resources/yessine/` :

| Fichier | Action |
|---|---|
| `login.fxml` | **Remplace** (ajoute le bouton S'inscrire) |
| `register.fxml` | **Nouveau** |
| `setup_2fa.fxml` | **Nouveau** |
| `verify_2fa.fxml` | **Nouveau** |
| `user_management.fxml` | **Nouveau** |

---

## Étape 5 — Relier UserManagement à admin.fxml (optionnel)

Dans votre `AdminController.java`, ajouter un bouton ou menu qui charge la page :
```java
Parent root = FXMLLoader.load(getClass().getResource("/yessine/user_management.fxml"));
someContainer.getChildren().setAll(root);
// ou en standalone :
emailField.getScene().setRoot(root);
```

---

## Ce qui change au runtime

### Login normal (sans 2FA)
```
login.fxml → LoginController.seConnecter()
  → Vérif email+password (UserService.login)
  → Vérif etat (compte actif ?)
  → Session remplie
  → Routing par rôle : MainLayout / admin.fxml / poserQuestion.fxml
```

### Login avec 2FA configurée
```
login.fxml → LoginController.seConnecter()
  → Auth OK + has2FA() = true
  → verify_2fa.fxml → code OK
  → Routing par rôle (même destination)
```

### Inscription
```
login.fxml → bouton "S'inscrire"
  → register.fxml → RegisterController
  → Validation + UserService.save()
  → Retour login.fxml
```

### Configuration 2FA (première fois)
```
setup_2fa.fxml → Setup2FAController.setUser(user)
  → Génération secret TOTP
  → QR code affiché (ZXing)
  → Code validé → UserService.saveGoogleAuthSecret()
  → Routing par rôle
```

---

## Nouveaux champs dans Session

```java
Session.getPhoto()       // URL photo de profil
Session.getEtat()        // Boolean : compte actif ou non
Session.isVerified()     // boolean : compte vérifié
```

---

## ⚠️ Points d'attention

1. **Mot de passe** : toujours en clair pour l'instant (cohérent avec l'existant). Pour hacher plus tard, ajouter BCrypt et modifier `UserService.login()` + `RegisterController`.

2. **`UserService` dans `services/aziz/UserService.java`** : non modifié, continue de fonctionner pour la liste des patients dans les consultations.

3. **`mapRow()` protégé** : si la migration SQL n'est pas encore faite, les nouvelles colonnes sont ignorées silencieusement (try/catch par colonne).

4. **2FA facultative** : un utilisateur sans `google_authenticator_secret` se connecte normalement. La 2FA se configure via `setup_2fa.fxml` quand vous décidez de l'exposer.
