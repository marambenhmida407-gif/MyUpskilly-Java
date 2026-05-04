package org.example.model;

import java.time.LocalDateTime;

public class Consultation {

    private int id;
    private int patientId;
    private String patientNom;
    private String patientEmail;
    private Integer pathologieId;
    private String pathologieNom;
    private LocalDateTime dateConsultation;
    private String motif;
    private String diagnostic;
    private String observations;
    private String ordonnance;
    private String statut;

    public Consultation() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getPatientNom() { return patientNom; }
    public void setPatientNom(String patientNom) { this.patientNom = patientNom; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public Integer getPathologieId() { return pathologieId; }
    public void setPathologieId(Integer pathologieId) { this.pathologieId = pathologieId; }

    public String getPathologieNom() { return pathologieNom; }
    public void setPathologieNom(String pathologieNom) { this.pathologieNom = pathologieNom; }

    public LocalDateTime getDateConsultation() { return dateConsultation; }
    public void setDateConsultation(LocalDateTime dateConsultation) { this.dateConsultation = dateConsultation; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getDiagnostic() { return diagnostic; }
    public void setDiagnostic(String diagnostic) { this.diagnostic = diagnostic; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getOrdonnance() { return ordonnance; }
    public void setOrdonnance(String ordonnance) { this.ordonnance = ordonnance; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Consultation #" + id + " - " + motif + " (" + statut + ")";
    }
}