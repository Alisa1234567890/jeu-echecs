package org.model.jeu;

import java.awt.Point;

public class Coup {

    public Point dep;
    public Point arr;
    private String type = "NORMAL"; // Type de coup

    private String pieceName    = "";
    private boolean capture     = false;
    private String disambiguation = "";
    private String promotionTo  = "Q";

    public Coup(Point dep, Point arr) {
        this.dep = dep;
        this.arr = arr;
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

