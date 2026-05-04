package com.yessinmedqa.models;

public class Reponse {

    private int id;
    private int questionId;
    private int doctorId;
    private String contenu;
    private String questionTitre;
    private String questionDescription;
    private String doctorNom;
    private String doctorPrenom;
    private String doctorSpecialite;
    private int rating;
    private String fichierChemin;
    private String categorie;
    private int questionUserId;
    private String updatedAt;

    public Reponse() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public String getQuestionTitre() { return questionTitre; }
    public void setQuestionTitre(String questionTitre) { this.questionTitre = questionTitre; }

    public String getQuestionDescription() { return questionDescription; }
    public void setQuestionDescription(String questionDescription) { this.questionDescription = questionDescription; }

    public String getDoctorNom() { return doctorNom; }
    public void setDoctorNom(String doctorNom) { this.doctorNom = doctorNom; }

    public String getDoctorPrenom() { return doctorPrenom; }
    public void setDoctorPrenom(String doctorPrenom) { this.doctorPrenom = doctorPrenom; }

    public String getDoctorSpecialite() { return doctorSpecialite; }
    public void setDoctorSpecialite(String doctorSpecialite) { this.doctorSpecialite = doctorSpecialite; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getFichierChemin() { return fichierChemin; }
    public void setFichierChemin(String fichierChemin) { this.fichierChemin = fichierChemin; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }


    public int getQuestionUserId() { return questionUserId; }
    public void setQuestionUserId(int questionUserId) { this.questionUserId = questionUserId; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}