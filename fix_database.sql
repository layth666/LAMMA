-- ========================================
-- SCRIPT DE CORRECTION POUR GIFs ET VOCAUX
-- ========================================
-- Exécutez ce script dans phpmyadmin ou MySQL client

-- 1. S'assurer que la table message_chat existe avec toutes les colonnes nécessaires
ALTER TABLE message_chat
ADD COLUMN IF NOT EXISTS type_message VARCHAR(50) DEFAULT 'TEXT',
ADD COLUMN IF NOT EXISTS fichier_path TEXT,
ADD COLUMN IF NOT EXISTS latitude DOUBLE,
ADD COLUMN IF NOT EXISTS longitude DOUBLE,
ADD COLUMN IF NOT EXISTS id_user INT DEFAULT 1;

-- 2. Vérifier la structure actuelle
DESCRIBE message_chat;

-- 3. Voir les messages GIF/AUDIO existants
SELECT id, contenu, type_message, fichier_path
FROM message_chat
WHERE type_message IN ('GIF', 'AUDIO')
LIMIT 20;

-- 4. Convertir les chemins absolus en chemins relatifs (si besoin)
-- Pour les chemins qui contiennent "uploads/", les garder tels quels
-- Pour les chemins absolus (C:\Users\...\uploads\...), extraire la partie "uploads/..."
UPDATE message_chat
SET fichier_path = SUBSTRING(fichier_path, POSITION('uploads' IN fichier_path))
WHERE (type_message IN ('GIF', 'AUDIO') OR type_message = 'IMAGE')
  AND fichier_path LIKE '%uploads%'
  AND POSITION('uploads' IN fichier_path) > 1;

-- 5. Remplacer les backslashes par des slashes (Windows → Unix)
UPDATE message_chat
SET fichier_path = REPLACE(fichier_path, '\\', '/')
WHERE fichier_path IS NOT NULL;

-- 6. Vérifier après correction
SELECT id, contenu, type_message, fichier_path
FROM message_chat
WHERE type_message IN ('GIF', 'AUDIO', 'IMAGE')
LIMIT 20;

-- 7. Compter les messages GIF et AUDIO avec chemins valides
SELECT type_message, COUNT(*) as nombre
FROM message_chat
WHERE type_message IN ('GIF', 'AUDIO')
GROUP BY type_message;

-- 8. Supprimer les messages GIF/AUDIO avec chemin NULL ou vide (optionnel)
-- DELETE FROM message_chat
-- WHERE type_message IN ('GIF', 'AUDIO')
--   AND (fichier_path IS NULL OR fichier_path = '');


