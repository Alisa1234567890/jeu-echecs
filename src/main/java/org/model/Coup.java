package org.model;

import java.awt.Point;

public class Coup {

    public Point dep;
    public Point arr;
    private String type = "NORMAL"; // Type de coup

    // Champs pour la notation PGN (SAN)
    private String pieceName    = "";   // ex. "Pawn", "Knight", "King"…
    private boolean capture     = false;
    private String disambiguation = ""; // "", "a", "3", "a3"
    private String promotionTo  = "Q"; // toujours Reine pour l'instant

    public Coup(Point dep, Point arr) {
        this.dep = dep;
        this.arr = arr;
    }

    public Coup(Point dep, Point arr, String type) {
        this.dep = dep;
        this.arr = arr;
        this.type = type;
    }

    public void setType(String type)                  { this.type = type; }
    public String getType()                           { return type; }

    public void setPieceName(String pieceName)        { this.pieceName = pieceName; }
    public String getPieceName()                      { return pieceName; }

    public void setCapture(boolean capture)           { this.capture = capture; }
    public boolean isCapture()                        { return capture; }

    public void setDisambiguation(String disambiguation) { this.disambiguation = disambiguation; }
    public String getDisambiguation()                 { return disambiguation; }

    public void setPromotionTo(String promotionTo)    { this.promotionTo = promotionTo; }
    public String getPromotionTo()                    { return promotionTo; }


    @Override
    public String toString() {
        String typeStr = "";
        if ("PRISE EN PASSANT".equals(type)) {
            typeStr = " [PRISE EN PASSANT]";
        } else if ("ROQUE".equals(type)) {
            typeStr = " [ROQUE]";
        } else if ("PROMOTION".equals(type)) {
            typeStr = " [PROMOTION]";
        } else if ("ECHEC".equals(type)) {
            typeStr = " [ÉCHEC]";
        } else if ("ECHEC ET MAT".equals(type)) {
            typeStr = " [ÉCHEC ET MAT]";
        } else if ("PAT".equals(type)) {
            typeStr = " [PAT]";
        }
        return "Coup(" + dep + " -> " + arr + ")" + typeStr;
    }
}

