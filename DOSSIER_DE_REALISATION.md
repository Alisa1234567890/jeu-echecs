# Dossier de realisation

## 1. Demarche de projet

Ce projet a ete structure autour de trois objectifs :
- proposer un jeu d'echecs jouable immediatement en interface Swing ;
- couvrir les regles classiques (echec, mat, pat, repetition, roque, prise en passant, promotion) ;
- fournir des outils de demonstration utiles le jour de la soutenance (PNG, vue pseudo-3D, connecteur reseau de test).

## 2. Choix techniques assumes

### Architecture
- `org.model` : logique de jeu, validation des coups, IA, gestion des fins de partie ;
- `org.controller` : interface principale et echiquier interactif ;
- `org.view` : vue console et vue pseudo-3D ;
- `org.util` / `org.tools` : generation PNG, theme, reseau, conversion SVG.

### Theme unique du plateau
Les couleurs du plateau ont ete centralisees dans `org.util.BoardTheme` afin d'assurer la coherence entre :
- le plateau principal Swing ;
- le PNG genere ;
- la vue pseudo-3D.

### IA a styles
En plus du niveau (`Facile`, `Moyen`, `Difficile`), l'IA propose maintenant un **style de jeu** :
- `Équilibré` : compromis entre tactique et stabilite ;
- `Agressif` : recherche les menaces, prises et echecs ;
- `Positionnel` : privilegie centre, activite et coordination ;
- `Prudent` : protege le roi et penalise davantage les cases dangereuses.

Ce style modifie :
- l'evaluation locale d'un coup (`scoreMove`) ;
- l'heuristique de recherche (`evaluatePosition`) ;
- l'information affichee dans l'interface principale.

## 3. Elements distinctifs presentes en demo

1. **Choix du style IA au lancement** (`org.Main`) ;
2. **Affichage du style IA et de sa description** dans la fenetre principale ;
3. **Vue pseudo-3D interactive** avec angles X/Y et zoom actifs ;
4. **Connecteur reseau de test** pilotable en local depuis l'interface ;
5. **Generation automatique d'un PNG standard** apres chaque coup.

## 4. Validation et qualite

Des tests automatises ont ete ajoutes dans `src/test/java` pour verifier :
- qu'un joueur en echec ne peut pas jouer un coup qui ne pare pas l'echec ;
- qu'une parade legale reste autorisee ;
- que les styles IA influencent reellement l'evaluation des coups ;
- que l'IA renvoie toujours un coup legal quel que soit le style choisi.

## 5. Fichiers a montrer a l'oral

- `src/main/java/org/model/Jeu.java`
- `src/main/java/org/model/JIA.java`
- `src/main/java/org/controller/MF.java`
- `src/main/java/org/controller/VC.java`
- `src/main/java/org/view/Simple3DView.java`
- `src/main/java/org/util/BoardTheme.java`
- `src/test/java/org/model/JeuAiStyleTest.java`
- `src/test/java/org/model/JeuRulesTest.java`

## 6. Message simple pour l'enseignant

"Le projet ne se limite pas a un moteur d'echecs : j'ai unifie le theme graphique, ajoute un choix de style IA visible dans l'interface, renforce les validations de coups en situation d'echec et ajoute des tests automatiques pour documenter ces choix."
