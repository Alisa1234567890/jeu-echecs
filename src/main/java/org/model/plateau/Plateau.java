package org.model.plateau;

import org.model.piece.Bishop;
import org.model.piece.King;
import org.model.piece.Knight;
import org.model.piece.Pawn;
import org.model.piece.Piece;
import org.model.piece.Queen;
import org.model.piece.Rook;

import java.awt.Point;

public class Plateau {

    private final int size = 8;
    private final Case[][] cases;

    public Plateau() {
        cases = new Case[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                cases[x][y] = new Case(x, y);
            }
        }
    }

    public Case getCase(int x, int y) {
        if (x < 0 || x >= size || y < 0 || y >= size) return null;
        return cases[x][y];
    }

    public Case getCase(Point p) {
        return getCase(p.x, p.y);
    }

    public boolean canMove(Point dep, Point arr) {
        Case cDep = getCase(dep);
        Case cArr = getCase(arr);
        if (cDep == null || cArr == null) return false;
        if (dep.equals(arr) || cDep.isEmpty()) return false;

        Piece piece = cDep.getPiece();
        Piece target = cArr.getPiece();
        if (target != null && target.isBlanc() == piece.isBlanc()) {
            return false;
        }

        int dx = arr.x - dep.x;
        int dy = arr.y - dep.y;

        if (piece instanceof Pawn) {
            return canMovePawn(piece, dep, dx, dy, target);
        }
        if (piece instanceof Knight) {
            return (Math.abs(dx) == 2 && Math.abs(dy) == 1) || (Math.abs(dx) == 1 && Math.abs(dy) == 2);
        }
        if (piece instanceof Bishop) {
            return Math.abs(dx) == Math.abs(dy) && isPathClear(dep, arr);
        }
        if (piece instanceof Rook) {
            return (dx == 0 || dy == 0) && isPathClear(dep, arr);
        }
        if (piece instanceof Queen) {
            boolean straight = dx == 0 || dy == 0;
            boolean diagonal = Math.abs(dx) == Math.abs(dy);
            return (straight || diagonal) && isPathClear(dep, arr);
        }
        if (piece instanceof King) {
            return Math.abs(dx) <= 1 && Math.abs(dy) <= 1;
        }
        return false;
    }

    public boolean deplacer(Point dep, Point arr) {
        Case cDep = getCase(dep);
        Case cArr = getCase(arr);
        if (cDep == null || cArr == null || !canMove(dep, arr)) return false;
        Piece p = cDep.getPiece();
        cDep.setPiece(null);
        cArr.setPiece(p);
        return true;
    }

    private boolean canMovePawn(Piece piece, Point dep, int dx, int dy, Piece target) {
        int direction = piece.isBlanc() ? -1 : 1;
        int startRow = piece.isBlanc() ? 6 : 1;

        if (dy == 0) {
            if (dx == direction && target == null) {
                return true;
            }
            if (dep.x == startRow && dx == 2 * direction && target == null) {
                Case intermediate = getCase(dep.x + direction, dep.y);
                return intermediate != null && intermediate.isEmpty();
            }
            return false;
        }

        return Math.abs(dy) == 1 && dx == direction && target != null && target.isBlanc() != piece.isBlanc();
    }

    private boolean isPathClear(Point dep, Point arr) {
        int stepX = Integer.compare(arr.x, dep.x);
        int stepY = Integer.compare(arr.y, dep.y);
        int x = dep.x + stepX;
        int y = dep.y + stepY;
        while (x != arr.x || y != arr.y) {
            Case current = getCase(x, y);
            if (current == null || !current.isEmpty()) {
                return false;
            }
            x += stepX;
            y += stepY;
        }
        return true;
    }
}
