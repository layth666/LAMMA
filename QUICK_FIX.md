# 🚀 FIX BASE DE DONNÉES - 3 ÉTAPES

## ✅ CODE FIXÉ + BASE À CORRIGER

Le code Java est **corrigé** (sauvegarde en chemins relatifs maintenant).

La base de données doit être **nettoyée**.

---

## 3 ÉTAPES SUPER SIMPLES

### **1️⃣ Corriger la base (5 min)**

**Ouvrir PhpMyAdmin :**
```
http://localhost:3307
```

**Aller à SQL :**
```
Gestion_Equipement → SQL
```

**Copier-coller et exécuter :**
```sql
ALTER TABLE message_chat 
ADD COLUMN IF NOT EXISTS type_message VARCHAR(50) DEFAULT 'TEXT',
ADD COLUMN IF NOT EXISTS fichier_path TEXT,
ADD COLUMN IF NOT EXISTS latitude DOUBLE,
ADD COLUMN IF NOT EXISTS longitude DOUBLE,
ADD COLUMN IF NOT EXISTS id_user INT DEFAULT 1;

UPDATE message_chat 
SET fichier_path = SUBSTRING(fichier_path, POSITION('uploads' IN fichier_path))
WHERE (type_message IN ('GIF', 'AUDIO') OR type_message = 'IMAGE') 
  AND fichier_path LIKE '%uploads%'
  AND POSITION('uploads' IN fichier_path) > 1;

UPDATE message_chat 
SET fichier_path = REPLACE(fichier_path, '\\', '/')
WHERE fichier_path IS NOT NULL;
```

### **2️⃣ Compiler (1 min)**
```
Ctrl+Shift+F9
```

### **3️⃣ Tester (2 min)**
```
Shift+F10 → Sélectionnez groupe → 🎁🎤 → ✅ Ça marche !
```

---

## 📊 RÉSULTAT

```
GIFs ............ ✅ Visibles
Vocaux ......... ✅ Visibles
Chemins ....... ✅ Corrects
```

---

**C'est fini ! 🎉**

