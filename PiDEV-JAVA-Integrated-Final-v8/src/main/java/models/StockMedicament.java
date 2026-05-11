package models;

import java.util.Date;

public class StockMedicament {

    private int    id;
    private String nom;
    private String forme;
    private String dosage;
    private int    quantite;
    private String unite;
    private int    seuilAlerte;
    private double prix;
    private String fournisseur;
    private Date   dateExpiry;

    public StockMedicament() {}

    public StockMedicament(int id, String nom, String forme, String dosage,
                           int quantite, String unite, int seuilAlerte,
                           double prix, String fournisseur, Date dateExpiry) {
        this.id          = id;
        this.nom         = nom;
        this.forme       = forme;
        this.dosage      = dosage;
        this.quantite    = quantite;
        this.unite       = unite;
        this.seuilAlerte = seuilAlerte;
        this.prix        = prix;
        this.fournisseur = fournisseur;
        this.dateExpiry  = dateExpiry;
    }

    // ── Computed helpers ─────────────────────────────────────────────────────

    /** True when quantity is zero */
    public boolean isOutOfStock()    { return quantite <= 0; }

    /** True when quantity is low but not zero */
    public boolean isLowStock()      { return quantite > 0 && quantite <= seuilAlerte; }

    /** Traffic-light status string */
    public String getStockStatus() {
        if (isOutOfStock()) return "Rupture";
        if (isLowStock())   return "Stock faible";
        return "Disponible";
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public int    getId()           { return id; }
    public void   setId(int id)     { this.id = id; }

    public String getNom()          { return nom; }
    public void   setNom(String v)  { this.nom = v; }

    public String getForme()        { return forme; }
    public void   setForme(String v){ this.forme = v; }

    public String getDosage()          { return dosage; }
    public void   setDosage(String v)  { this.dosage = v; }

    public int    getQuantite()        { return quantite; }
    public void   setQuantite(int v)   { this.quantite = v; }

    public String getUnite()           { return unite; }
    public void   setUnite(String v)   { this.unite = v; }

    public int    getSeuilAlerte()     { return seuilAlerte; }
    public void   setSeuilAlerte(int v){ this.seuilAlerte = v; }

    public double getPrix()            { return prix; }
    public void   setPrix(double v)    { this.prix = v; }

    public String getFournisseur()         { return fournisseur; }
    public void   setFournisseur(String v) { this.fournisseur = v; }

    public Date   getDateExpiry()         { return dateExpiry; }
    public void   setDateExpiry(Date v)   { this.dateExpiry = v; }

    @Override
    public String toString() {
        return nom + (dosage.isEmpty() ? "" : " " + dosage)
                + " — " + quantite + " " + unite;
    }
}
