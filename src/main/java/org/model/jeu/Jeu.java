package org.model.jeu;

import org.model.piece.*;
import org.model.plateau.EchiquierModele;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;
import org.tools.ImageGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

public class Jeu extends Observable implements Runnable {

    public Coup nextC;

    private Joueur joueur1;
    private Joueur joueur2;
    private Joueur joueurCourant;
    private boolean termine = false;
    private EchiquierModele echiquier;
    private Coup dernierCoup;
    private final Map<String, Integer> positionHistory = new HashMap<>();
    private Thread gameThread;
    private volatile boolean attenteCoup = false;

    public Jeu() {
        echiquier = new EchiquierModele();
        joueur1 = new JHumain(this, true);
        joueur2 = new JHumain(this, false);
        joueurCourant = joueur1;
        Plateau p = PlateauSingleton.INSTANCE;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = echiquier.getPiece(r, c);
                p.getCase(r, c).setPiece(piece);
            }
        }
        gameThread = new Thread(this, "Jeu-Thread");
        gameThread.start();

        enregistrerPosition();
        sauvegardePng(); // image initiale
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

            if (!moved) continue;

            Joueur joueurSuivant = (joueurCourant == joueur1) ? joueur2 : joueur1;

            boolean isEchec = joueurSuivant.estEnEchec();
            boolean isMat   = isEchec && !joueurSuivant.aDesCoupsLegaux();

            if (isMat) {
                String gagnant = joueurCourant.isBlanc() ? "Blanc" : "Noir";
                System.out.println("RÉSULTAT: ÉCHEC ET MAT! Gagnant: " + gagnant);
                if (nextC != null) nextC.setType("ECHEC ET MAT");
                termine = true;
                sauvegardePng();
                setChanged();
                notifyObservers("ÉCHEC ET MAT - Gagnant: " + gagnant);
                break;
            }

            if (estPat(joueurSuivant)) {
                System.out.println("RÉSULTAT: PAT!");
                if (nextC != null) nextC.setType("PAT");
                termine = true;
                sauvegardePng();
                setChanged();
                notifyObservers("PAT - Match Nul");
                break;
            }

            joueurCourant = joueurSuivant;

            enregistrerPosition();
            if (estRepetitionTriple()) {
                System.out.println("RÉSULTAT: NULLE PAR RÉPÉTITION!");
                if (nextC != null) nextC.setType("RÉPÉTITION");
                termine = true;
                sauvegardePng();
                setChanged();
                notifyObservers("NULLE - Répétition de position (3 fois)");
                break;
            }
        }
    }

    public boolean partieTerminee() {
        return termine;
    }

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

            if (piece.isBlanc() != joueurCourant.isBlanc()) {
                System.out.println("Move rejected: not your piece (expected "
                        + (joueurCourant.isBlanc() ? "Blanc" : "Noir") + ")");
                return false;
            }

            org.model.plateau.Case oldEnPassantTarget = plateau.getEnPassantTarget();

            Piece captured = plateau.getCase(c.arr).getPiece();
            Piece capturedEnPassant = null;
            boolean isEnPassant = false;

            org.model.plateau.Case epTarget = plateau.getEnPassantTarget();
            if (piece instanceof Pion && captured == null && c.dep.y != c.arr.y
                    && epTarget != null
                    && epTarget.getX() == c.arr.x && epTarget.getY() == c.arr.y) {
                int dirEnPassant = piece.isBlanc() ? 1 : -1;
                org.model.plateau.Case capturedCase = plateau.getCase(c.arr.x + dirEnPassant, c.arr.y);
                if (capturedCase != null && capturedCase.getPiece() instanceof Pion
                        && capturedCase.getPiece().isBlanc() != piece.isBlanc()) {
                    isEnPassant = true;
                    capturedEnPassant = capturedCase.getPiece();
                    c.setType("PRISE EN PASSANT");
                    System.out.println("MISE A JOUR: PRISE EN PASSANT");
                }
            }

            c.setPieceName(piece.getClass().getSimpleName());
            c.setCapture(captured != null || isEnPassant);
            if (!("Pawn".equals(piece.getClass().getSimpleName()))
                    && !("King".equals(piece.getClass().getSimpleName()))) {
                c.setDisambiguation(computeDisambiguation(plateau, piece, c.arr));
            }

            boolean ok = plateau.deplacer(c.dep, c.arr);
            if (!ok) {
                System.out.println("MISE A JOUR: deplacement non autorise");
                echiquier.syncFromPlateau(plateau);
                setChanged();
                notifyObservers(c);
                return false;
            }

            if (isEnPassant) {
                int dirEnPassant = piece.isBlanc() ? 1 : -1;
                plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).setPiece(null);
                System.out.println("(Pion capturé en passant)");
            }

            // Détection roque
            boolean isCastling = false;
            if (piece instanceof Roi) {
                if (Math.abs(c.arr.y - c.dep.y) == 2) {
                    isCastling = true;
                    c.setType("ROQUE");
                    System.out.println("MISE A JOUR: ROQUE");
                    if (c.arr.y > c.dep.y) {
                        Piece rook = plateau.getCase(c.arr.x, 7).getPiece();
                        if (rook instanceof Tour) {
                            plateau.getCase(c.arr.x, 7).setPiece(null);
                            plateau.getCase(c.arr.x, 5).setPiece(rook);
                            System.out.println("(Roque côté roi)");
                        }
                    } else {
                        Piece rook = plateau.getCase(c.arr.x, 0).getPiece();
                        if (rook instanceof Tour) {
                            plateau.getCase(c.arr.x, 0).setPiece(null);
                            plateau.getCase(c.arr.x, 3).setPiece(rook);
                            System.out.println("(Roque côté reine)");
                        }
                    }
                }
            }

            // Promotion
            if (piece instanceof Pion) {
                int endRow = c.arr.x;
                if ((piece.isBlanc() && endRow == 0) || (!piece.isBlanc() && endRow == 7)) {
                    Piece newQueen = new Dame(piece.getColor());
                    plateau.getCase(c.arr).setPiece(newQueen);
                    c.setType("PROMOTION");
                    c.setPromotionTo("Q");
                    System.out.println("MISE A JOUR: PROMOTION — Pion -> Reine");
                }
            }

            echiquier.syncFromPlateau(plateau);
            dernierCoup = c;

            if (piece instanceof Pion && Math.abs(c.arr.x - c.dep.x) == 2) {
                int passedRow = (c.dep.x + c.arr.x) / 2;
                plateau.setEnPassantTarget(plateau.getCase(passedRow, c.arr.y));
                System.out.println("EP target set: (" + passedRow + "," + c.arr.y + ")");
            } else {
                plateau.setEnPassantTarget(null);
            }

            if (joueurCourant.estEnEchec()) {
                System.out.println("MISE A JOUR: ROI EN ÉCHEC — coup illégal, annulation");

                plateau.getCase(c.dep).setPiece(originalPiece);
                plateau.getCase(c.arr).setPiece(captured);

                if (isEnPassant && capturedEnPassant != null) {
                    int dirEnPassant = piece.isBlanc() ? 1 : -1;
                    plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).setPiece(capturedEnPassant);
                }
                if (isCastling) {
                    if (c.arr.y > c.dep.y) {
                        Piece rook = plateau.getCase(c.arr.x, 5).getPiece();
                        if (rook instanceof Tour) {
                            plateau.getCase(c.arr.x, 5).setPiece(null);
                            plateau.getCase(c.arr.x, 7).setPiece(rook);
                        }
                    } else {
                        Piece rook = plateau.getCase(c.arr.x, 3).getPiece();
                        if (rook instanceof Tour) {
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

            // Vérifier si l'adversaire est en échec
            Joueur adversaire = (joueurCourant == joueur1) ? joueur2 : joueur1;
            if (adversaire.estEnEchec()) {
                c.setType("ECHEC");
                System.out.println("MISE A JOUR: ÉCHEC");
            }

            System.out.println("MISE A JOUR: coup valide");
            sauvegardePng();
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

    public Joueur getJoueurCourant() {
        return joueurCourant;
    }

    public void nouvellePartie() {
        Thread old = gameThread;
        synchronized (this) {
            termine = true;
            nextC = null;
            notifyAll();
        }

        if (old != null && old.isAlive()) {
            try { old.join(2000); } catch (InterruptedException ignored) {}
        }

        synchronized (this) {
            echiquier = new EchiquierModele();
            Plateau p = PlateauSingleton.INSTANCE;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    p.getCase(r, c).setPiece(echiquier.getPiece(r, c));
                }
            }
            p.setEnPassantTarget(null);

            joueur1 = new JHumain(this, true);
            joueur2 = new JHumain(this, false);
            joueurCourant = joueur1;
            dernierCoup = null;
            nextC = null;
            attenteCoup = false;
            termine = false;

            positionHistory.clear();
        }

        enregistrerPosition();
        setChanged();
        notifyObservers(null);

        gameThread = new Thread(this, "Jeu-Thread");
        gameThread.start();
        System.out.println("Nouvelle partie lancée.");
    }

    public void sauvegardePng() {
        String dir  = System.getProperty("user.dir", System.getProperty("user.home"));
        String path = dir + java.io.File.separator + "partie_echecs.png";
        java.awt.image.BufferedImage img = ImageGenerator.renderBoard(PlateauSingleton.INSTANCE);
        ImageGenerator.saveAsPng(img, path);
    }

    private String computeDisambiguation(Plateau plateau, Piece piece, java.awt.Point arr) {
        List<Piece> ambiguous = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int col = 0; col < 8; col++) {
                org.model.plateau.Case cas = plateau.getCase(r, col);
                if (cas == null) continue;
                Piece other = cas.getPiece();
                if (other == null || other == piece) continue;
                if (other.getClass() != piece.getClass()) continue;
                if (other.isBlanc() != piece.isBlanc()) continue;
                // Est-ce que cette pièce peut atteindre
                for (org.model.plateau.Case accessible : other.getCaseAccessible()) {
                    if (accessible != null
                            && accessible.getX() == arr.x
                            && accessible.getY() == arr.y) {
                        ambiguous.add(other);
                        break;
                    }
                }
            }
        }
        if (ambiguous.isEmpty()) return "";

        int depFile = piece.getCase().getY();
        int depRank = piece.getCase().getX();

        boolean fileCollision = false;
        for (Piece a : ambiguous) {
            if (a.getCase() != null && a.getCase().getY() == depFile) {
                fileCollision = true;
                break;
            }
        }
        if (!fileCollision) return String.valueOf((char) ('a' + depFile));

        boolean rankCollision = false;
        for (Piece a : ambiguous) {
            if (a.getCase() != null && a.getCase().getX() == depRank) {
                rankCollision = true;
                break;
            }
        }
        if (!rankCollision) return String.valueOf(8 - depRank);

        return String.valueOf((char) ('a' + depFile)) + (8 - depRank);
    }

    public String genererClePosition() {
        Plateau plateau = PlateauSingleton.INSTANCE;
        StringBuilder sb = new StringBuilder(80);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                org.model.plateau.Case cas = plateau.getCase(r, c);
                if (cas == null || cas.getPiece() == null) {
                    sb.append('.');
                } else {
                    sb.append(cas.getPiece().getImageName()); // e.g. "wK", "bP"
                }
                sb.append(',');
            }
        }

        sb.append(joueurCourant.isBlanc() ? 'W' : 'B');
        sb.append(';');

        org.model.plateau.Case ep = plateau.getEnPassantTarget();
        if (ep != null) {
            sb.append(ep.getX()).append(':').append(ep.getY());
        } else {
            sb.append('-');
        }
        sb.append(';');

        org.model.plateau.Case wKing = plateau.getCase(7, 4);
        org.model.plateau.Case wRookK = plateau.getCase(7, 7);
        org.model.plateau.Case wRookQ = plateau.getCase(7, 0);
        sb.append((wKing  != null && wKing.getPiece()  instanceof Roi && wKing.getPiece().isBlanc())  ? 'K' : '-');
        sb.append((wRookK != null && wRookK.getPiece() instanceof Tour && wRookK.getPiece().isBlanc()) ? 'R' : '-');
        sb.append((wRookQ != null && wRookQ.getPiece() instanceof Tour && wRookQ.getPiece().isBlanc()) ? 'R' : '-');
        // Noir
        org.model.plateau.Case bKing = plateau.getCase(0, 4);
        org.model.plateau.Case bRookK = plateau.getCase(0, 7);
        org.model.plateau.Case bRookQ = plateau.getCase(0, 0);
        sb.append((bKing  != null && bKing.getPiece()  instanceof Roi && !bKing.getPiece().isBlanc())  ? 'k' : '-');
        sb.append((bRookK != null && bRookK.getPiece() instanceof Tour && !bRookK.getPiece().isBlanc()) ? 'r' : '-');
        sb.append((bRookQ != null && bRookQ.getPiece() instanceof Tour && !bRookQ.getPiece().isBlanc()) ? 'r' : '-');

        return sb.toString();
    }

    private void enregistrerPosition() {
        String cle = genererClePosition();
        positionHistory.merge(cle, 1, Integer::sum);
        System.out.println("Position enregistrée (" + positionHistory.get(cle) + "x): " + cle.substring(0, Math.min(40, cle.length())) + "…");
    }

    public boolean estRepetitionTriple() {
        String cle = genererClePosition();
        return positionHistory.getOrDefault(cle, 0) >= 3;
    }
}
