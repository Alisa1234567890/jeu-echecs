### Echiquier - Jeu d'Echecs en Java

Application de jeu d'echecs en Java avec interface graphique Swing, vue console et generation d'image PNG du plateau.

#### Lancement

`mvn -q -DskipTests package java -jar target/Echiquier-all.jar`

#### Fonctionnalites

-   Interface graphique Swing (drag & drop des pieces)
-   Vue console avec symboles Unicode
-   Cases accessibles mises en evidence au clic
-   Detection : echec, echec et mat, pat, nulle par repetition
-   Roque (grand et petit), prise en passant, promotion
-   Nouvelle partie via menu (Cmd+N)
-   Image PNG du plateau generee apres chaque coup : `partie_echecs.png`