package entities;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Equipement {
    private Long id;
    private String nom;
    private String description;
    private String categorie;
    private String type; // VENTE / LOCATION
    private BigDecimal prix;
    private String ville;
    private String statut; // DISPONIBLE / VENDU / LOUE
    private Timestamp dateAjout;

    public Equipement() {}

    // Constructeur pour INSERT
    public Equipement(String nom, String description, String categorie, String type, BigDecimal prix, String ville, String statut) {
        this.nom = nom;
        this.description = description;
        this.categorie = categorie;
        this.type = type;
        this.prix = prix;
        this.ville = ville;
        this.statut = statut != null ? statut : "DISPONIBLE";
    }

    // Constructeur complet pour SELECT
    public Equipement(Long id, String nom, String description, String categorie, String type, BigDecimal prix, String ville, String statut, Timestamp dateAjout) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.categorie = categorie;
        this.type = type;
        this.prix = prix;
        this.ville = ville;
        this.statut = statut;
        this.dateAjout = dateAjout;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Timestamp getDateAjout() { return dateAjout; }
    public void setDateAjout(Timestamp dateAjout) { this.dateAjout = dateAjout; }

    @Override
    public String toString() {
        return "Equipement{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", categorie='" + categorie + '\'' +
                ", type='" + type + '\'' +
                ", prix=" + prix +
                ", ville='" + ville + '\'' +
                ", statut='" + statut + '\'' +
                ", dateAjout=" + dateAjout +
                '}';
    }
}
