import entities.GroupeChat;
import entities.MessageChat;
import Services.GroupeChatService;
import Services.MessageChatService;

public class TestConsole {

    public static void main(String[] args) {

        GroupeChatService groupeService = new GroupeChatService();
        MessageChatService messageService = new MessageChatService();

        System.out.println("===== TEST CONSOLE CHAT =====");

        // 1️⃣ CREATE GROUPE
        GroupeChat g = new GroupeChat("Console Group", "Test depuis console", "PUBLIC" , 1);
        groupeService.ajouter(g);

        // 2️⃣ AFFICHER GROUPES
        System.out.println("\n📌 Liste des groupes :");
        groupeService.afficher().forEach(System.out::println);

        // ⚠️ Mets un id existant ici
        int idGroupe = 1;

        // 3️⃣ AJOUT MESSAGE
        MessageChat m1 = new MessageChat("Salut depuis la console 👋", idGroupe, 1);
        messageService.ajouter(m1);

        // 4️⃣ AFFICHER MESSAGES DU GROUPE
        System.out.println("\n💬 Messages du groupe " + idGroupe + " :");
        messageService.afficherParGroupe(idGroupe).forEach(System.out::println);

        // 5️⃣ UPDATE MESSAGE (exemple id=1)
        MessageChat msg = messageService.getById(1);
        if (msg != null) {
            msg.setContenu("Message modifié depuis console ✅");
            messageService.modifier(msg);
        }

        // 6️⃣ DELETE MESSAGE (optionnel)
        // messageService.supprimer(1);

        System.out.println("\n===== FIN TEST =====");
    }
}