package inkball;

import processing.core.PApplet;
import processing.core.PImage;
import java.util.*;

public class Ball {
    public float x, y;
    private float xVelocity, yVelocity;
    private final float speed;
    private int size;
    private PImage sprite;
    private String color;
    private App app;
    private final int originalSize;
    private int collisionCooldown = 0;
    public boolean hasReflected = false;

    public Ball(float initX, float initY, float speed, PImage sprite, String color, App app) {
        this.x = initX; //Initial X Pos
        this.y = initY; // Initial Y Pos
        this.speed = speed;
        this.sprite = sprite;
        this.xVelocity = Math.random() < 0.5 ? -1 : 1;;
        this.yVelocity = Math.random() < 0.5 ? -1 : 1;;
        this.size = sprite.width;
        this.color = color;
        this.app = app;
        this.originalSize = sprite.width;

        System.out.println(this.color + " ball created!");
    }

    public void update() {
        x += xVelocity * speed;
        y += yVelocity * speed;

        if (collisionCooldown > 0) {
            collisionCooldown--;
        }
    }

    public void display(PApplet app) {
        app.image(sprite, x, y, size, size);
    }

    public void collisionCheck(Tile[][] tiles, List<Line> lines, List<Hole> holes) {
        if (x < 0 || x + size > App.WIDTH) {
            xVelocity *= -1;
        }
        if (y < App.TOPBAR || y + size > App.HEIGHT) {
            yVelocity *= -1;
        }

        wallCollision(tiles);

        if (lines != null){
            hasReflected = false;
            lineCollision(lines);
        }
    }

    private void wallCollision(Tile[][] tiles) {
        if (collisionCooldown > 0){
            return;
        }

        float ballRadius = (float) size / 2;
        float ballCenterX = x + ballRadius;
        float ballCenterY = y + ballRadius;

        // Determine the current column and row of the ball's center
        int currentCol = (int) (ballCenterX / App.CELLSIZE);
        int currentRow = (int) ((ballCenterY - App.TOPBAR) / App.CELLHEIGHT);

        // Checks around the ball to see if there is a collision
        for (int row = currentRow - 1; row <= currentRow + 1; row++) {
            for (int col = currentCol - 1; col <= currentCol + 1; col++) {
                if (row >= 0 && row < App.BOARD_HEIGHT && col >= 0 && col < App.BOARD_WIDTH) {
                    Tile tile = tiles[row][col];
                    if (isWall(tile)) {
                        // If the tile is a wall
                        float tileLeft = col * App.CELLSIZE;
                        float tileRight = tileLeft + App.CELLSIZE;
                        float tileTop = row * App.CELLHEIGHT + App.TOPBAR;
                        float tileBottom = tileTop + App.CELLHEIGHT;

                        // Check for collision and calculate the appropriate normal vector
                        float normalX = 0;
                        float normalY = 0;

                        // Vertical collision
                        if (ballCenterX > tileLeft && ballCenterX < tileRight) {
                            if (ballCenterY < tileTop && (tileTop - ballCenterY) <= ballRadius) {
                                normalY = -1;  // Hit from above, normal points up
                            } else if (ballCenterY > tileBottom && (ballCenterY - tileBottom) <= ballRadius) {
                                normalY = 1;   // Hit from below, normal points down
                            }

                            // Horizontal collisions
                        } else if (ballCenterY > tileTop && ballCenterY < tileBottom) {
                            if (ballCenterX < tileLeft && (tileLeft - ballCenterX) <= ballRadius) {
                                normalX = -1;  // Hit from the left, normal points left
                            } else if (ballCenterX > tileRight && (ballCenterX - tileRight) <= ballRadius) {
                                normalX = 1;   // Hit from the right, normal points right
                            }
                        }

                        // Corner collision
                        if (isIntersecting(ballCenterX, ballCenterY, tileLeft, tileTop)) { // Top-left corner
                            normalX = -1;
                            normalY = -1;
                        } else if (isIntersecting(ballCenterX, ballCenterY, tileRight, tileTop)) { // Top-right corner
                            normalX = 1;
                            normalY = -1;
                        } else if (isIntersecting(ballCenterX, ballCenterY, tileLeft, tileBottom)) { // Bottom-left corner
                            normalX = -1;
                            normalY = 1;
                        } else if (isIntersecting(ballCenterX, ballCenterY, tileRight, tileBottom)) { // Bottom-right corner
                            normalX = 1;
                            normalY = 1;
                        }

                        //Execute collision logic
                        if (normalX != 0 || normalY != 0) {
                            changeColorBasedOnWall(tile.getType());
                            reflect(normalX, normalY);
                            minVelocity(); //Ensures a minimum velocity

                            collisionCooldown = 5;

                            x += normalX * 2;
                            y += normalY * 2;
                            return;  // Exit after the first collision to prevent multiple reflections
                        }
                    }
                }
            }
        }
    }

