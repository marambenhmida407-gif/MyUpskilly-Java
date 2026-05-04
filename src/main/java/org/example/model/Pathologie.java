package org.example.model;

public class Pathologie {

    private int id;
    private int userId;
    private String nom;
    private String description;
    private String type;
    private String gravite;

    public Pathologie() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getGravite() { return gravite; }
    public void setGravite(String gravite) { this.gravite = gravite; }

    @Override
    public String toString() {
        return nom + " (" + gravite + ")";
    }
}