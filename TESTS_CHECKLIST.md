## 🧪 Tests - Checklist de Vérification

Voici comment tester toutes les nouvelles fonctionnalités :

---

### ✅ **TEST 1 : Visibilité des Couleurs**

**But** : Vérifier que tous les textes sont lisibles.

1. Lancez l'app
2. Ouvrez un groupe de chat
3. Vérifiez que vous voyez :
   - ✅ Nom du groupe (blanc)
   - ✅ Description du groupe (blanc léger)
   - ✅ Messages reçus (texte blanc)
   - ✅ Messages envoyés (texte blanc)
   - ✅ Timestamps (gris léger)
   - ✅ Tous les emojis (👍 ❤️ 😂 etc)

**Critères d'acceptation** :
- ✅ 100% lisible à vue
- ✅ Pas de glyphes emoji manquants
- ✅ Contraste ≥ 4.5:1 (AA WCAG)

---

### ✅ **TEST 2 : Réactions Emoji**

**But** : Tester ajout/retrait réactions.

**Scénario 1 : Ajouter réaction**
1. Ouvrez un groupe
2. Envoyez un message : "Test emoji"
3. Cliquez sur 😊 en bas du message
4. Sélectionnez 👍
5. ✅ 👍 1 apparaît sous le message

**Scénario 2 : Ajouter 2e emoji**
1. Cliquez 😊 à nouveau
2. Sélectionnez ❤️
3. ✅ Affiche : 👍 1  ❤️ 1

**Scénario 3 : Toggle (retirer)**
1. Cliquez sur 👍 1
2. ✅ Le 👍 disparaît
3. ✅ Affiche : ❤️ 1

**Scénario 4 : Plusieurs utilisateurs**
1. (Demander à ami de tester aussi)
2. Ami ajoute 👍
3. ✅ Affiche : 👍 2

**Critères d'acceptation** :
- ✅ Réactions sauvegardées en BD
- ✅ Compteur correct
- ✅ Toggle fonctionne
- ✅ Multi-utilisateurs OK

---

### ✅ **TEST 3 : Images - Affichage Inline**

**But** : Vérifier images s'affichent directement (pas "pièce jointe").

1. Ouvrez groupe
2. Cliquez 📎 (trombone)
3. Sélectionnez image locale (PNG/JPG)
4. ✅ Image s'affiche directement dans chat
5. ✅ Pas de label "(pièce jointe)"
6. ✅ Cliquez image → Agrandisse
7. ✅ Bouton "Ouvrir" disponible

**Tailles à tester** :
- ✅ 500x500px
- ✅ 1920x1080px
- ✅ 100x100px (petit)

**Critères d'acceptation** :
- ✅ Preview 320x240px dans chat
- ✅ Ratio conservé
- ✅ Clic sur image agrandit
- ✅ Pas d'erreur

---

### ✅ **TEST 4 : Sondages**

**But** : Vérifier sondages fonctionnels.

**Créer sondage** :
1. Cliquez 📊
2. Question : "Quel jour ?"
3. Option 1 : "Lundi"
4. Option 2 : "Mardi"
5. Option 3 : "Mercredi"
6. Créez
7. ✅ Sondage apparaît

**Voter** :
1. Cliquez "Lundi"
2. ✅ Bouton se disable (grisé)
3. ✅ Affiche : "Lundi" = 1 (100%)
4. ✅ Bouton est vert (voted)
5. Essayez cliquer "Mardi"
6. ✅ Impossible (disabled)

**Critères d'acceptation** :
- ✅ 1 seul vote par utilisateur
- ✅ Barre progression visible
- ✅ Pourcentage correct
- ✅ Bouton voté = vert

---

### ✅ **TEST 5 : Vocal (Placeholder)**

**But** : Vérifier bouton existe et dialog fonctionne.

1. Cliquez 🎤
2. ✅ Dialog s'ouvre
3. ✅ Message : "Fonctionnalité en cours..."
4. ✅ Fermer dialog OK

**Critères d'acceptation** :
- ✅ Bouton présent
- ✅ Dialog s'ouvre
- ✅ Pas d'erreur

---

### ✅ **TEST 6 : GIFs (Placeholder)**

**But** : Vérifier bouton existe et dialog fonctionne.

1. Cliquez 🎁
2. ✅ Dialog s'ouvre
3. ✅ Message : "Fonctionnalité en cours..."
4. ✅ Fermer dialog OK

**Critères d'acceptation** :
- ✅ Bouton présent
- ✅ Dialog s'ouvre
- ✅ Pas d'erreur

---

### ✅ **TEST 7 : Performance**

**But** : Vérifier app reste fluide.

**Chargement messages** :
1. Ouvrez groupe avec 100+ messages
2. ✅ Défilement fluide
3. ✅ Pas de freeze

**Réactions massives** :
1. Envoyez message
2. Ajoutez 10 réactions différentes
3. ✅ Affichage rapide (<500ms)

**Critères d'acceptation** :
- ✅ FPS ≥ 30 en défilement
- ✅ Réactions chargent <1sec
- ✅ Pas de lag

---

### ✅ **TEST 8 : Cross-Browser/OS**

Test sur :
- Windows 10/11 ✅
- Linux (Ubuntu) ✅
- macOS ✅

**À vérifier** :
- ✅ Emojis affichés
- ✅ Couleurs cohérentes
- ✅ Boutons cliquables
- ✅ Pas d'artifact affichage

---

## 🐛 Troubleshooting

### ❌ Les réactions ne sauvegardent pas
**Solution** :
1. Vérifiez table `message_reactions` existe
   ```sql
   SHOW TABLES LIKE '%reaction%';
   ```
2. Si absent, redémarrez app (créé auto)
3. Vérifiez BD est en lecture/écriture

### ❌ Emojis ne s'affichent pas
**Solution** :
1. Vérifiez police système supporte emoji
2. Mettez à jour JDK
3. Testez sur Windows 11 (meilleur support native)

### ❌ Images ne s'affichent pas inline
**Solution** :
1. Vérifiez dossier `uploads/` existe
2. Vérifiez permissions lecture/écriture
3. Vérifiez format image : PNG/JPG/GIF
4. Vérifiez chemin enregistré en BD

---

## 📊 Rapport de Test

**Template** :

```
Test Date: [DATE]
Tester: [NOM]
OS: [Windows/Linux/macOS]
Java Version: [1.8/11/17/21]

✅ Visuels & Couleurs
✅ Réactions Emoji
✅ Images Inline
✅ Sondages
⚠️ Vocal (Framework)
⚠️ GIFs (Framework)
✅ Performance
✅ Cross-OS

Issues: [Liste bugs trouvés]
Notes: [Observations]

Status: [PASS/FAIL]
```

---

**Bon testing ! 🚀**

