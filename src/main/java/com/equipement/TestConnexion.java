package com.equipement;

import entities.GroupeChat;
import Services.GroupeChatService;

public class TestConnexion {
    public static void main(String[] args) {

        GroupeChatService service = new GroupeChatService();

        // CREATE
        service.ajouter(new GroupeChat("Camping Lovers", "Groupe pour organiser sorties", "PUBLIC"));

        // READ
        System.out.println(service.afficher());

        // UPDATE (exemple)
        GroupeChat g = service.getById(1);
        if (g != null) {
            g.setNom("Camping Lovers Updated");
            service.modifier(g);
        }

        // DELETE (exemple)
        // service.supprimer(1);
    }
}