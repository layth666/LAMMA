# 🚀 INSTRUCTIONS DE LANCEMENT

**Application** : Gestion Équipements + Messagerie Améliorée  
**Version** : v2.0-Enhanced-Messaging  
**Date** : 04 Mars 2026

---

## ✅ Prérequis

- ✅ Java JDK 11+ (testé sur JDK 21)
- ✅ JavaFX SDK 21+
- ✅ MariaDB/MySQL en cours d'exécution
- ✅ IntelliJ IDEA (ou IDE JavaFX compatible)

---

## 🎯 Étapes de Lancement

### 1️⃣ Vérifier la Base de Données

```sql
-- Vérifier que vous pouvez vous connecter
mysql -u root -h 127.0.0.1 -P 3307

-- Vérifier la BD existe
SHOW DATABASES LIKE 'Gestion_Equipement';

-- Sélectionner BD
USE Gestion_Equipement;

-- Vérifier tables principales
SHOW TABLES;
-- Doit afficher : utilisateur, equipement, groupe_chat, message_chat, sondage_options, etc.

-- Nouvelle table créée auto au démarrage :
SHOW TABLES LIKE '%reaction%';
-- Doit afficher : message_reactions (si vide = créé auto)
```

### 2️⃣ Ouvrir le Projet dans IntelliJ

1. **File** → **Open**
2. Naviguer vers : `C:\Users\asusm\IdeaProjects\Gestion_Equipements`
3. Cliquer **OK**
4. IntelliJ indexe le projet (2-3 minutes)

### 3️⃣ Configurer SDK JavaFX

1. **File** → **Project Structure** (Ctrl+Alt+Shift+S)
2. **Libraries** → **+** → **Java**
3. Pointer vers votre dossier **JavaFX SDK** (ex : `C:\javafx-sdk-21`)
4. **Apply** → **OK**

### 4️⃣ Compiler le Projet

**Option A : IntelliJ (Recommandé)**
```
Build → Build Project (Ctrl+F9)
```

Attendez le message :
```
Build completed successfully
```

**Option B : Maven (si configuré)**
```bash
mvn clean compile
mvn javafx:run
```

### 5️⃣ Lancer l'Application

**Option A : IntelliJ**
1. Localiser : `src/main/java/ChatConsoleApp.java`
2. Clic droit → **Run 'ChatConsoleApp.main()'**
3. OU : Appuyez sur **Shift+F10**

**Option B : Raccourci Clavier**
- **Alt+Shift+F10** → Sélectionner configuration
- **F9** → Run

**Output attendu** :
```
✅ Connexion MariaDB réussie
✅ Table message_reactions vérifiée
[Chat] Fenêtre s'ouvre...
```

---

## 🧪 Tests Rapides Post-Lancement

### Test 1 : Connexion (30 sec)
1. ✅ App s'ouvre
2. ✅ Page login
3. ✅ Login avec `admin / admin` (ou user créé)
4. ✅ Dashboard affiche

### Test 2 : Messagerie (2 min)
1. ✅ Cliquez **Messagerie** (gauche)
2. ✅ Sélectionnez un groupe
3. ✅ Voir messages anciens
4. ✅ Tapez "Test" + Envoyez
5. ✅ Message s'affiche

### Test 3 : Réactions (1 min)
1. ✅ Cliquez 😊 sous votre message
2. ✅ Menu emoji s'ouvre
3. ✅ Sélectionnez 👍
4. ✅ Emoji apparaît avec compteur

### Test 4 : Images (1 min)
1. ✅ Cliquez 📎
2. ✅ Sélectionnez image
3. ✅ Image s'affiche inline (pas "pièce jointe")

### Test 5 : Sondages (1 min)
1. ✅ Cliquez 📊
2. ✅ "Quel jour ?" → Lundi / Mardi
3. ✅ Votez
4. ✅ Barre progress + pourcentage

### Test 6 : Couleurs (30 sec)
1. ✅ Tous textes lisibles (blanc)
2. ✅ Noms groupes visibles
3. ✅ Emojis affichés (18-22px)
4. ✅ Pas de texte invisible

---

## 🔍 Debugging si Problème

### ❌ Emojis ne s'affichent pas
**Solution** :
```
1. Vérifiez JDK version : 11+ (de préférence 17+)
2. Vérifiez police système Windows : Segoe UI Symbol
3. Redémarrez l'IDE
4. Nettoyez cache : File → Invalidate Caches
```

### ❌ Réactions ne sauvegardent pas
**Solution** :
```
1. Vérifiez BD : mysql> SELECT * FROM message_reactions;
2. Vérifiez permissions : utilisateur MariaDB en read/write
3. Vérifiez table créée : SHOW TABLES LIKE '%reaction%';
4. Redémarrez app (table créée auto)
```

### ❌ Images ne s'affichent pas
**Solution** :
```
1. Vérifiez dossier : C:\Users\asusm\IdeaProjects\Gestion_Equipements\uploads\
2. Créez-le si absent : mkdir uploads
3. Vérifiez permissions : lire/écrire
4. Vérifiez format : PNG/JPG/GIF
```

### ❌ App ne démarre pas
**Solution** :
```
1. Vérifiez BD running : mysql -u root
2. Vérifiez JDK JavaFX : javafxrt sur classpath
3. Vérifiez pom.xml : <maven-compiler-plugin> version
4. Nettoyez : Clean → Build
5. Redémarrez IDE
```

### ❌ Console affiche erreur SQL
**Solution** :
```
1. Vérifiez connection string : MyDataBase.java ligne 14
2. Vérifiez BD existe : mysql> SHOW DATABASES;
3. Vérifiez utilisateur/password
4. Vérifiez port 3307 (ou configurez dans code)
```

