package Services;

import entities.MessageChat;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageChatService {

    private final Connection cnx;

    public MessageChatService() {
        cnx = MyDataBase.getInstance().getCnx();
    }

    // CREATE
    public void ajouter(MessageChat m) {
        String sql = "INSERT INTO message_chat (contenu, id_groupe, type_message, fichier_path, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, m.getContenu() != null ? m.getContenu() : "");
            ps.setInt(2, m.getIdGroupe());
            ps.setString(3, m.getTypeMessage() != null ? m.getTypeMessage() : "TEXT");
            ps.setString(4, m.getFichierPath());
            ps.setObject(5, m.getLatitude());
            ps.setObject(6, m.getLongitude());
            ps.executeUpdate();
            System.out.println("✅ Message ajouté !");
        } catch (SQLException e) {
            if (e.getMessage() != null && (e.getMessage().contains("type_message") || e.getMessage().contains("Unknown column"))) {
                try (PreparedStatement ps = cnx.prepareStatement("INSERT INTO message_chat (contenu, id_groupe) VALUES (?, ?)")) {
                    ps.setString(1, m.getContenu() != null ? m.getContenu() : "");
                    ps.setInt(2, m.getIdGroupe());
                    ps.executeUpdate();
                    System.out.println("✅ Message ajouté !");
                } catch (SQLException ex2) {
                    System.out.println("❌ Erreur ajout message");
                    ex2.printStackTrace();
                }
            } else {
                System.out.println("❌ Erreur ajout message");
                e.printStackTrace();
            }
        }
    }

    // READ ALL (DB -> List -> Stream)
    public List<MessageChat> afficher() {
        List<MessageChat> list = new ArrayList<>();
        String sql = "SELECT id, contenu, date_envoi, id_groupe, type_message, fichier_path, latitude, longitude FROM message_chat";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                MessageChat msg = new MessageChat(rs.getInt("id"), rs.getString("contenu"), rs.getTimestamp("date_envoi"), rs.getInt("id_groupe"));
                try { msg.setTypeMessage(rs.getString("type_message")); } catch (Exception ignored) {}
                try { msg.setFichierPath(rs.getString("fichier_path")); } catch (Exception ignored) {}
                try { msg.setLatitude(rs.getDouble("latitude")); if (rs.wasNull()) msg.setLatitude(null); } catch (Exception ignored) {}
                try { msg.setLongitude(rs.getDouble("longitude")); if (rs.wasNull()) msg.setLongitude(null); } catch (Exception ignored) {}
                list.add(msg);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && (e.getMessage().contains("type_message") || e.getMessage().contains("Unknown column"))) {
                try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery("SELECT id, contenu, date_envoi, id_groupe FROM message_chat")) {
                    while (rs.next()) list.add(new MessageChat(rs.getInt("id"), rs.getString("contenu"), rs.getTimestamp("date_envoi"), rs.getInt("id_groupe")));
                } catch (SQLException ex2) {
                    System.out.println("❌ Erreur affichage messages");
                    ex2.printStackTrace();
                }
            } else {
                System.out.println("❌ Erreur affichage messages");
                e.printStackTrace();
            }
        }

        // ✅ Stream: tri DESC par date_envoi
        return list.stream()
                .sorted((a, b) -> {
                    Timestamp da = a.getDateEnvoi();
                    Timestamp db = b.getDateEnvoi();
                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return db.compareTo(da);
                })
                .toList();
    }

    // READ BY ID (Stream)
    public MessageChat getById(int id) {
        return afficher().stream()
                .filter(m -> m.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // READ messages d’un groupe (Stream)
    public List<MessageChat> afficherParGroupe(int idGroupe) {
        // ✅ Stream: filter + tri ASC (style chat)
        return afficher().stream()
                .filter(m -> m.getIdGroupe() == idGroupe)
                .sorted((a, b) -> {
                    Timestamp da = a.getDateEnvoi();
                    Timestamp db = b.getDateEnvoi();
                    if (da == null && db == null) return 0;
                    if (da == null) return -1;
                    if (db == null) return 1;
                    return da.compareTo(db);
                })
                .toList();
    }

    // UPDATE
    public void modifier(MessageChat m) {
        String sql = "UPDATE message_chat SET contenu = ?, type_message = ?, fichier_path = ?, latitude = ?, longitude = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, m.getContenu());
            ps.setString(2, m.getTypeMessage() != null ? m.getTypeMessage() : "TEXT");
            ps.setString(3, m.getFichierPath());
            ps.setObject(4, m.getLatitude());
            ps.setObject(5, m.getLongitude());
            ps.setInt(6, m.getId());
            int updated = ps.executeUpdate();
            System.out.println(updated > 0 ? "✅ Message modifié !" : "⚠️ Aucun message trouvé");
        } catch (SQLException e) {
            try (PreparedStatement ps = cnx.prepareStatement("UPDATE message_chat SET contenu = ? WHERE id = ?")) {
                ps.setString(1, m.getContenu());
                ps.setInt(2, m.getId());
                ps.executeUpdate();
            } catch (SQLException ex2) {
                System.out.println("❌ Erreur modification message");
                ex2.printStackTrace();
            }
        }
    }

    // DELETE
    public void supprimer(int id) {
        String sql = "DELETE FROM message_chat WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            System.out.println(deleted > 0 ? "✅ Message supprimé !" : "⚠️ Aucun message trouvé");
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression message");
            e.printStackTrace();
        }
    }

    // BONUS Stream: rechercher un mot dans les messages d’un groupe
    public List<MessageChat> rechercherDansGroupe(int idGroupe, String mot) {
        String m = (mot == null) ? "" : mot.toLowerCase();
        return afficherParGroupe(idGroupe).stream()
                .filter(msg -> msg.getContenu() != null && msg.getContenu().toLowerCase().contains(m))
                .toList();
    }
}