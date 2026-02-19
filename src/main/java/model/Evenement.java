package model;

import java.time.LocalDate;

public class Evenement {
    private int id;
    private String nom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String lieu;
    private String description;
    private String statut; // PLANIFIE, EN_COURS, TERMINE, ANNULE

    public Evenement() {}

    public Evenement(int id, String nom, LocalDate dateDebut, LocalDate dateFin,
                     String lieu, String description, String statut) {
        this.id = id;
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.description = description;
        this.statut = statut;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return nom;
    }
}