    //Helper for corner collision
    private boolean isIntersecting(float ballCenterX, float ballCenterY, float cornerX, float cornerY) {
        float distance = (float) Math.sqrt(Math.pow(ballCenterX - cornerX, 2) + Math.pow(ballCenterY - cornerY, 2));
        float radius = (float) size/2;

        return distance < radius;
    }

    // Check if a given tile is a wall based on its type
    private boolean isWall(Tile tile) {
        char type = tile.getType();
        return type == 'X' || type == '1' || type == '2' || type == '3' || type == '4';
    }

    private void reflect(float normalX, float normalY) {
        float length = (float) Math.sqrt(normalX * normalX + normalY * normalY);
        if (length > 0) {
            normalX /= length;
            normalY /= length;
        }

        // Dot Product for trajectory calculation
        float dotProduct = (xVelocity * normalX) + (yVelocity * normalY);

        // Apply new trajectory
        xVelocity = xVelocity - 2 * dotProduct * normalX;
        yVelocity = yVelocity - 2 * dotProduct * normalY;

        // Play the bounce sound
        App.soundManager.play("bounce");
        hasReflected = true;
    }

    public void lineCollision(List<Line> lines) {
        float newX = x + xVelocity;
        float newY = y + yVelocity;

        for (Line line : lines) {
            List<float[]> segments = line.getLineSegments();
            for (int j = 0; j < segments.size() - 1; j++) {
                float[] P1 = segments.get(j);
                float[] P2 = segments.get(j + 1); //Adjacent line segments

                // Calculate distances
                float distP1Ball = distance(P1[0], P1[1], newX, newY);
                float distP2Ball = distance(P2[0], P2[1], newX, newY);
                float distP1P2 = distance(P1[0], P1[1], P2[0], P2[1]);

                if (distP1Ball + distP2Ball < distP1P2 + size/2) {
                    // Reflect the ball off the line
                    reflectOffLine(P1, P2);

                    //Remove line
                    lines.remove(line);
                    return;
                }
            }
        }
    }

    //Helper for calculating distance
    private float distance(float Ax, float Ay, float Bx, float By) {
        return (float) Math.sqrt(Math.pow(Bx - Ax, 2) + Math.pow(By - Ay, 2));
        }

    private void reflectOffLine(float[] start, float[] end) {
        float dx = end[0] - start[0];
        float dy = end[1] - start[1];
        float magnitude = (float) Math.sqrt(dx * dx + dy * dy);

        float normalX = -dy / magnitude;
        float normalY = dx / magnitude;

        reflect(normalX, normalY);
    }

    private void changeColorBasedOnWall(char wallType) {
        PImage newSprite = null;
        switch (wallType) {
            case '1':  // Orange wall
                newSprite = app.loadImage("inkball/ball1.png");
                this.color = "orange";
                break;
            case '2':  // Blue wall
                newSprite = app.loadImage("inkball/ball2.png");
                this.color = "blue";
                break;
            case '3':  // Green wall
                newSprite = app.loadImage("inkball/ball3.png");
                this.color = "green";
                break;
            case '4':  // Yellow wall
                newSprite = app.loadImage("inkball/ball4.png");
                this.color = "yellow";
                break;
            default:   // Keep the current color for other walls
                break;
        }
        if (newSprite != null){
            sprite = newSprite;
        }
    }

    private void minVelocity() {
        float minSpeed = 1.0f;
        float speed = (float) Math.sqrt(xVelocity * xVelocity + yVelocity * yVelocity);
        if (speed < minSpeed) {
            float scale = minSpeed / speed;
            xVelocity *= scale;
            yVelocity *= scale;
        }
    }

    public int getOriginalSize() {
        return originalSize;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getSize() {
        return size;
    }

    public String getColor() {
        return color;
    }

    public void setxVelocity(float xVelocity) {
        this.xVelocity = xVelocity;
    }

    public void setyVelocity(float yVelocity) {
        this.yVelocity = yVelocity;
    }

    public void setSize(int newSize){
        this.size = newSize ;
    }

    public float getxVelocity(){
        return this.xVelocity;
    }

    public float getyVelocity(){
        return this.yVelocity;
    }
}