package utils;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;

/**
 * Utilitaire TOTP (Time-based One-Time Password) pour la 2FA Google Authenticator.
 * Utilise la librairie com.warrenstrange:googleauth:1.5.0
 * Dépendance à ajouter dans pom.xml (voir pom.xml fourni).
 */
public class TotpUtil {

    private static final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    /** Génère une nouvelle clé secrète TOTP pour un utilisateur */
    public static String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    /**
     * Retourne l'URL otpauth:// à encoder en QR code.
     * Format compatible Google Authenticator / Authy.
     */
    public static String getQrUrl(String email, String secret) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                "EspritMedical",   // Nom de l'application affiché dans Google Auth
                email,
                new GoogleAuthenticatorKey.Builder(secret).build()
        );
    }

    /**
     * Vérifie le code à 6 chiffres entré par l'utilisateur.
     * Fenêtre de tolérance : ±1 période (30 sec) gérée par la lib.
     */
    public static boolean verifyCode(String secret, int code) {
        GoogleAuthenticator gAuthWithWindow = new GoogleAuthenticator(
                new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                        .setWindowSize(5)  // tolère ±2 minutes de décalage
                        .build()
        );
        return gAuthWithWindow.authorize(secret, code);
    }
}
