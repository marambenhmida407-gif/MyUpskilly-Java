package org.example.service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.example.model.Consultation;

public class StripeService {

    // APRÈS ✅
    private static final String STRIPE_SECRET_KEY = "";
    public String createCheckoutUrl(Consultation c, String patientNom) {
        Stripe.apiKey = STRIPE_SECRET_KEY;

        long amount = calculateAmount(c);
        String baseUrl = ConsultationWebServer.getInstance().getBaseUrl();

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(baseUrl + "/payment/success/" + c.getId())
                    .setCancelUrl(baseUrl + "/consultation/" + c.getId())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("eur")
                                    .setUnitAmount(amount)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Consultation #" + c.getId() + " - " + patientNom)
                                            .setDescription("Motif: " + c.getMotif())
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private long calculateAmount(Consultation c) {
        if (c.getPathologieNom() != null) {
            String path = c.getPathologieNom().toLowerCase();
            if (path.contains("cancer") || path.contains("hiv")) return 15000;
            if (path.contains("diabete")) return 10000;
        }
        return 5000;
    }
}