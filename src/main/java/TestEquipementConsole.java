import entities.Equipement;
import Services.EquipementService;

import java.math.BigDecimal;

public class TestEquipementConsole {

    public static void main(String[] args) {

        EquipementService equipementService = new EquipementService();

        System.out.println("===== TEST CONSOLE ÉQUIPEMENTS =====");

        // 1️⃣ CREATE ÉQUIPEMENT
        Equipement e1 = new Equipement(
            "Tente 4 places",
            "Tente spacieuse pour 4 personnes, imperméable",
            "Camping",
            "LOCATION",
            new BigDecimal("50.00"),
            "Tunis",
            "DISPONIBLE"
        );
        equipementService.ajouter(e1);

        Equipement e2 = new Equipement(
            "Vélo de route",
            "Vélo de route professionnel, taille M",
            "Sport",
            "VENTE",
            new BigDecimal("350.00"),
            "Sfax",
            "DISPONIBLE"
        );
        equipementService.ajouter(e2);

        // 2️⃣ AFFICHER TOUS LES ÉQUIPEMENTS
        System.out.println("\n📌 Liste de tous les équipements :");
        equipementService.afficher().forEach(System.out::println);

        // 3️⃣ RECHERCHER PAR NOM
        System.out.println("\n🔍 Recherche par nom 'Tente' :");
        equipementService.rechercherParNom("Tente").forEach(System.out::println);

        // 4️⃣ RECHERCHER PAR TYPE
        System.out.println("\n🔍 Recherche par type 'LOCATION' :");
        equipementService.rechercherParType("LOCATION").forEach(System.out::println);

        // 5️⃣ RECHERCHER PAR CATÉGORIE
        System.out.println("\n🔍 Recherche par catégorie 'Camping' :");
        equipementService.rechercherParCategorie("Camping").forEach(System.out::println);

        // 6️⃣ TRIER PAR PRIX CROISSANT
        System.out.println("\n📊 Tri par prix croissant :");
        equipementService.trierParPrixCroissant().forEach(e -> 
            System.out.println(e.getNom() + " - " + e.getPrix() + " €")
        );

        // 7️⃣ RECHERCHE GLOBALE
        System.out.println("\n🔍 Recherche globale 'Vélo' :");
        equipementService.rechercherGlobal("Vélo").forEach(System.out::println);

        // 8️⃣ GET BY ID
        System.out.println("\n📌 Équipement par ID (exemple ID=1) :");
        Equipement equipement = equipementService.getById(1L);
        if (equipement != null) {
            System.out.println(equipement);
        } else {
            System.out.println("Aucun équipement trouvé avec cet ID");
        }

        // 9️⃣ UPDATE (exemple avec ID=1)
        Equipement equipementToUpdate = equipementService.getById(1L);
        if (equipementToUpdate != null) {
            System.out.println("\n✏️ Modification de l'équipement ID=1 :");
            equipementToUpdate.setPrix(new BigDecimal("45.00"));
            equipementToUpdate.setStatut("LOUE");
            equipementService.modifier(equipementToUpdate);
            System.out.println("✅ Équipement modifié !");
        }

        // 🔟 FILTRER PAR PRIX
        System.out.println("\n💰 Filtrage par prix (entre 40 et 100 €) :");
        equipementService.filtrerParPrix(
            new BigDecimal("40.00"),
            new BigDecimal("100.00")
        ).forEach(e -> System.out.println(e.getNom() + " - " + e.getPrix() + " €"));

        // 1️⃣1️⃣ AFFICHER APRÈS MODIFICATIONS
        System.out.println("\n📌 Liste finale des équipements :");
        equipementService.afficher().forEach(System.out::println);

        // 1️⃣2️⃣ DELETE (optionnel - décommenter pour tester)
        // System.out.println("\n🗑️ Suppression de l'équipement ID=2 :");
        // equipementService.supprimer(2L);
        // System.out.println("✅ Équipement supprimé !");

        System.out.println("\n===== FIN TEST =====");
    }
}
