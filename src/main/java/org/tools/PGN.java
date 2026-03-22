package org.tools;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Representation en memoire d un jeu PGN.
 * Contient les tags (headers) et la liste des coups SAN avec commentaires optionnels.
 */
@SuppressWarnings("unused")
public class PGN {
    private final Map<String, String> tags = new LinkedHashMap<>();
    private final List<String> moves = new ArrayList<>();
    private final List<String> comments = new ArrayList<>();
    public void setTag(String name, String value) { tags.put(name, value); }
    public String getTag(String name) { return tags.get(name); }
    public Map<String, String> getTags() { return tags; }
    public void addMove(String san) { addMove(san, ""); }
    public void addMove(String san, String comment) {
        moves.add(san);
        comments.add(comment == null ? "" : comment);
    }
    public List<String> getMoves()    { return moves; }
    public List<String> getComments() { return comments; }
    public String getResult()         { return tags.getOrDefault("Result", "*"); }
}
