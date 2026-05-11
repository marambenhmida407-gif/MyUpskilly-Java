package utils;

/**
 * Session utilisateur — singleton statique.
 * Stocke les données en mémoire pendant toute la durée de la session.
 * Champs originaux + photo, etat, isVerified (intégration Maraam).
 */
public class Session {

    // ── Champs originaux ──────────────────────────────────────────────────────
    private static int    userId;
    private static String nom;
    private static String prenom;
    private static String email;
    private static String role;       // "patient" | "doctor" | "admin"
    private static String specialite;

    // ── Champs ajoutés (Maraam) ───────────────────────────────────────────────
    private static String  photo;
    private static Boolean etat;
    private static boolean isVerified;

    // ── Getters / Setters originaux ───────────────────────────────────────────

    public static int    getUserId()              { return userId; }
    public static void   setUserId(int id)        { Session.userId = id; }

    public static String getNom()                 { return nom; }
    public static void   setNom(String n)         { Session.nom = n; }

    public static String getPrenom()              { return prenom; }
    public static void   setPrenom(String p)      { Session.prenom = p; }

    public static String getEmail()               { return email; }
    public static void   setEmail(String e)       { Session.email = e; }

    public static String getRole()                { return role; }
    public static void   setRole(String r)        { Session.role = r; }

    public static String getSpecialite()          { return specialite; }
    public static void   setSpecialite(String s)  { Session.specialite = s; }

    // ── Getters / Setters ajoutés ─────────────────────────────────────────────

    public static String  getPhoto()              { return photo; }
    public static void    setPhoto(String p)      { Session.photo = p; }

    public static Boolean getEtat()              { return etat; }
    public static void    setEtat(Boolean e)     { Session.etat = e; }

    public static boolean isVerified()            { return isVerified; }
    public static void    setVerified(boolean v)  { Session.isVerified = v; }

    // ── Helpers rôle ─────────────────────────────────────────────────────────

    public static boolean isDoctor()  { return "doctor".equals(role); }
    public static boolean isPatient() { return "patient".equals(role); }
    public static boolean isAdmin()   { return "admin".equals(role); }

    // ── Clear ─────────────────────────────────────────────────────────────────

    public static void clear() {
        userId     = 0;
        nom        = null;
        prenom     = null;
        email      = null;
        role       = null;
        specialite = null;
        photo      = null;
        etat       = null;
        isVerified = false;
    }
}
