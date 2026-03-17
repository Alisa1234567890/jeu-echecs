package org.model;

import org.model.piece.*;
import org.model.plateau.EchiquierModele;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.awt.Point;
import java.util.Observable;

public class Jeu extends Observable implements Runnable {

    public Coup nextC;

    private Joueur joueur1;
    private Joueur joueur2;
    private Joueur joueurCourant;
    private boolean termine = false;

    private EchiquierModele echiquier;
    private Coup dernierCoup;  // Pour la prise en passant
    private boolean[] roqueDisponible = new boolean[2];  // blanc, noir
    private boolean[] roiBouge = new boolean[2];  // blanc, noir - pour déterminer si roque possible

    public Jeu() {
        echiquier = new EchiquierModele();
        joueur1 = new JHumain(this, true);   // Joueur blanc
        joueur2 = new JHumain(this, false);  // Joueur noir
        joueurCourant = joueur1;
        roqueDisponible[0] = roqueDisponible[1] = true;  // Les deux peuvent roquer au départ
        roiBouge[0] = roiBouge[1] = false;
        Plateau p = PlateauSingleton.INSTANCE;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = echiquier.getPiece(r, c);
                p.getCase(r, c).setPiece(piece);
            }
        }
        new Thread(this, "Jeu-Thread").start();
    }

    public EchiquierModele getEchiquier() {
        return echiquier;
    }

    @Override
    public void run() {
        jouerPartie();
    }

    public void jouerPartie() {
        while (!partieTerminee()) {
            Joueur js = getJoueurSuivant();
            
            // Vérifier les conditions de fin de partie
            if (estEchecEtMat(js)) {
                String gagnant = js.isBlanc() ? "Noir" : "Blanc";
                System.out.println("RÉSULTAT: ÉCHEC ET MAT!");
                System.out.println("Gagnant: " + String.format("%-26s", gagnant));
                System.out.println("Perdant: " + String.format("%-26s", (js.isBlanc() ? "Blanc" : "Noir")));

                if (nextC != null) {
                    nextC.setType("ECHEC ET MAT"); // Marquer le dernier coup
                }
                termine = true;
                setChanged();
                notifyObservers("ÉCHEC ET MAT - Gagnant: " + gagnant);
                break;
            }
            
            if (estPat(js)) {
                System.out.println("RÉSULTAT: PAT!");
                System.out.println("Aucun joueur n'a gagné ");

                if (nextC != null) {
                    nextC.setType("PAT");
                }
                termine = true;
                setChanged();
                notifyObservers("PAT - Match Nul");
                break;
            }
            
            Coup c = js.getCoup();
            if (c != null) {
                appliquerCoup(c);
            }
        }
    }

    public boolean partieTerminee() {
        return termine;
    }

    public Joueur getJoueurSuivant() {
        joueurCourant = (joueurCourant == joueur1) ? joueur2 : joueur1;
        return joueurCourant;
    }

    public void appliquerCoup(Coup c) {
        if (c == null) return;
        synchronized (this) {
            nextC = c;
            System.out.println("Attempting move: " + c.dep + " -> " + c.arr);

            Plateau plateau = PlateauSingleton.INSTANCE;
            Piece piece = plateau.getCase(c.dep).getPiece();
            Piece originalPiece = piece;
            if (piece == null) {
                System.out.println(" Move result: no piece at departure");
                return;
            }

            Piece captured = plateau.getCase(c.arr).getPiece();
            Piece capturedEnPassant = null;
            boolean isEnPassant = false;

            if (piece instanceof Pawn && captured == null
                    && c.dep.y != c.arr.y && c.dep.x != c.arr.x) {
                int dirEnPassant = piece.isBlanc() ? 1 : -1;
                Piece pawnCaptured = plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).getPiece();
                if (pawnCaptured instanceof Pawn && pawnCaptured.isBlanc() != piece.isBlanc()) {
                    isEnPassant = true;
                    capturedEnPassant = pawnCaptured;
                    c.setType("PRISE EN PASSANT");
                    plateau.getCase(c.arr).setPiece(capturedEnPassant);
                    System.out.println("MISE A JOUR: PRISE EN PASSANT");
                }
            }

            boolean ok = plateau.deplacer(c.dep, c.arr);
            if (!ok) {
                if (isEnPassant) plateau.getCase(c.arr).setPiece(null);
                System.out.println("MISE A JOUR: deplacement non autorise");
                echiquier.syncFromPlateau(plateau);
                setChanged();
                notifyObservers(c);
                return;
            }

            if (isEnPassant) {
                int dirEnPassant = piece.isBlanc() ? 1 : -1;
                plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).setPiece(null);
                System.out.println("(Pion capturé en passant)");
            }

            boolean isCastling = false;
            if (piece instanceof King) {
                int colorIndex = piece.isBlanc() ? 0 : 1;
                roiBouge[colorIndex] = true;

                if (Math.abs(c.arr.y - c.dep.y) == 2) {
                    isCastling = true;
                    c.setType("ROQUE");
                    System.out.println("MISE A JOUR: ROQUE");
                    if (c.arr.y > c.dep.y) {
                        Piece rook = plateau.getCase(c.arr.x, 7).getPiece();
                        if (rook instanceof Rook) {
                            plateau.getCase(c.arr.x, 7).setPiece(null);
                            plateau.getCase(c.arr.x, 5).setPiece(rook);
                            System.out.println("(Roque côté roi)");
                        }
                    } else {
                        Piece rook = plateau.getCase(c.arr.x, 0).getPiece();
                        if (rook instanceof Rook) {
                            plateau.getCase(c.arr.x, 0).setPiece(null);
                            plateau.getCase(c.arr.x, 3).setPiece(rook);
                            System.out.println("(Roque côté reine)");
                        }
                    }
                }
            }

            if (piece instanceof Rook) {
                int colorIndex = piece.isBlanc() ? 0 : 1;
                roqueDisponible[colorIndex] = false;
            }


            Piece promotedPiece = piece;
            if (piece instanceof Pawn && !isCastling) {
                int endRow = c.arr.x;
                if ((piece.isBlanc() && endRow == 0) || (!piece.isBlanc() && endRow == 7)) {
                    Piece newQueen = new Queen(piece.getColor());
                    plateau.getCase(c.arr).setPiece(newQueen);
                    promotedPiece = newQueen;
                    c.setType("PROMOTION");
                    System.out.println("MISE A JOUR: PROMOTION");
                    System.out.println("Pion -> Reine");
                }
            }


            echiquier.syncFromPlateau(plateau);
            dernierCoup = c;

            if (joueurCourant.estEnEchec()) {
                System.out.println("MISE A JOUR: ROI EN ÉCHEC");
                System.out.println("Le coup laisse le roi en échec");

                plateau.deplacer(c.arr, c.dep);

                if (promotedPiece != originalPiece) {
                    plateau.getCase(c.dep).setPiece(originalPiece);
                }

                if (captured != null) {
                    plateau.getCase(c.arr).setPiece(captured);
                }


                if (isEnPassant) {
                    int dirEnPassant = piece.isBlanc() ? 1 : -1;
                    plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).setPiece(capturedEnPassant);
                    plateau.getCase(c.arr).setPiece(null);
                }

                if (isCastling && piece instanceof King) {
                    if (c.arr.y > c.dep.y) {
                        Piece rook = plateau.getCase(c.arr.x, 5).getPiece();
                        if (rook instanceof Rook) {
                            plateau.getCase(c.arr.x, 5).setPiece(null);
                            plateau.getCase(c.arr.x, 7).setPiece(rook);
                        }
                    } else {
                        Piece rook = plateau.getCase(c.arr.x, 3).getPiece();
                        if (rook instanceof Rook) {
                            plateau.getCase(c.arr.x, 3).setPiece(null);
                            plateau.getCase(c.arr.x, 0).setPiece(rook);
                        }
                    }
                }

                echiquier.syncFromPlateau(plateau);
                setChanged();
                notifyObservers(c);
                return;
            }

            System.out.println("MISE A JOUR: coup valide");
            setChanged();
            this.notifyAll();
            notifyObservers(c);
        }
    }

    public void setCoup(Coup c) {
        if (c == null) return;
        synchronized (this) {
            this.nextC = c;
            this.notifyAll();
            setChanged();
            notifyObservers(c);
        }
    }


    private boolean estRoqueValide(Coup c, Piece piece) {
        if (!(piece instanceof King)) return false;
        if (Math.abs(c.arr.y - c.dep.y) != 2) return false;

        int colorIndex = piece.isBlanc() ? 0 : 1;
        if (roiBouge[colorIndex]) return false;

        Plateau plateau = PlateauSingleton.INSTANCE;


        int minY = Math.min(c.dep.y, c.arr.y);
        int maxY = Math.max(c.dep.y, c.arr.y);
        for (int y = minY + 1; y < maxY; y++) {
            if (!plateau.getCase(c.dep.x, y).isEmpty()) {
                return false;
            }
        }

        return true;
    }


    private boolean estPriseEnPassantValide(Coup c, Piece piece) {
        if (!(piece instanceof Pawn)) return false;
        if (dernierCoup == null) return false;


        Plateau plateau = PlateauSingleton.INSTANCE;
        Piece lastMovedPiece = plateau.getCase(dernierCoup.arr).getPiece();
        if (!(lastMovedPiece instanceof Pawn)) return false;


        if (c.arr.x == dernierCoup.arr.x && c.arr.y == dernierCoup.arr.y + 1 ||
            c.arr.x == dernierCoup.arr.x && c.arr.y == dernierCoup.arr.y - 1) {
            int dist = Math.abs(dernierCoup.arr.x - dernierCoup.dep.x);
            return dist == 2;
        }

        return false;
    }


    public boolean estEchecEtMat(Joueur joueur) {

        return joueur.estEnEchec() && !joueur.aDesCoupsLegaux();
    }


    public boolean estPat(Joueur joueur) {

        return !joueur.estEnEchec() && !joueur.aDesCoupsLegaux();
    }



    public boolean aGagne(Joueur joueur) {
        Joueur adversaire = (joueur == joueur1) ? joueur2 : joueur1;
        return estEchecEtMat(adversaire);
    }
}
