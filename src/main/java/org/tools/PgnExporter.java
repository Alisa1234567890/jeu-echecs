package org.tools;
import java.util.Map;
/**
 * Produit le texte PGN standard a partir d un objet PGN.
 */
public class PgnExporter {
    public static String buildPgnText(PGN p) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : p.getTags().entrySet()) {
            sb.append('[').append(e.getKey()).append(" \"").append(e.getValue()).append("\"]\n");
        }
        sb.append('\n');
        int half = 0;
        for (int i = 0; i < p.getMoves().size(); i++) {
            if (half % 2 == 0) {
                sb.append(half / 2 + 1).append(". ");
            }
            sb.append(p.getMoves().get(i));
            String c = p.getComments().get(i);
            if (c != null && !c.isEmpty()) {
                sb.append(" {").append(c).append("} ");
            } else {
                sb.append(' ');
            }
            half++;
        }
        sb.append(p.getResult()).append('\n');
        return sb.toString();
    }
}
