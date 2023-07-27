package net.sf.borg.ui.calendar;

import java.awt.*;

class DrawContext {
    Graphics2D g2;
    String text;
    int x;
    int y;
    int w;
    boolean strike;

    public DrawContext(Graphics2D g2, String text, int x, int y, int w, boolean strike) {
        this.g2 = g2;
        this.text = text;
        this.x = x;
        this.y = y;
        this.w = w;
        this.strike = strike;
    }
}

