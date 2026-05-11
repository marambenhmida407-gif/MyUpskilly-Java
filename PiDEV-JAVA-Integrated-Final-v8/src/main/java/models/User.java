package models;

/**
 * Modèle User — table `users` de la base esprit
 * Colonnes originales + colonnes Maraam (photo, is_verified, etat, grade, adresse,
 * description_specialite, google_authenticator_secret, username)
 */
public class User {

    private int     id;
    private String  nom;
    private String  prenom;
    private String  email;
    private String  password;
    private String  role;          // 'admin' | 'doctor' | 'patient'
    private String  specialite;

    // Colonnes ajoutées (migration Maraam)
    private String  username;
    private String  photo;
    private boolean isVerified;
    private Boolean etat;          // null possible sur anciens comptes → traité comme TRUE
    private String  grade;
    private String  adresse;
    private String  descriptionSpecialite;
    private String  googleAuthenticatorSecret;

    public User() {}

    // ── Getters / Setters originaux ───────────────────────────────────────────

    public int     getId()           { return id; }
    public void    setId(int id)     { this.id = id; }

    public String  getNom()          { return nom; }
    public void    setNom(String n)  { this.nom = n; }

    public String  getPrenom()              { return prenom; }
    public void    setPrenom(String p)      { this.prenom = p; }

    public String  getEmail()               { return email; }
    public void    setEmail(String e)       { this.email = e; }

    public String  getPassword()            { return password; }
    public void    setPassword(String p)    { this.password = p; }

    public String  getRole()               { return role; }
    public void    setRole(String r)       { this.role = r; }

    public String  getSpecialite()          { return specialite; }
    public void    setSpecialite(String s)  { this.specialite = s; }

    // ── Getters / Setters nouveaux ────────────────────────────────────────────

    public String  getUsername()            { return username; }
    public void    setUsername(String u)    { this.username = u; }

    public String  getPhoto()               { return photo; }
    public void    setPhoto(String p)       { this.photo = p; }

    public boolean isVerified()             { return isVerified; }
    public void    setVerified(boolean v)   { this.isVerified = v; }

    public Boolean getEtat()               { return etat; }
    public void    setEtat(Boolean e)      { this.etat = e; }

    /** Retourne true si le compte est actif (null → considéré actif) */
    public boolean isActif() { return etat == null || Boolean.TRUE.equals(etat); }

    public String  getGrade()               { return grade; }
    public void    setGrade(String g)       { this.grade = g; }

    public String  getAdresse()             { return adresse; }
    public void    setAdresse(String a)     { this.adresse = a; }

    public String  getDescriptionSpecialite()          { return descriptionSpecialite; }
    public void    setDescriptionSpecialite(String d)  { this.descriptionSpecialite = d; }

    public String  getGoogleAuthenticatorSecret()            { return googleAuthenticatorSecret; }
    public void    setGoogleAuthenticatorSecret(String s)    { this.googleAuthenticatorSecret = s; }

    public boolean has2FA() {
        return googleAuthenticatorSecret != null && !googleAuthenticatorSecret.isEmpty();
    }

    @Override
    public String toString() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "") + " <" + email + ">";
    }
}
