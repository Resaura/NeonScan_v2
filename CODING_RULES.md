# Règles globales de développement (Codex)

Projet : NeonScan (et autres apps liées)  
Stack : Kotlin, Jetpack Compose, MVVM, Room

Ces règles sont **obligatoires** pour toute modification.

---

## 1. Modifications minimales

- Faire des modifications **locales et ciblées**.
- Ne **pas** réécrire un fichier entier sauf si c’est demandé explicitement, ou que tu juge qu'il est necéssaire de le faire pour une raison bien valable. 
- Ne **pas** supprimer de gros blocs de code “pour repartir de zéro” sans instruction claire.
- Ne **pas** reformater complètement un fichier juste pour changer la mise en forme.

---

## 2. Encodage et format

- Tous les fichiers doivent rester en **UTF-8**.
- Ne pas changer l’encodage, les fins de ligne ou ajouter/enlever de BOM.
- Ne pas créer de doublons du même fichier sans raison valable.

---

## 3. Fichiers existants

- Considérer que le code existant est important.
- Adapter ton travail à la structure déjà en place plutôt que tout refaire.
- Si une refonte est nécessaire, la faire **par petites étapes**, pas en effaçant tout d’un coup.

---

## 4. Suppression et réécritures

- Ne supprimer aucun fichier ou dossier complet sans demande explicite.
- Ne pas recréer un fichier complet “from scratch” s’il existe déjà, sauf si c’est clairement demandé.
- Toute suppression de **fichier ou dossier** doit :
  - afficher une **confirmation** (“Annuler” / “Supprimer”),
  - n’effectuer la suppression **qu’après** confirmation.

---

## 5. Fonctionnalités logiques et complètes

- Tout élément créé (écran, bouton, menu, filtre, dialog) doit être **fonctionnel**.
- Aucun bouton ne doit être “mort” : chaque action doit produire un effet logique (navigation, changement de liste, message, etc.).
- Un fichier ne doit **jamais disparaître sans raison** :
  - il doit toujours être visible dans au moins un contexte (Tous, un filtre, un dossier, etc.).
- Les filtres doivent être cohérents :
  - appliquer un filtre doit produire un résultat logique, pas vider toutes les listes sans explication.

---

## 6. Architecture et stack

- Rester sur la stack définie :
  - Kotlin
  - Jetpack Compose pour l’UI
  - MVVM (ViewModels, use cases, repositories)
  - Room pour la persistance locale
- Ne pas ajouter de nouvelles grosses dépendances sans justification.
- Ne pas utiliser ML Kit ou Google Play Services pour le scan sauf demande explicite.

---

## 7. Style de travail

- Travailler **fichier par fichier** ou fonctionnalité par fonctionnalité.
- Éviter les “gros refactors globaux” non demandés.
- Si plusieurs fichiers doivent être modifiés pour une tâche, le faire de façon structurée et compréhensible.
- Ajouter des commentaires courts et clairs sur les points importants (filtres, assignation de dossiers, suppression, etc.).

---

## 8. Rappel important

Tu dois respecter ces règles pour **toutes** les tâches, même si elles ne sont pas répétées dans le prompt.  
Priorité absolue :
- ne pas casser des fichiers qui fonctionnaient,
- ne pas tout supprimer ou tout réécrire sans demande,
- garder l’application logique et cohérente pour l’utilisateur.
