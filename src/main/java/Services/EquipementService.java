package Services;

import entities.Equipement;
import utils.MyDataBase;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EquipementService {

    private final Connection cnx;

    public EquipementService() {
        cnx = MyDataBase.getInstance().getCnx();
    }

    // CREATE
    public void ajouter(Equipement e) {
        Long id = ajouterRetourId(e);
        if (id != null) e.setId(id);
    }

    /** Ajoute un équipement et retourne son ID généré (pour sauvegarder les attributs). */
    public Long ajouterRetourId(Equipement e) {
        String sql = "INSERT INTO equipement (nom, description, categorie, type, prix, ville, statut, caracteristiques) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getNom());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getCategorie());
            ps.setString(4, e.getType());
            ps.setBigDecimal(5, e.getPrix());
            ps.setString(6, e.getVille());
            ps.setString(7, e.getStatut() != null ? e.getStatut() : "DISPONIBLE");
            ps.setString(8, e.getCaracteristiques());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                long id = rs.getLong(1);
                System.out.println("✅ Équipement ajouté ! id=" + id);
                return id;
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("caracteristiques")) {
                try {
                    String sql2 = "INSERT INTO equipement (nom, description, categorie, type, prix, ville, statut) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps2 = cnx.prepareStatement(sql2, Statement.RETURN_GENERATED_KEYS)) {
                        ps2.setString(1, e.getNom());
                        ps2.setString(2, e.getDescription());
                        ps2.setString(3, e.getCategorie());
                        ps2.setString(4, e.getType());
                        ps2.setBigDecimal(5, e.getPrix());
                        ps2.setString(6, e.getVille());
                        ps2.setString(7, e.getStatut() != null ? e.getStatut() : "DISPONIBLE");
                        ps2.executeUpdate();
                        ResultSet rs2 = ps2.getGeneratedKeys();
                        if (rs2.next()) { return rs2.getLong(1); }
                    }
                } catch (SQLException ex2) {
                    System.out.println("❌ Erreur ajout équipement");
                    ex2.printStackTrace();
                }
            } else {
                System.out.println("❌ Erreur ajout équipement");
                ex.printStackTrace();
            }
        }
        return null;
    }

    // READ ALL (DB -> List -> Stream)
    public List<Equipement> afficher() {
        List<Equipement> list = new ArrayList<>();
        String sqlWithVues = "SELECT id, nom, description, categorie, type, prix, ville, statut, date_ajout, caracteristiques, nombre_vues FROM equipement";
        String sqlBase = "SELECT id, nom, description, categorie, type, prix, ville, statut, date_ajout, caracteristiques FROM equipement";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sqlWithVues)) {
            while (rs.next()) {
                Equipement eq = new Equipement(
                        rs.getLong("id"),
                        rs.getString("nom"),
                        rs.getString("description"),
                        rs.getString("categorie"),
                        rs.getString("type"),
                        rs.getBigDecimal("prix"),
                        rs.getString("ville"),
                        rs.getString("statut"),
                        rs.getTimestamp("date_ajout")
                );
                try { eq.setCaracteristiques(rs.getString("caracteristiques")); } catch (Exception ignored) {}
                try { eq.setNombreVues(rs.getInt("nombre_vues")); } catch (Exception ignored) {}
                list.add(eq);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && (e.getMessage().contains("nombre_vues") || e.getMessage().contains("Unknown column"))) {
                try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sqlBase)) {
                    while (rs.next()) {
                        Equipement eq = new Equipement(rs.getLong("id"), rs.getString("nom"), rs.getString("description"),
                                rs.getString("categorie"), rs.getString("type"), rs.getBigDecimal("prix"),
                                rs.getString("ville"), rs.getString("statut"), rs.getTimestamp("date_ajout"));
                        try { eq.setCaracteristiques(rs.getString("caracteristiques")); } catch (Exception ignored) {}
                        list.add(eq);
                    }
                } catch (SQLException ex2) {
                    System.out.println("❌ Erreur affichage équipements");
                    ex2.printStackTrace();
                }
            } else if (e.getMessage() != null && e.getMessage().contains("caracteristiques")) {
                try {
                    String sql2 = "SELECT id, nom, description, categorie, type, prix, ville, statut, date_ajout FROM equipement";
                    try (ResultSet rs2 = cnx.createStatement().executeQuery(sql2)) {
                        while (rs2.next()) {
                            list.add(new Equipement(rs2.getLong("id"), rs2.getString("nom"), rs2.getString("description"),
                                    rs2.getString("categorie"), rs2.getString("type"), rs2.getBigDecimal("prix"),
                                    rs2.getString("ville"), rs2.getString("statut"), rs2.getTimestamp("date_ajout")));
                        }
                    }
                } catch (SQLException ex2) {
                    System.out.println("❌ Erreur affichage équipements");
                    ex2.printStackTrace();
                }
            } else {
                System.out.println("❌ Erreur affichage équipements");
                e.printStackTrace();
            }
        }

        // ✅ Stream: tri par date_ajout DESC (plus récent en premier)
        return list.stream()
                .sorted((a, b) -> {
                    Timestamp da = a.getDateAjout();
                    Timestamp db = b.getDateAjout();
                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return db.compareTo(da);
                })
                .collect(Collectors.toList());
    }

    // READ BY ID (Stream)
    public Equipement getById(Long id) {
        return afficher().stream()
                .filter(e -> e.getId() != null && e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // UPDATE
    public void modifier(Equipement e) {
        String sql = "UPDATE equipement SET nom = ?, description = ?, categorie = ?, type = ?, prix = ?, ville = ?, statut = ?, caracteristiques = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, e.getNom());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getCategorie());
            ps.setString(4, e.getType());
            ps.setBigDecimal(5, e.getPrix());
            ps.setString(6, e.getVille());
            ps.setString(7, e.getStatut());
            ps.setString(8, e.getCaracteristiques());
            ps.setLong(9, e.getId());
            int updated = ps.executeUpdate();
            System.out.println(updated > 0 ? "✅ Équipement modifié !" : "⚠️ Aucun équipement trouvé");
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("caracteristiques")) {
                try {
                    String sql2 = "UPDATE equipement SET nom = ?, description = ?, categorie = ?, type = ?, prix = ?, ville = ?, statut = ? WHERE id = ?";
                    try (PreparedStatement ps2 = cnx.prepareStatement(sql2)) {
                        ps2.setString(1, e.getNom());
                        ps2.setString(2, e.getDescription());
                        ps2.setString(3, e.getCategorie());
                        ps2.setString(4, e.getType());
                        ps2.setBigDecimal(5, e.getPrix());
                        ps2.setString(6, e.getVille());
                        ps2.setString(7, e.getStatut());
                        ps2.setLong(8, e.getId());
                        ps2.executeUpdate();
                        System.out.println("✅ Équipement modifié !");
                    }
                } catch (SQLException ex2) {
                    System.out.println("❌ Erreur modification équipement");
                    ex2.printStackTrace();
                }
            } else {
                System.out.println("❌ Erreur modification équipement");
                ex.printStackTrace();
            }
        }
    }

    // DELETE
    public void supprimer(Long id) {
        String sql = "DELETE FROM equipement WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            int deleted = ps.executeUpdate();
            System.out.println(deleted > 0 ? "✅ Équipement supprimé !" : "⚠️ Aucun équipement trouvé");
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression équipement");
            e.printStackTrace();
        }
    }

    /** Incrémente le nombre de vues d'un équipement (pour statistique "équipement le plus affiché"). */
    public void incrementerVues(Long id) {
        String sql = "UPDATE equipement SET nombre_vues = COALESCE(nombre_vues, 0) + 1 WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() == null || (!e.getMessage().contains("nombre_vues") && !e.getMessage().contains("Unknown column")))
                System.out.println("❌ Erreur incrementerVues: " + e.getMessage());
        }
    }

    // ✅ Stream: Rechercher par nom (insensible à la casse)
    public List<Equipement> rechercherParNom(String mot) {
        String m = (mot == null) ? "" : mot.trim().toLowerCase();
        return afficher().stream()
                .filter(e -> e.getNom() != null && e.getNom().toLowerCase().contains(m))
                .collect(Collectors.toList());
    }

    // ✅ Stream: Rechercher par catégorie
    public List<Equipement> rechercherParCategorie(String categorie) {
        String c = (categorie == null) ? "" : categorie.trim().toLowerCase();
        return afficher().stream()
                .filter(e -> e.getCategorie() != null && e.getCategorie().trim().toLowerCase().equals(c))
                .collect(Collectors.toList());
    }

    // ✅ Stream: Rechercher par type (VENTE/LOCATION)
    public List<Equipement> rechercherParType(String type) {
        String t = (type == null) ? "" : type.trim().toUpperCase();
        return afficher().stream()
                .filter(e -> e.getType() != null && e.getType().trim().toUpperCase().equals(t))
                .collect(Collectors.toList());
    }

    // ✅ Stream: Rechercher par statut
    public List<Equipement> rechercherParStatut(String statut) {
        String s = (statut == null) ? "" : statut.trim().toUpperCase();
        return afficher().stream()
                .filter(e -> e.getStatut() != null && e.getStatut().trim().toUpperCase().equals(s))
                .collect(Collectors.toList());
    }

    // ✅ Stream: Rechercher par ville
    public List<Equipement> rechercherParVille(String ville) {
        String v = (ville == null) ? "" : ville.trim().toLowerCase();
        return afficher().stream()
                .filter(e -> e.getVille() != null && e.getVille().toLowerCase().contains(v))
                .collect(Collectors.toList());
    }

    // ✅ Stream: Recherche globale (nom, description, catégorie, ville)
    public List<Equipement> rechercherGlobal(String motCle) {
        String m = (motCle == null) ? "" : motCle.trim().toLowerCase();
        if (m.isEmpty()) return afficher();
        
        return afficher().stream()
                .filter(e -> {
                    boolean matchNom = e.getNom() != null && e.getNom().toLowerCase().contains(m);
                    boolean matchDesc = e.getDescription() != null && e.getDescription().toLowerCase().contains(m);
                    boolean matchCat = e.getCategorie() != null && e.getCategorie().toLowerCase().contains(m);
                    boolean matchVille = e.getVille() != null && e.getVille().toLowerCase().contains(m);
                    return matchNom || matchDesc || matchCat || matchVille;
                })
                .collect(Collectors.toList());
    }

    // ✅ Stream: Trier par prix (croissant)
    public List<Equipement> trierParPrixCroissant() {
        return afficher().stream()
                .sorted(Comparator.comparing(e -> e.getPrix() != null ? e.getPrix() : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    // ✅ Stream: Trier par prix (décroissant)
    public List<Equipement> trierParPrixDecroissant() {
        return afficher().stream()
                .sorted((a, b) -> {
                    BigDecimal pa = a.getPrix() != null ? a.getPrix() : BigDecimal.ZERO;
                    BigDecimal pb = b.getPrix() != null ? b.getPrix() : BigDecimal.ZERO;
                    return pb.compareTo(pa);
                })
                .collect(Collectors.toList());
    }

    // ✅ Stream: Trier par nom (alphabétique)
    public List<Equipement> trierParNom() {
        return afficher().stream()
                .sorted(Comparator.comparing(e -> e.getNom() != null ? e.getNom().toLowerCase() : ""))
                .collect(Collectors.toList());
    }

    // ✅ Équipements disponibles (exclut VENDU)
    public List<Equipement> afficherDisponibles() {
        return afficher().stream()
                .filter(e -> e.getStatut() == null || !e.getStatut().equals("VENDU"))
                .collect(Collectors.toList());
    }

    // ✅ Stream: Filtrer par prix (min, max)
    public List<Equipement> filtrerParPrix(BigDecimal min, BigDecimal max) {
        BigDecimal minVal = min != null ? min : BigDecimal.ZERO;
        BigDecimal maxVal = max != null ? max : new BigDecimal("999999999");
        
        return afficher().stream()
                .filter(e -> {
                    BigDecimal prix = e.getPrix() != null ? e.getPrix() : BigDecimal.ZERO;
                    return prix.compareTo(minVal) >= 0 && prix.compareTo(maxVal) <= 0;
                })
                .collect(Collectors.toList());
    }
}
