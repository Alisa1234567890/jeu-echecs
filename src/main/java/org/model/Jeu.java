package org.model;

import org.model.piece.*;
import org.model.plateau.EchiquierModele;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;
import org.tools.PgnRecorder;

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
    // Enregistreur PGN
    private PgnRecorder pgnRecorder = new PgnRecorder("Blanc", "Noir");
    // Historique des positions pour détecter la répétition (clef = position de jeu)
    private final Map<String, Integer> positionHistory = new HashMap<>();
    // Référence au thread de jeu courant (pour pouvoir l'arrêter lors d'une nouvelle partie)
    private Thread gameThread;
    // true uniquement quand le jeu est bloqué dans attendreCoup()
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
        // Enregistrer la position initiale
        enregistrerPosition();
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

            // Calculer mat/pat/échec directement depuis l'état du jeu,
            // indépendamment de c.getType() (qui peut être "PROMOTION", "ROQUE", etc.)
            boolean isEchec = joueurSuivant.estEnEchec();
            boolean isMat   = isEchec && !joueurSuivant.aDesCoupsLegaux();

            // ── Enregistrement PGN ──────────────────────────────────────────
            pgnRecorder.enregistrerCoup(c, joueurCourant.isBlanc(),
                    isEchec && !isMat, isMat);

            if (isMat) {
                String gagnant = joueurCourant.isBlanc() ? "Blanc" : "Noir";
                System.out.println("RÉSULTAT: ÉCHEC ET MAT! Gagnant: " + gagnant);
                if (nextC != null) nextC.setType("ECHEC ET MAT");
                termine = true;
                pgnRecorder.setResult(joueurCourant.isBlanc() ? "1-0" : "0-1");
                sauvegardePgn();
                setChanged();
                notifyObservers("ÉCHEC ET MAT - Gagnant: " + gagnant);
                break;
            }

            if (estPat(joueurSuivant)) {
                System.out.println("RÉSULTAT: PAT!");
                if (nextC != null) nextC.setType("PAT");
                termine = true;
                pgnRecorder.setResult("1/2-1/2");
                sauvegardePgn();
                setChanged();
                notifyObservers("PAT - Match Nul");
                break;
            }

            // Changer de joueur AVANT d'enregistrer la position :
            // la clef doit refléter qui joue ensuite, pas qui vient de jouer.
            joueurCourant = joueurSuivant;

            // Enregistrer la position et vérifier la répétition triple
            enregistrerPosition();
            if (estRepetitionTriple()) {
                System.out.println("RÉSULTAT: NULLE PAR RÉPÉTITION!");
                if (nextC != null) nextC.setType("RÉPÉTITION");
                termine = true;
                pgnRecorder.setResult("1/2-1/2");
                sauvegardePgn();
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

            // Valider que la pièce appartient au joueur courant
            if (piece.isBlanc() != joueurCourant.isBlanc()) {
                System.out.println("Move rejected: not your piece (expected "
                        + (joueurCourant.isBlanc() ? "Blanc" : "Noir") + ")");
                return false;
            }

            org.model.plateau.Case oldEnPassantTarget = plateau.getEnPassantTarget();

            Piece captured = plateau.getCase(c.arr).getPiece();
            Piece capturedEnPassant = null;
            boolean isEnPassant = false;

            // Detecter le prise en passant
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

            // ── Métadonnées PGN — collectées AVANT le déplacement ───────────
            c.setPieceName(piece.getClass().getSimpleName());
            c.setCapture(captured != null || isEnPassant);
            if (!("Pawn".equals(piece.getClass().getSimpleName()))
                    && !("King".equals(piece.getClass().getSimpleName()))) {
                c.setDisambiguation(computeDisambiguation(plateau, piece, c.arr));
            }
            // ────────────────────────────────────────────────────────────────

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
                    c.setPromotionTo("Q");
                    System.out.println("MISE A JOUR: PROMOTION — Pion -> Reine");
                }
            }

            echiquier.syncFromPlateau(plateau);
            dernierCoup = c;

            if (piece instanceof Pawn && Math.abs(c.arr.x - c.dep.x) == 2) {
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

            // Vérifier si l'adversaire est en échec
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

    /** Retourne l'enregistreur PGN (pour la sauvegarde manuelle depuis l'UI). */
    public PgnRecorder getPgnRecorder() {
        return pgnRecorder;
    }

    /**
     * Réinitialise la partie sans relancer l'application.
     * Arrête le thread courant, remet le plateau à zéro et lance un nouveau thread.
     */
    public void nouvellePartie() {
        // 1. Signaler au thread courant de s'arrêter
        Thread old = gameThread;
        synchronized (this) {
            termine = true;
            nextC = null;
            notifyAll(); // Débloquer attendreCoup() s'il est en attente
        }

        // 2. Attendre que l'ancien thread soit bien terminé (max 2 s)
        if (old != null && old.isAlive()) {
            try { old.join(2000); } catch (InterruptedException ignored) {}
        }

        // 3. Réinitialiser tout l'état du jeu
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
            pgnRecorder = new PgnRecorder("Blanc", "Noir");
        }

        // 4. Enregistrer la position initiale et redessiner
        enregistrerPosition();
        setChanged();
        notifyObservers(null); // Déclenche redraw + "Tour : BLANCS" dans VC

        // 5. Lancer un nouveau thread de jeu
        gameThread = new Thread(this, "Jeu-Thread");
        gameThread.start();
        System.out.println("Nouvelle partie lancée.");
    }

    /**
     * Sauvegarde la partie au format PGN dans le dossier personnel de l'utilisateur.
     * Fichier : ~/partie_echecs.pgn
     */
    public void sauvegardePgn() {
        String path = System.getProperty("user.home") + "/partie_echecs.pgn";
        pgnRecorder.sauvegarder(path);
    }

    /**
     * Calcule la désambiguïsation SAN pour une pièce qui va se déplacer en {@code arr}.
     * Vérifie s'il existe une autre pièce du même type et couleur pouvant également
     * atteindre cette case.
     */
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
                // Est-ce que cette pièce peut atteindre `arr` ?
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

        // La colonne (file) suffit-elle à distinguer la pièce ?
        boolean fileCollision = false;
        for (Piece a : ambiguous) {
            if (a.getCase() != null && a.getCase().getY() == depFile) {
                fileCollision = true;
                break;
            }
        }
        if (!fileCollision) return String.valueOf((char) ('a' + depFile));

        // Le rang (rank) suffit-il ?
        boolean rankCollision = false;
        for (Piece a : ambiguous) {
            if (a.getCase() != null && a.getCase().getX() == depRank) {
                rankCollision = true;
                break;
            }
        }
        if (!rankCollision) return String.valueOf(8 - depRank);

        // Les deux sont nécessaires
        return String.valueOf((char) ('a' + depFile)) + (8 - depRank);
    }

    /**
     * Génère une clef unique représentant la position courante du jeu.
     * La clef encode :
     *  - les pièces présentes sur chaque case (type + couleur)
     *  - le joueur qui doit jouer
     *  - la case cible de la prise en passant (si elle existe)
     *  - les droits de roque (présence roi/tours sur cases initiales)
     */
    public String genererClePosition() {
        Plateau plateau = PlateauSingleton.INSTANCE;
        StringBuilder sb = new StringBuilder(80);

        // 1. Encodage du plateau (8×8)
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

        // 2. Joueur courant
        sb.append(joueurCourant.isBlanc() ? 'W' : 'B');
        sb.append(';');

        // 3. Cible en passant
        org.model.plateau.Case ep = plateau.getEnPassantTarget();
        if (ep != null) {
            sb.append(ep.getX()).append(':').append(ep.getY());
        } else {
            sb.append('-');
        }
        sb.append(';');

        // 4. Droits de roque (roi et tours sur leur case initiale)
        // Blanc
        org.model.plateau.Case wKing = plateau.getCase(7, 4);
        org.model.plateau.Case wRookK = plateau.getCase(7, 7);
        org.model.plateau.Case wRookQ = plateau.getCase(7, 0);
        sb.append((wKing  != null && wKing.getPiece()  instanceof org.model.piece.King  && wKing.getPiece().isBlanc())  ? 'K' : '-');
        sb.append((wRookK != null && wRookK.getPiece() instanceof org.model.piece.Rook  && wRookK.getPiece().isBlanc()) ? 'R' : '-');
        sb.append((wRookQ != null && wRookQ.getPiece() instanceof org.model.piece.Rook  && wRookQ.getPiece().isBlanc()) ? 'R' : '-');
        // Noir
        org.model.plateau.Case bKing = plateau.getCase(0, 4);
        org.model.plateau.Case bRookK = plateau.getCase(0, 7);
        org.model.plateau.Case bRookQ = plateau.getCase(0, 0);
        sb.append((bKing  != null && bKing.getPiece()  instanceof org.model.piece.King  && !bKing.getPiece().isBlanc())  ? 'k' : '-');
        sb.append((bRookK != null && bRookK.getPiece() instanceof org.model.piece.Rook  && !bRookK.getPiece().isBlanc()) ? 'r' : '-');
        sb.append((bRookQ != null && bRookQ.getPiece() instanceof org.model.piece.Rook  && !bRookQ.getPiece().isBlanc()) ? 'r' : '-');

        return sb.toString();
    }

    /** Enregistre la position courante dans l'historique. */
    private void enregistrerPosition() {
        String cle = genererClePosition();
        positionHistory.merge(cle, 1, Integer::sum);
        System.out.println("Position enregistrée (" + positionHistory.get(cle) + "x): " + cle.substring(0, Math.min(40, cle.length())) + "…");
    }

    /** Retourne true si la position courante s'est répétée 3 fois ou plus. */
    public boolean estRepetitionTriple() {
        String cle = genererClePosition();
        return positionHistory.getOrDefault(cle, 0) >= 3;
    }
}
