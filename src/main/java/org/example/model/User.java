package org.example.model;

public class User {

    private int id;
    private String email;
    private String roles;
    private String password;
    private String userName;
    private String nom;
    private String prenom;
    private String photo;
    private boolean isVerified = false;
    private String specialite;
    private String descriptionSpecialite;
    private String adresse;
    private String grade;
    private String googleAuthenticatorSecret;
    private Boolean etat;

    public User() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getDescriptionSpecialite() { return descriptionSpecialite; }
    public void setDescriptionSpecialite(String descriptionSpecialite) { this.descriptionSpecialite = descriptionSpecialite; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getGoogleAuthenticatorSecret() { return googleAuthenticatorSecret; }
    public void setGoogleAuthenticatorSecret(String s) { this.googleAuthenticatorSecret = s; }

    public Boolean getEtat() { return etat; }
    public void setEtat(Boolean etat) { this.etat = etat; }

    @Override
    public String toString() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "") + " <" + email + ">";
    }
}