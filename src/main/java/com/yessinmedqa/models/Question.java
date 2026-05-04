package com.yessinmedqa.models;

public class Question {

    private int id;
    private int userId;
    private String categorie;
    private String titre;
    private String description;
    private boolean sousTraitement;
    private boolean aDesAllergies;
    private String fichierChemin;
    private String statut;
    private double taille;
    private double poids;
    private String descriptionTraitement;
    private String descriptionAllergies;

    public Question() {}

    public Question(int userId, String titre, String description,
                    boolean sousTraitement, boolean aDesAllergies,
                    String fichierChemin, double taille, double poids,
                    String descriptionTraitement, String descriptionAllergies) {
        this.userId = userId;
        this.titre = titre;
        this.description = description;
        this.sousTraitement = sousTraitement;
        this.aDesAllergies = aDesAllergies;
        this.fichierChemin = fichierChemin;
        this.taille = taille;
        this.poids = poids;
        this.descriptionTraitement = descriptionTraitement;
        this.descriptionAllergies = descriptionAllergies;
        this.statut = "en_attente";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSousTraitement() { return sousTraitement; }
    public void setSousTraitement(boolean sousTraitement) { this.sousTraitement = sousTraitement; }

    public boolean isaDesAllergies() { return aDesAllergies; }
    public void setaDesAllergies(boolean aDesAllergies) { this.aDesAllergies = aDesAllergies; }

    public String getFichierChemin() { return fichierChemin; }
    public void setFichierChemin(String fichierChemin) { this.fichierChemin = fichierChemin; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public double getTaille() { return taille; }
    public void setTaille(double taille) { this.taille = taille; }

    public double getPoids() { return poids; }
    public void setPoids(double poids) { this.poids = poids; }

    public String getDescriptionTraitement() { return descriptionTraitement; }
    public void setDescriptionTraitement(String descriptionTraitement) { this.descriptionTraitement = descriptionTraitement; }

    public String getDescriptionAllergies() { return descriptionAllergies; }
    public void setDescriptionAllergies(String descriptionAllergies) { this.descriptionAllergies = descriptionAllergies; }
}