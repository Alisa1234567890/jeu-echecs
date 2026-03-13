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
    private boolean[] roqueDisponible = new boolean[2];  // [blanc, noir]
    private boolean[] roiBouge = new boolean[2];  // [blanc, noir] - pour déterminer si roque possible

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
                System.out.println("Move result: no piece at departure");
                return;
            }

            Piece captured = plateau.getCase(c.arr).getPiece();
            Piece capturedEnPassant = null;
            boolean isEnPassant = false;

            // 0. Déterminer si c'est une prise en passant AVANT de déplacer
            if (piece instanceof Pawn && captured == null
                    && c.dep.y != c.arr.y && c.dep.x != c.arr.x) {
                int dirEnPassant = piece.isBlanc() ? 1 : -1;
                Piece pawnCaptured = plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).getPiece();
                if (pawnCaptured instanceof Pawn && pawnCaptured.isBlanc() != piece.isBlanc()) {
                    isEnPassant = true;
                    capturedEnPassant = pawnCaptured;
                    // CRUCIAL : placer temporairement le pion ennemi sur c.arr
                    // pour que plateau.deplacer() accepte le mouvement diagonal
                    plateau.getCase(c.arr).setPiece(capturedEnPassant);
                    System.out.println("En passant detected, placing temp piece");
                }
            }

            // 1. Vérifier que le mouvement est légal
            boolean ok = plateau.deplacer(c.dep, c.arr);
            if (!ok) {
                // Annuler le placement temporaire si besoin
                if (isEnPassant) plateau.getCase(c.arr).setPiece(null);
                System.out.println("Move result: illegal move");
                echiquier.syncFromPlateau(plateau);
                setChanged();
                notifyObservers(c);
                return;
            }

            // 2. Retirer le vrai pion capturé en passant (sa case d'origine, pas c.arr)
            if (isEnPassant) {
                int dirEnPassant = piece.isBlanc() ? 1 : -1;
                plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).setPiece(null);
                System.out.println("En passant capture executed!");
            }

            // 3. Gérer le roque AVANT promotion
            boolean isCastling = false;
            if (piece instanceof King) {
                int colorIndex = piece.isBlanc() ? 0 : 1;
                roiBouge[colorIndex] = true;

                if (Math.abs(c.arr.y - c.dep.y) == 2) {
                    isCastling = true;
                    System.out.println("Castling detected!");
                    if (c.arr.y > c.dep.y) {
                        Piece rook = plateau.getCase(c.arr.x, 7).getPiece();
                        if (rook instanceof Rook) {
                            plateau.getCase(c.arr.x, 7).setPiece(null);
                            plateau.getCase(c.arr.x, 5).setPiece(rook);
                            System.out.println("Kingside castling executed");
                        }
                    } else {
                        Piece rook = plateau.getCase(c.arr.x, 0).getPiece();
                        if (rook instanceof Rook) {
                            plateau.getCase(c.arr.x, 0).setPiece(null);
                            plateau.getCase(c.arr.x, 3).setPiece(rook);
                            System.out.println("Queenside castling executed");
                        }
                    }
                }
            }

            // 4. Marquer les tours comme déplacées
            if (piece instanceof Rook) {
                int colorIndex = piece.isBlanc() ? 0 : 1;
                roqueDisponible[colorIndex] = false;
            }

            // 5. Promotion du pion
            Piece promotedPiece = piece;
            if (piece instanceof Pawn && !isCastling) {
                int endRow = c.arr.x;
                if ((piece.isBlanc() && endRow == 0) || (!piece.isBlanc() && endRow == 7)) {
                    Piece newQueen = new Queen(piece.getColor());
                    plateau.getCase(c.arr).setPiece(newQueen);
                    promotedPiece = newQueen;
                    System.out.println("Pawn promoted to Queen!");
                }
            }

            // 6. Synchroniser l'affichage
            echiquier.syncFromPlateau(plateau);
            dernierCoup = c;

            // 7. Vérifier échec après le coup
            if (joueurCourant.estEnEchec()) {
                System.out.println("Move would leave king in check - INVALID");

                plateau.deplacer(c.arr, c.dep);

                if (promotedPiece != originalPiece) {
                    plateau.getCase(c.dep).setPiece(originalPiece);
                }

                if (captured != null) {
                    plateau.getCase(c.arr).setPiece(captured);
                }

                // Restaurer le pion capturé en passant ET vider c.arr
                if (isEnPassant) {
                    int dirEnPassant = piece.isBlanc() ? 1 : -1;
                    plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).setPiece(capturedEnPassant);
                    plateau.getCase(c.arr).setPiece(null); // c.arr doit être vide
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

            System.out.println("Move result: success");
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

    /**
     * Vérifie si le roque est valide (conditions pour roquer).
     */
    private boolean estRoqueValide(Coup c, Piece piece) {
        if (!(piece instanceof King)) return false;
        if (Math.abs(c.arr.y - c.dep.y) != 2) return false;

        int colorIndex = piece.isBlanc() ? 0 : 1;
        if (roiBouge[colorIndex]) return false;  // Le roi a déjà bougé

        Plateau plateau = PlateauSingleton.INSTANCE;

        // Vérifier que les cases intermédiaires sont vides
        int minY = Math.min(c.dep.y, c.arr.y);
        int maxY = Math.max(c.dep.y, c.arr.y);
        for (int y = minY + 1; y < maxY; y++) {
            if (!plateau.getCase(c.dep.x, y).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Vérifie si la prise en passant est valide.
     */
    private boolean estPriseEnPassantValide(Coup c, Piece piece) {
        if (!(piece instanceof Pawn)) return false;
        if (dernierCoup == null) return false;

        // Le dernier coup doit être un pion qui s'est déplacé de 2 cases
        Plateau plateau = PlateauSingleton.INSTANCE;
        Piece lastMovedPiece = plateau.getCase(dernierCoup.arr).getPiece();
        if (!(lastMovedPiece instanceof Pawn)) return false;

        // Vérifier que c'est une capture en diagonale sans pièce
        if (c.arr.x == dernierCoup.arr.x && c.arr.y == dernierCoup.arr.y + 1 ||
            c.arr.x == dernierCoup.arr.x && c.arr.y == dernierCoup.arr.y - 1) {
            int dist = Math.abs(dernierCoup.arr.x - dernierCoup.dep.x);
            return dist == 2;  // Le pion s'est déplacé de 2 cases
        }

        return false;
    }
}
