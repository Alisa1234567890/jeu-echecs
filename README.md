# Echiquier - Jeu d'Echecs en Java

Application de jeu d'echecs en Java avec interface graphique, vue console et generation d'image PNG du plateau.

## Prerequis
- Java JDK 23+
- Apache Maven 3.8+

## Lancement

```bash
mvn -q -DskipTests package
java -jar target/Echiquier-all.jar
```

## Fonctionnalites

### Interface Utilisateur
- **Interface graphique Swing** avec drag & drop des pieces
- **Vue console** avec symboles Unicode chess (♔ ♕ ♖ ♗ ♘ ♙)
- **Highlights visuels** : cases accessibles mises en evidence au clic
- **Mode choix** : Jouer contre un ami ou contre l'IA
- **Niveaux IA** : Facile, Moyen, Difficile (avec Minimax + Alpha-Beta Pruning)
- **Styles IA** : Équilibré, Agressif, Positionnel, Prudent
- **Horloge en temps reel** : Affiche le temps restant pour chaque joueur
- **Annonces de coups** : Messages pour captures, echecs, mats, etc.
- **Confetti** : Animation celebrant la victoire

### Regles d'Echecs
- **Detection d'echec** : Identifie quand le roi est en echec
- **Detection d'echec et mat** : Fin de partie si le roi est en echec et mat
- **Detection de pat** : Draw si aucun coup legal n'existe et le roi n'est pas en echec
- **Nulle par repetition** : Draw si la meme position se repete 3 fois
- **Nulle par la regle des 50 coups** : Draw apres 50 coups sans prise ni mouvement de pion
- **Materiel insuffisant** : Draw si les deux joueurs n'ont pas assez de pieces pour faire echec et mat

### Coups Specials
- **Roque** (grand et petit) : Castling avec verification des conditions
- **Prise en passant** : Capture speciale de pion
- **Promotion** : Transformation du pion en dame, fou, cavalier ou tour

### Sauvegarde et Affichage
- **Image PNG** : Generation automatique du plateau apres chaque coup
- **Fichier de sortie** : `partie_echecs.png` dans le repertoire courant
- **Historique des coups** : Affiche dans la console avec notation PGN
- **Theme unifie du plateau** : meme palette entre la vue principale, le PNG et la vue pseudo-3D

### Gestion de Partie
- **Nouvelle partie** : Reinitialisation complete du plateau
- **Resign** : Permet au joueur actif de capituler
- **Horloge** : Incremente et affiche continuellement le temps ecoule

## Architecture

### Packages

#### `org.model`
- **Jeu** : Logique principale du jeu, regles d'echecs, gestion IA
- **Joueur** : Interface pour les joueurs
- **JHumain** : Implementation pour les joueurs humains
- **JIA** : Implementation pour l'IA (random, minimax, alpha-beta)
- **Coup** : Representation d'un coup
- **JeuObserver** : Interface pour observer les changements de jeu

#### `org.model.piece`
- **Piece** : Classe abstraite pour toutes les pieces
- **King, Queen, Rook, Bishop, Knight, Pawn** : Implementations specifiques

#### `org.model.plateau`
- **Plateau** : Singleton representant l'echiquier
- **Case** : Representation d'une case du plateau
- **EchiquierModele** : Modele de l'echiquier pour la synchronisation UI

#### `org.controller`
- **MF** : Frame principale Swing, gestion UI
- **VC** : Composant de l'echiquier graphique avec drag & drop

#### `org.view`
- **VueConsole** : Affichage console avec symboles Unicode

#### `org.util`
- **BoardTheme** : Palette centralisee du plateau
- **ImageGenerator** : Generation des images PNG du plateau
- **SvgToPngConverter** : Conversion SVG vers PNG pour les pieces

## Algorithmes IA

### Niveaux de Difficulte

#### Facile
- Selectionne un coup aleatoire parmi les coups legaux

#### Moyen
- Minimax avec alpha-beta pruning, profondeur 2
- Evaluation positionnelle incluant:
  - Valeur des pieces
  - Piece-square tables (positions ideales)
  - Mobilite des pieces
  - Securite du roi

#### Difficile
- Minimax avec alpha-beta pruning, profondeur 4
- Meme evaluation que Moyen mais plus profonde
- Coups executes instantanement pour meilleure experience

### Styles de Jeu IA

- **Équilibré** : compromis entre activite, securite et opportunites tactiques
- **Agressif** : favorise les prises, les echecs et la pression immediate
- **Positionnel** : insiste davantage sur le centre, la coordination et l'activite des pieces
- **Prudent** : penalise plus fortement les cases dangereuses et valorise la securite du roi

## Symboles Unicode

```
Pieces Blanches:  ♔ ♕ ♖ ♗ ♘ ♙
Pieces Noires:    ♚ ♛ ♜ ♝ ♞ ♟
```

## Exemple de Sortie Console

```
NOUVELLE PARTIE
  a b c d e f g h
  +-+-+-+-+-+-+-+-+
8 |♜|♞|♝|♛|♚|♝|♞|♜|8
  +-+-+-+-+-+-+-+-+
7 |♟|♟|♟|♟|♟|♟|♟|♟|7
  +-+-+-+-+-+-+-+-+
6 | | | | | | | | |6
  +-+-+-+-+-+-+-+-+
5 | | | | | | | | |5
  +-+-+-+-+-+-+-+-+
4 | | | | | | | | |4
  +-+-+-+-+-+-+-+-+
3 | | | | | | | | |3
  +-+-+-+-+-+-+-+-+
2 |♙|♙|♙|♙|♙|♙|♙|♙|2
  +-+-+-+-+-+-+-+-+
1 |♖|♘|♗|♕|♔|♗|♘|♖|1
  +-+-+-+-+-+-+-+-+
  a b c d e f g h

1. NORMAL e2 -> e4
2. NORMAL e7 -> e5
...
```

## Notes Techniques

- **Thread de jeu** : La logique du jeu s'execute dans un thread dedié
- **Synchronization** : Utilisation de `synchronized` pour la securite multi-thread
- **Observer Pattern** : Les observateurs (UI, console) sont notifies des changements
- **Swing EDT** : Les updates UI sont delegues au Swing Event Dispatch Thread
- **Horloge** : Mise a jour via Timer Swing (200ms) pour fluidite
- **Tests automatiques** : Des tests JUnit valident les styles IA et la gestion des coups sous echec

## Dossier de projet

- `DOSSIER_DE_REALISATION.md` : demarche, choix techniques, points distinctifs a presenter a l'oral

## Auteur
Projet de jeu d'echecs en Java avec IA avancee et interface graphique.

