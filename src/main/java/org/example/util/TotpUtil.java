package org.example.util;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

public class TotpUtil {
    private static final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // Generate a new secret key for a user
    public static String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    // Get the QR code URL (use with Google Charts API to render it)
    public static String getQrUrl(String email, String secret) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                "MyUpskilly", email,
                new GoogleAuthenticatorKey.Builder(secret).build()
        );
    }

    // Verify the 6-digit code entered by user
    public static boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }
}