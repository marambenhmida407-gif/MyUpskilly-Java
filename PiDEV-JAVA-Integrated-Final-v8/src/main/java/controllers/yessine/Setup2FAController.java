package controllers.yessine;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import models.User;
import services.yessine.UserService;
import utils.TotpUtil;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Contrôleur de configuration 2FA.
 * CORRIGÉ par rapport à la version Maraam :
 *  - Génère et affiche le QR code (ZXing via PixelWriter, sans javafx-swing)
 *  - Sauvegarde le secret en BDD après validation du premier code
 *  - Redirige vers la bonne page selon le rôle (via LoginController.navigateByRole)
 */
public class Setup2FAController {

    @FXML private ImageView qrImageView;
    @FXML private Label     lblSecretKey;
    @FXML private Label     lblEmail;
    @FXML private TextField tfCode;
    @FXML private Label     lblError;
    @FXML private Label     lblInfo;

    private User   currentUser;
    private String secret;

    private final UserService userService = new UserService();

    /**
     * Appelé après chargement du FXML.
     * Injecter l'utilisateur avant que le controller soit utilisé.
     */
    public void setUser(User user) {
        this.currentUser = user;
        this.secret = TotpUtil.generateSecret();

        // Afficher l'email et la clé manuelle
        if (lblEmail    != null) lblEmail.setText(user.getEmail());
        if (lblSecretKey != null) lblSecretKey.setText(secret);

        // Générer et afficher le QR code
        renderQrCode();
    }

    @FXML
    public void initialize() {
        if (lblError != null) lblError.setText("");
        if (lblInfo  != null) lblInfo.setText(
                "Scannez ce QR code avec Google Authenticator ou Authy,\n" +
                "puis entrez le code à 6 chiffres pour confirmer.");
    }

    @FXML
    public void handleConfirm() {
        if (lblError != null) lblError.setText("");

        String codeText = tfCode.getText().trim();
        if (codeText.isEmpty() || !codeText.matches("\\d{6}")) {
            lblError.setText("Entrez un code à 6 chiffres valide.");
            return;
        }

        int code = Integer.parseInt(codeText);

        if (TotpUtil.verifyCode(secret, code)) {
            // ✅ Code correct → sauvegarder le secret en BDD
            try {
                userService.saveGoogleAuthSecret(currentUser.getId(), secret);
                currentUser.setGoogleAuthenticatorSecret(secret);
                System.out.println("✅ 2FA configurée pour " + currentUser.getEmail());
                // Naviguer vers la page du rôle
                LoginController.navigateByRole(tfCode);
            } catch (Exception e) {
                lblError.setText("Erreur lors de la sauvegarde : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            lblError.setText("❌ Code incorrect. Vérifiez l'heure de votre téléphone et réessayez.");
            tfCode.clear();
        }
    }

    @FXML
    public void handleSkip() {
        // L'utilisateur ne veut pas configurer la 2FA maintenant
        // Naviguer directement vers sa page par rôle
        LoginController.navigateByRole(tfCode);
    }

    // ── Génération QR code ────────────────────────────────────────────────────

    private void renderQrCode() {
        if (qrImageView == null || currentUser == null || secret == null) return;
        try {
            String qrUrl = TotpUtil.getQrUrl(currentUser.getEmail(), secret);

            QRCodeWriter writer = new QRCodeWriter();
            int size = 200;
            BitMatrix matrix = writer.encode(qrUrl, BarcodeFormat.QR_CODE, size, size);

            // Conversion BufferedImage → WritableImage (sans javafx-swing)
            BufferedImage buffered = MatrixToImageWriter.toBufferedImage(matrix);
            WritableImage fxImage = new WritableImage(size, size);
            PixelWriter pw = fxImage.getPixelWriter();
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    pw.setArgb(x, y, buffered.getRGB(x, y));
                }
            }
            qrImageView.setImage(fxImage);
        } catch (Exception e) {
            System.err.println("Erreur génération QR: " + e.getMessage());
            if (lblError != null)
                lblError.setText("QR non disponible — utilisez la clé manuelle ci-dessus.");
        }
    }
}