---

## 📊 Architecture Fichiers Clés

```
Gestion_Equipements/
├── pom.xml                          ← Configuration Maven
├── src/main/java/
│   ├── ChatConsoleApp.java          ← POINT D'ENTRÉE
│   ├── TestConsole.java
│   ├── controllers/
│   │   ├── ChatController.java      ← RÉACTIONS + VOCAL + GIF
│   │   ├── EquipementStoreController.java
│   │   └── GifPickerController.java ← Stub GIF
│   ├── Services/
│   │   ├── MessageReactionService.java  ← NOUVEAU
│   │   ├── MessageChatService.java
│   │   └── OptionSondageService.java
│   └── entities/
│       ├── MessageChat.java
│       └── ...
├── src/main/resources/
│   ├── views/
│   │   ├── ChatView.fxml            ← MODIFIÉ (boutons 🎤🎁)
│   │   └── GifPickerView.fxml       ← NOUVEAU
│   └── css/
│       ├── chat-improved.css        ← NOUVEAU (styles)
│       └── admin.css
├── target/
│   └── classes/                     ← Fichiers compilés
├── uploads/                         ← Images uploadées
└── DOCUMENTATION/
    ├── DEPLOYMENT_SUMMARY.md
    ├── GUIDE_MESSAGERIE.md
    ├── VISUAL_SUMMARY.md
    └── TESTS_CHECKLIST.md
```

---

## 🎓 Noms de Classe Importants

| Classe | Rôle |
|--------|------|
| **ChatConsoleApp** | Point d'entrée (main) |
| **ChatController** | Logique messagerie + réactions |
| **MessageReactionService** | Gestion réactions BD |
| **EquipementStoreController** | Logique boutique |
| **MessageChatService** | CRUD messages |
| **OptionSondageService** | Gestion sondages |

---

## 🔧 Configuration Important

### MyDataBase.java (Connexion BD)
```java
// Ligne 14-16
private static final String URL = "jdbc:mariadb://127.0.0.1:3307/Gestion_Equipement";
private static final String USER = "root";
private static final String PASSWORD = "";

// ADAPTER SI NÉCESSAIRE :
// - PORT : 3307 → 3306 (ou votre port)
// - USER : root → votre utilisateur
// - PASSWORD : "" → votre mot de passe
```

### ChatView.fxml (Stylesheet)
```xml
<!-- Ligne 7 -->
stylesheets="@/css/chat-improved.css, @/css/admin.css"

<!-- IMPORTANT : chat-improved.css doit exister -->
<!-- Créé automatiquement lors du déploiement -->
```

---

## 📝 Commandes Utiles

### Vérifier BD Saine
```bash
mysql> USE Gestion_Equipement;
mysql> SELECT COUNT(*) FROM utilisateur;
mysql> SELECT COUNT(*) FROM message_chat;
mysql> SELECT COUNT(*) FROM message_reactions;
```

### Nettoyer Cache IntelliJ
```
File → Invalidate Caches... → Invalidate and Restart
```

### Reconstruire Projet
```
Build → Clean Project
Build → Build Project
```

### Afficher Console Erreur
```
View → Tool Windows → Run (ou Alt+4)
```

---

## ✅ Checklist Avant Lancement

- [ ] JDK 11+ installé
- [ ] JavaFX SDK configuré dans IDE
- [ ] MariaDB en cours d'exécution
- [ ] BD `Gestion_Equipement` créée
- [ ] Dossier `uploads/` existe
- [ ] Fichier `chat-improved.css` existe
- [ ] ChatView.fxml pointe vers `chat-improved.css`
- [ ] Pas d'erreurs IntelliJ (alt+6)
- [ ] Build réussi

---

## 🎯 Résultat Attendu

### Au Lancement
```
✅ Fenêtre JavaFX s'ouvre
✅ Page Login affiche
✅ Couleurs cohérentes (bleu/blanc)
✅ Console : ✅ Connexion MariaDB réussie
✅ Console : ✅ Table message_reactions vérifiée
```

### En Utilisation
```
✅ Messages affichés en bulles bleu/blanc
✅ Emoji 😊 bouton cliquable
✅ Menu emoji s'ouvre
✅ Réactions sauvegardées en BD
✅ Images s'affichent inline
✅ Sondages avec barres progress
✅ Vocal/GIF boutons présents (placeholder)
```

---

## 🚨 Points Critiques

1. **CSS** : `chat-improved.css` doit exister
2. **BD** : `message_reactions` créée auto au démarrage
3. **Emojis** : Nécessite JDK 17+ pour meilleur rendu
4. **Images** : Dossier `uploads/` avec permissions R/W
5. **Port** : MariaDB sur 3307 (adapter si différent)

---

## 📞 Aide Rapide

| Problème | Solution |
|----------|----------|
| App ne démarre | Vérifiez BD running + JDK |
| Réactions invisibles | Vérifiez couleurs CSS |
| Images ne s'uploadent | Vérifiez dossier `uploads/` |
| Emojis carrés | Mettez à jour JDK |
| Sondage ne sauvegarde | Vérifiez `sondage_options` table |

---

## 🎊 PRÊT À LANCER !

```
cd C:\Users\asusm\IdeaProjects\Gestion_Equipements
# Ouvrir dans IntelliJ IDEA
# Build → Build Project (Ctrl+F9)
# Shift+F10 pour lancer
```

**Bon lancement ! 🚀**

Tous les tests doivent PASSER. Si problème, consultez le TESTS_CHECKLIST.md.

