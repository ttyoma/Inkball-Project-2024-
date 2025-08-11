package inkball;

import processing.core.PApplet;
import processing.core.PImage;

public class Tile {
    private int x, y;
    private char type;
    private PImage sprite;

    public Tile(int x, int y, char type, PImage sprite) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.sprite = sprite;
    }

    public void display (PApplet app){
        if (sprite != null) {
            app.image(sprite, x, y);
        }
    }

    public char getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}