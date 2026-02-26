-- Migration complète Backend Admin Dashboard
-- Exécuter chaque bloc une seule fois (ignorer les erreurs "column/table exists").

-- 1) Équipement : nombre de vues (pour statistique "équipement le plus affiché")
ALTER TABLE equipement ADD COLUMN nombre_vues INT DEFAULT 0;

-- 2) Messages : type, fichier, localisation
ALTER TABLE message_chat ADD COLUMN type_message VARCHAR(50) DEFAULT 'TEXT';
ALTER TABLE message_chat ADD COLUMN fichier_path TEXT;
ALTER TABLE message_chat ADD COLUMN latitude DOUBLE;
ALTER TABLE message_chat ADD COLUMN longitude DOUBLE;

-- 3) Table attributs spécifiques par type d'équipement (option 2 scalable)
CREATE TABLE IF NOT EXISTS equipement_attributs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    equipement_id BIGINT NOT NULL,
    nom_attribut VARCHAR(100) NOT NULL,
    valeur VARCHAR(255),
    FOREIGN KEY (equipement_id) REFERENCES equipement(id) ON DELETE CASCADE
);

-- 4) Table mots interdits (anti-gros mots)
CREATE TABLE IF NOT EXISTS banned_words (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mot VARCHAR(100) NOT NULL UNIQUE
);

-- Insérer quelques mots interdits par défaut (optionnel)
INSERT IGNORE INTO banned_words (mot) VALUES ('merde'), ('putain'), ('connard'), ('idiot');
