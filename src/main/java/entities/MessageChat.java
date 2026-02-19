package entities;

import java.sql.Timestamp;

public class MessageChat {
    private int id;
    private String contenu;
    private Timestamp dateEnvoi;
    private int idGroupe;

    public MessageChat() {}

    // Pour INSERT
    public MessageChat(String contenu, int idGroupe) {
        this.contenu = contenu;
        this.idGroupe = idGroupe;
    }

    // Pour SELECT
    public MessageChat(int id, String contenu, Timestamp dateEnvoi, int idGroupe) {
        this.id = id;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.idGroupe = idGroupe;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public Timestamp getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(Timestamp dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public int getIdGroupe() { return idGroupe; }
    public void setIdGroupe(int idGroupe) { this.idGroupe = idGroupe; }

    @Override
    public String toString() {
        return "MessageChat{" +
                "id=" + id +
                ", contenu='" + contenu + '\'' +
                ", dateEnvoi=" + dateEnvoi +
                ", idGroupe=" + idGroupe +
                '}';
    }
}