package org.model;

import org.model.piece.*;
import org.model.plateau.EchiquierModele;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.Observable;

public class Jeu extends Observable implements Runnable {

    public Coup nextC;

    private Joueur joueur1;
    private Joueur joueur2;
    private Joueur joueurCourant;
    private boolean termine = false;

    private EchiquierModele echiquier;
    private Coup dernierCoup;
    // true uniquement quand le jeu est bloqué dans attendreCoup() - empêche de jouer 2x
    private volatile boolean attenteCoup = false;

    public Jeu() {
        echiquier = new EchiquierModele();
        joueur1 = new JHumain(this, true);   // Joueur blanc
        joueur2 = new JHumain(this, false);  // Joueur noir
        joueurCourant = joueur1;             // Blanc joue en premier
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
            Coup c = joueurCourant.getCoup();
            boolean moved = false;
            if (c != null) {
                moved = appliquerCoup(c);
            }

            // Only advance the turn when a legal move was actually made
            if (!moved) continue;

            Joueur joueurSuivant = (joueurCourant == joueur1) ? joueur2 : joueur1;

            if (estEchecEtMat(joueurSuivant)) {
                String gagnant = joueurCourant.isBlanc() ? "Blanc" : "Noir";
                System.out.println("RÉSULTAT: ÉCHEC ET MAT! Gagnant: " + gagnant);
                if (nextC != null) nextC.setType("ECHEC ET MAT");
                termine = true;
                setChanged();
                notifyObservers("ÉCHEC ET MAT - Gagnant: " + gagnant);
                break;
            }

            if (estPat(joueurSuivant)) {
                System.out.println("RÉSULTAT: PAT!");
                if (nextC != null) nextC.setType("PAT");
                termine = true;
                setChanged();
                notifyObservers("PAT - Match Nul");
                break;
            }

            joueurCourant = joueurSuivant;
        }
    }

    public boolean partieTerminee() {
        return termine;
    }

    /**
     * Bloque le Jeu-Thread jusqu'à ce que l'utilisateur clique (via setCoup).
     * Le flag attenteCoup est true uniquement pendant ce wait(), ce qui empêche
     * setCoup d'accepter un coup en dehors du bon moment.
     */
    public synchronized Coup attendreCoup() {
        attenteCoup = true;
        try {
            wait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        attenteCoup = false;
        return nextC;
    }

    /**
     * Appelé par la vue quand l'utilisateur clique.
     * Ignoré si le jeu n'est pas en train d'attendre un coup (évite de jouer 2x).
     */
    public void setCoup(Coup c) {
        if (c == null) return;
        synchronized (this) {
            if (!attenteCoup) {
                System.out.println("setCoup: ignoré — pas le bon moment");
                return;
            }
            this.nextC = c;
            this.notifyAll();
        }
    }

    public boolean appliquerCoup(Coup c) {
        if (c == null) return false;
        synchronized (this) {
            nextC = c;
            System.out.println("Attempting move: " + c.dep + " -> " + c.arr);

            Plateau plateau = PlateauSingleton.INSTANCE;
            Piece piece = plateau.getCase(c.dep).getPiece();
            Piece originalPiece = piece;

            if (piece == null) {
                System.out.println("Move result: no piece at departure");
                return false;
            }

            // Valider que la pièce appartient au joueur courant
            if (piece.isBlanc() != joueurCourant.isBlanc()) {
                System.out.println("Move rejected: not your piece (expected "
                        + (joueurCourant.isBlanc() ? "Blanc" : "Noir") + ")");
                return false;
            }

            // Save en passant target for possible rollback
            org.model.plateau.Case oldEnPassantTarget = plateau.getEnPassantTarget();

            Piece captured = plateau.getCase(c.arr).getPiece();
            Piece capturedEnPassant = null;
            boolean isEnPassant = false;

            // Detect en passant: diagonal pawn move to the current ep-target square (empty)
            org.model.plateau.Case epTarget = plateau.getEnPassantTarget();
            if (piece instanceof Pawn && captured == null && c.dep.y != c.arr.y
                    && epTarget != null
                    && epTarget.getX() == c.arr.x && epTarget.getY() == c.arr.y) {
                int dirEnPassant = piece.isBlanc() ? 1 : -1;
                org.model.plateau.Case capturedCase = plateau.getCase(c.arr.x + dirEnPassant, c.arr.y);
                if (capturedCase != null && capturedCase.getPiece() instanceof Pawn
                        && capturedCase.getPiece().isBlanc() != piece.isBlanc()) {
                    isEnPassant = true;
                    capturedEnPassant = capturedCase.getPiece();
                    c.setType("PRISE EN PASSANT");
                    System.out.println("MISE A JOUR: PRISE EN PASSANT");
                }
            }

            boolean ok = plateau.deplacer(c.dep, c.arr);
            if (!ok) {
                System.out.println("MISE A JOUR: deplacement non autorise");
                echiquier.syncFromPlateau(plateau);
                setChanged();
                notifyObservers(c);
                return false;
            }

            // Remove the captured pawn (en passant)
            if (isEnPassant) {
                int dirEnPassant = piece.isBlanc() ? 1 : -1;
                plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).setPiece(null);
                System.out.println("(Pion capturé en passant)");
            }

            // Détection roque
            boolean isCastling = false;
            if (piece instanceof King) {
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

            // Promotion
            if (piece instanceof Pawn) {
                int endRow = c.arr.x;
                if ((piece.isBlanc() && endRow == 0) || (!piece.isBlanc() && endRow == 7)) {
                    Piece newQueen = new Queen(piece.getColor());
                    plateau.getCase(c.arr).setPiece(newQueen);
                    c.setType("PROMOTION");
                    System.out.println("MISE A JOUR: PROMOTION — Pion -> Reine");
                }
            }

            echiquier.syncFromPlateau(plateau);
            dernierCoup = c;

            // Update en passant target: set after double pawn push, clear otherwise
            if (piece instanceof Pawn && Math.abs(c.arr.x - c.dep.x) == 2) {
                int passedRow = (c.dep.x + c.arr.x) / 2;
                plateau.setEnPassantTarget(plateau.getCase(passedRow, c.arr.y));
                System.out.println("EP target set: (" + passedRow + "," + c.arr.y + ")");
            } else {
                plateau.setEnPassantTarget(null);
            }

            // Si le coup laisse le roi du joueur courant en échec → coup illégal, annuler
            if (joueurCourant.estEnEchec()) {
                System.out.println("MISE A JOUR: ROI EN ÉCHEC — coup illégal, annulation");

                // Direct restoration — bypass deplacer (backward moves not in getCaseAccessible)
                plateau.getCase(c.dep).setPiece(originalPiece);
                plateau.getCase(c.arr).setPiece(captured);

                if (isEnPassant && capturedEnPassant != null) {
                    int dirEnPassant = piece.isBlanc() ? 1 : -1;
                    plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).setPiece(capturedEnPassant);
                }
                if (isCastling) {
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

                plateau.setEnPassantTarget(oldEnPassantTarget);
                echiquier.syncFromPlateau(plateau);
                setChanged();
                notifyObservers(c);
                return false;
            }

            // Vérifier si l'adversaire est en échec simple
            Joueur adversaire = (joueurCourant == joueur1) ? joueur2 : joueur1;
            if (adversaire.estEnEchec()) {
                c.setType("ECHEC");
                System.out.println("MISE A JOUR: ÉCHEC");
            }

            System.out.println("MISE A JOUR: coup valide");
            setChanged();
            notifyObservers(c);
            return true;
        }
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

    public Coup getDernierCoup() {
        return dernierCoup;
    }

    public Joueur getJoueurCourant() {
        return joueurCourant;
    }
}
