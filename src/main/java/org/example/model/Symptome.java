package org.example.model;

public class Symptome {

    private int id;
    private String nom;
    private String description;
    private Integer patologieId;
    private String patologieNom;

    public Symptome() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPatologieId() { return patologieId; }
    public void setPatologieId(Integer patologieId) { this.patologieId = patologieId; }

    public String getPatologieNom() { return patologieNom; }
    public void setPatologieNom(String patologieNom) { this.patologieNom = patologieNom; }

    @Override
    public String toString() {
        return nom;
    }
}