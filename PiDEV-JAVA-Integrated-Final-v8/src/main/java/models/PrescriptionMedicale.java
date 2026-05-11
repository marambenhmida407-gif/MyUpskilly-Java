package models;

import java.util.Date;

public class PrescriptionMedicale {
    private int id;
    private Date datePrescription;
    private String medicaments;
    private String recommandations;
    private String suivi;
    private int suiviTherapeutiqueId;

    public PrescriptionMedicale() {}

    public PrescriptionMedicale(int id, Date datePrescription, String medicaments,
                                  String recommandations, String suivi, int suiviTherapeutiqueId) {
        this.id = id;
        this.datePrescription = datePrescription;
        this.medicaments = medicaments;
        this.recommandations = recommandations;
        this.suivi = suivi;
        this.suiviTherapeutiqueId = suiviTherapeutiqueId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Date getDatePrescription() { return datePrescription; }
    public void setDatePrescription(Date datePrescription) { this.datePrescription = datePrescription; }

    public String getMedicaments() { return medicaments; }
    public void setMedicaments(String medicaments) { this.medicaments = medicaments; }

    public String getRecommandations() { return recommandations; }
    public void setRecommandations(String recommandations) { this.recommandations = recommandations; }

    public String getSuivi() { return suivi; }
    public void setSuivi(String suivi) { this.suivi = suivi; }

    public int getSuiviTherapeutiqueId() { return suiviTherapeutiqueId; }
    public void setSuiviTherapeutiqueId(int suiviTherapeutiqueId) { this.suiviTherapeutiqueId = suiviTherapeutiqueId; }

    @Override
    public String toString() { return medicaments; }
}
