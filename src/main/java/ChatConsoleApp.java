import entities.MessageChat;
import Services.MessageChatService;

import java.util.Scanner;

public class ChatConsoleApp {

    public static void main(String[] args) {

        MessageChatService messageService = new MessageChatService();
        Scanner sc = new Scanner(System.in);

        System.out.println("===== CHAT CONSOLE (CRUD Messages) =====");

        // L'utilisateur choisit le groupe dans lequel il envoie les messages
        System.out.print("Donne l'ID du groupe (id_groupe) existant : ");
        int idGroupe = Integer.parseInt(sc.nextLine().trim());

        while (true) {
            System.out.println("\n--- MENU ---");
            System.out.println("1) Envoyer un message (CREATE)");
            System.out.println("2) Afficher messages du groupe (READ)");
            System.out.println("3) Modifier un message (UPDATE)");
            System.out.println("4) Supprimer un message (DELETE)");
            System.out.println("0) Quitter");
            System.out.print("Choix: ");

            String choix = sc.nextLine().trim();

            switch (choix) {

                case "1" -> {
                    System.out.print("Tape ton message: ");
                    String contenu = sc.nextLine();

                    if (contenu.isBlank()) {
                        System.out.println("⚠️ Message vide, annulé.");
                        break;
                    }

                    MessageChat m = new MessageChat(contenu, idGroupe);
                    messageService.ajouter(m);
                }

                case "2" -> {
                    System.out.println("\n💬 Messages du groupe " + idGroupe + " :");
                    var msgs = messageService.afficherParGroupe(idGroupe);

                    if (msgs.isEmpty()) {
                        System.out.println("(Aucun message)");
                    } else {
                        msgs.forEach(System.out::println);
                    }
                }

                case "3" -> {
                    System.out.print("Donne l'ID du message à modifier: ");
                    int idMsg = Integer.parseInt(sc.nextLine().trim());

                    MessageChat msg = messageService.getById(idMsg);
                    if (msg == null) {
                        System.out.println("❌ Message introuvable !");
                        break;
                    }

                    System.out.println("Message actuel: " + msg.getContenu());
                    System.out.print("Nouveau contenu: ");
                    String newContenu = sc.nextLine();

                    if (newContenu.isBlank()) {
                        System.out.println("⚠️ Nouveau contenu vide, annulé.");
                        break;
                    }

                    msg.setContenu(newContenu);
                    messageService.modifier(msg);
                }

                case "4" -> {
                    System.out.print("Donne l'ID du message à supprimer: ");
                    int idMsg = Integer.parseInt(sc.nextLine().trim());
                    messageService.supprimer(idMsg);
                }

                case "0" -> {
                    System.out.println("👋 Bye!");
                    sc.close();
                    return;
                }

                default -> System.out.println("⚠️ Choix invalide.");
            }
        }
    }
}