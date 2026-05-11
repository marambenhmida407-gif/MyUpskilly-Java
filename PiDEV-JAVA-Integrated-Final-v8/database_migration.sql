-- ============================================================
-- MIGRATION — Ajout des colonnes Maraam à la table users
-- Exécuter UNE SEULE FOIS sur votre base esprit existante
-- Compatible MySQL 5.7+ et MySQL 8.x
-- ============================================================

USE esprit;

-- username (optionnel, affiché dans le profil)
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(255) AFTER prenom;

-- photo (chemin vers la photo de profil)
ALTER TABLE users ADD COLUMN IF NOT EXISTS photo VARCHAR(255) AFTER username;

-- is_verified (compte vérifié par admin ou email)
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE AFTER photo;

-- etat (TRUE = actif, FALSE = banni/désactivé)
ALTER TABLE users ADD COLUMN IF NOT EXISTS etat BOOLEAN DEFAULT TRUE AFTER is_verified;

-- grade (ex: Professeur, Dr, Interne...)
ALTER TABLE users ADD COLUMN IF NOT EXISTS grade VARCHAR(100) AFTER etat;

-- adresse
ALTER TABLE users ADD COLUMN IF NOT EXISTS adresse VARCHAR(255) AFTER grade;

-- description_specialite (bio longue du médecin)
ALTER TABLE users ADD COLUMN IF NOT EXISTS description_specialite TEXT AFTER adresse;

-- google_authenticator_secret (null = pas de 2FA, sinon clé TOTP)
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_authenticator_secret VARCHAR(255) AFTER description_specialite;

-- Mettre etat = TRUE pour les utilisateurs existants (migration propre)
UPDATE users SET etat = TRUE WHERE etat IS NULL;

-- ============================================================
-- VÉRIFICATION — doit lister toutes les nouvelles colonnes
-- ============================================================
DESCRIBE users;
