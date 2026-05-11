package models;

import java.util.Date;

public class SuiviTherapeutique {
    private int id;
    private int patientId;       // FK → users.id (role='patient')
    private String patientNom;   // joined field for display only
    private Date dateDebut;
    private Date dateFin;
    private String typeSuivi;
    private String objectifTherapeutique;
    private String statut;

    public SuiviTherapeutique() {}

    /** Full constructor used by the service when loading from DB with JOIN */
    public SuiviTherapeutique(int id, int patientId, String patientNom,
                               Date dateDebut, Date dateFin,
                               String typeSuivi, String objectifTherapeutique, String statut) {
        this.id = id;
        this.patientId = patientId;
        this.patientNom = patientNom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.typeSuivi = typeSuivi;
        this.objectifTherapeutique = objectifTherapeutique;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getPatientNom() { return patientNom; }
    public void setPatientNom(String patientNom) { this.patientNom = patientNom; }

    public Date getDateDebut() { return dateDebut; }
    public void setDateDebut(Date dateDebut) { this.dateDebut = dateDebut; }

    public Date getDateFin() { return dateFin; }
    public void setDateFin(Date dateFin) { this.dateFin = dateFin; }

    public String getTypeSuivi() { return typeSuivi; }
    public void setTypeSuivi(String typeSuivi) { this.typeSuivi = typeSuivi; }

    public String getObjectifTherapeutique() { return objectifTherapeutique; }
    public void setObjectifTherapeutique(String objectifTherapeutique) { this.objectifTherapeutique = objectifTherapeutique; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() { return typeSuivi + " - " + statut; }
}
