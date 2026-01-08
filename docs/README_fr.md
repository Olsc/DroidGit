# DroidGit 📱💻

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DroidGit** est un puissant serveur Git pour Android. Il transforme votre appareil mobile en un serveur d'hébergement Git entièrement fonctionnel, vous permettant de gérer les dépôts de code, les utilisateurs et les permissions directement depuis votre téléphone ou tablette.

Accessible via **HTTP**, DroidGit facilite le partage de code et la collaboration en déplacement. (Note : Actuellement seul le protocole HTTP est pris en charge, le support SSH pourrait être ajouté dans les versions futures)

[English](https://github.com/Olsc/DroidGit/blob/main/README.md) | [中文](https://github.com/Olsc/DroidGit/blob/main/docs/README_zh.md) | [Español](https://github.com/Olsc/DroidGit/blob/main/docs/README_es.md) | [Français](https://github.com/Olsc/DroidGit/blob/main/docs/README_fr.md) | [日本語](https://github.com/Olsc/DroidGit/blob/main/docs/README_ja.md) | [한국어](https://github.com/Olsc/DroidGit/blob/main/docs/README_ko.md) | [Русский](https://github.com/Olsc/DroidGit/blob/main/docs/README_ru.md)

![DroidGit Preview](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_1.jpg)

![DroidGit Screenshot](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_2.jpg)

---

## ✨ Fonctionnalités Clés

### 🚀 Fonctionnalités de Base
- **Support du Protocole HTTP** : Hébergez des dépôts Git en utilisant le protocole **Smart HTTP**.
- **Opérations Git** : Support complet des commandes Git standard : `clone`, `push`, `pull`, `fetch`.
- **Gestion des Utilisateurs** : Créez plusieurs utilisateurs avec des mots de passe et des permissions dédiés.
- **Gestion des Dépôts** : Créez, supprimez et modifiez des dépôts via l'application Android ou la console Web.

### 🌐 Console de Gestion Web
- **Interface Web** : Gérez votre serveur depuis n'importe quel navigateur (PC/Mobile) à l'adresse `http://<ip-appareil>:<port>/`.
- **Navigateur de Dépôts** : Parcourez les fichiers, les répertoires et l'historique des commits visuellement.
- **Rendu Markdown** : Affiche automatiquement les fichiers `README.md` pour la documentation du projet.
- **Coloration Syntaxique** : Visualisez le code avec une belle coloration syntaxique.
- **Aperçu des Fichiers** : Prévisualisez des images, des vidéos, de l'audio et des fichiers texte directement dans le navigateur.
- **Modifications Rapides** : Mettez à jour les descriptions et les paramètres des dépôts depuis l'interface Web.

### 🛠 Intégration Système
- **Gestion des Services** : S'exécute en tant que service de fond.
- **Connectivité WiFi** : Démarrage/arrêt automatique basé sur la connectivité WiFi.

---

## 📖 Guide de Démarrage Rapide

### 1. Démarrer le Serveur
1.  Ouvrez DroidGit sur votre appareil Android.
2.  Appuyez sur l'icône du bouton d'alimentation pour **Démarrer** le serveur.
3.  Notez l'adresse IP et le port affichés (ex., `192.168.1.5:8080`).

### 2. Accéder à la Console Web
1.  Sur votre ordinateur, ouvrez un navigateur.
2.  Accédez à l'adresse `http://<ip-appareil>:<port>/` (ex., `http://192.168.1.5:8080/`).
3.  Vous verrez la **Console Web DroidGit**.

### 3. Créer un Dépôt
1.  Dans la console Web, cliquez sur **New Repository**.
2.  Entrez un nom (ex., `mon-projet`) et une description.
3.  Cliquez sur **Create**.

### 4. Cloner et Pousser (Clone & Push)
Vous pouvez maintenant utiliser Git sur votre ordinateur pour interagir avec le dépôt.

**Via HTTP :**
```bash
# Cloner le dépôt vide
git clone http://<ip-appareil>:<port>/mon-projet.git

# Ou ajouter comme distant à un projet existant
cd mon-projet
git init
git remote add origin http://<ip-appareil>:<port>/mon-projet.git
git add .
git commit -m "Premier commit"
git push -u origin master
```

---

## 🛡️ Licence & EULA

### Licence Open Source
Ce projet est sous licence **Apache License, Version 2.0**. Vous êtes libre d'utiliser, de modifier et de distribuer ce logiciel selon les termes de la licence Apache. Voir [LICENSE](../LICENSE) pour plus de détails.

### Contrat de Licence Utilisateur Final (EULA)
DroidGit adhère aux principes internationaux de **Paix, Respect et Égalité**.
En utilisant ce logiciel, vous acceptez de :
- L'utiliser conformément aux lois locales et aux normes internationales de conduite sur Internet.
- **NE PAS** l'utiliser pour des discours de haine, de la discrimination, de la violence ou la promotion de contenus illégaux (CSAM, terrorisme, etc.).
- Respecter la vie privée et les droits de propriété intellectuelle.

---

## 🏗 Architecture
- **Cœur du Serveur** : NanoHTTPD (HTTP)
- **Moteur Git** : Eclipse JGit.
- **Base de Données** : ORMLite (SQLite).

---
