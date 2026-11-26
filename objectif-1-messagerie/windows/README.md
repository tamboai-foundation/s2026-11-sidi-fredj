# NGoma — Windows (Compose Desktop + TModeler)

Projet de départ Foundation. Package : `ai.tambo.foundation.ngoma`. Charte rouge (`#E0262D`).

## Lancer
```
gradlew.bat run
```
JDK 21 requis.

## Écrans (de quoi réaliser le Module 1 SI)
Barre d'onglets à gauche : **Messages**, **Vidéos**, **Personnes**, **Profil**.
1. **Gestion des utilisateurs** : inscription / connexion, **Personnes** (liste, recherche, profil d'une personne, écrire un message), **Profil** (modifier son nom et sa description, voir ses vidéos, se déconnecter, supprimer son compte).
2. **Conversations et messages** : liste des conversations (recherche, filtres, épingler, sourdine, archiver), conversation (envoi, réponse, modification, suppression, réactions).
3. **Publications vidéo et interactions**, façon TikTok : **Vidéos** (fil à défilement vertical page par page), publier (photo ou vidéo, facultatives, + légende ; une légende seule est acceptée), modifier la légende, supprimer, j'aime, commentaires et réponses (aimer, modifier, supprimer les siens), publications des autres, onglet « Mes vidéos ». La lecture de la vidéo n'est pas incluse (aucun lecteur dans Compose Desktop) : à ajouter avec la bibliothèque de votre choix sur Post.mediaPath.

## Mocks (niveau ui-bridge-TModeler)
`src/main/kotlin/ai/tambo/foundation/ngoma/bridge/Bridges.kt` : interfaces `AccountBridge`, `MessagingBridge` et `VideoBridge`, avec leurs mocks (tout identifiant est accepté).
Le point de bascule est l'objet `Bridges` dans `Main.kt` : remplacer les mocks par des implémentations TModeler.

Les écrans ne changent pas : votre travail est de **concevoir vos modèles TModeler** (utilisateur, conversation, message, publication, commentaire, j'aime) puis d'écrire les trois implémentations des interfaces à partir de ces modèles. Les photos et vidéos choisies dans l'interface sont des fichiers : à enregistrer comme médias TModeler.

## TModeler
- `co.tamboai:TModeler:1.0.1` (Maven Central).
- Init : `NGomaTm.kt` (`DB_VERSION` : l'incrémenter repart d'une base vide).
- Exemple de modèle : `models/Note.kt`.
