package org.model;

public class Case {

    private Coup nextC;

    public void set(Coup c) {
        nextC = c;
    }

    public Coup getNextC() {
        return nextC;
    }
}
