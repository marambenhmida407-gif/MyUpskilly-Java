package com.yessinmedqa;

public class Session {

    private static int userId;
    private static String nom;
    private static String prenom;
    private static String email;
    private static String role; // "patient" or "doctor"
    private static String specialite;

    public static boolean isAdmin() { return "admin".equals(role); }
    public static int getUserId() { return userId; }
    public static void setUserId(int userId) { Session.userId = userId; }

    public static String getNom() { return nom; }
    public static void setNom(String nom) { Session.nom = nom; }

    public static String getPrenom() { return prenom; }
    public static void setPrenom(String prenom) { Session.prenom = prenom; }

    public static String getEmail() { return email; }
    public static void setEmail(String email) { Session.email = email; }

    public static String getRole() { return role; }
    public static void setRole(String role) { Session.role = role; }

    public static String getSpecialite() { return specialite; }
    public static void setSpecialite(String specialite) { Session.specialite = specialite; }

    public static boolean isDoctor() { return "doctor".equals(role); }
    public static boolean isPatient() { return "patient".equals(role); }

    public static void clear() {
        userId = 0;
        nom = null;
        prenom = null;
        email = null;
        role = null;
        specialite = null;
    }
}