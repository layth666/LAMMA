-- Script SQL de création table utilisateur et insertion par défaut
CREATE TABLE IF NOT EXISTS utilisateur (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'USER'
);

-- Insertion de l'Admin principal (vous pouvez changer le mot de passe après)
INSERT INTO utilisateur (nom, email, password, role) 
VALUES ('Alexandre (Admin)', 'admin@lamma.com', 'admin123', 'ADMIN');

-- Insertion d'un Utilisateur de test régulier
INSERT INTO utilisateur (nom, email, password, role) 
VALUES ('Utilisateur Test', 'user@lamma.com', 'user123', 'USER');

-- Si la table existe déjà, ajoutez simplement la colonne
-- ALTER TABLE utilisateur ADD COLUMN role VARCHAR(50) DEFAULT 'USER';
-- UPDATE utilisateur SET role = 'ADMIN' WHERE email = 'admin@lamma.com';
