package inkball;

import processing.core.PApplet;
import processing.core.PImage;

public class Hole {
    private float x, y;
    private String color;
    private PImage sprite;

    public Hole(float x, float y, String color, PImage sprite) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.sprite = sprite;

        System.out.println(this.color + " hole created!");
    }

    public boolean isBallCaptured(Ball ball) {

        float[] holeCenter = getCenter();

        float ballCenterX = ball.getX() + (ball.getSize() / 2); // Center of the ball
        float ballCenterY = ball.getY() + (ball.getSize() / 2); // Center of the ball

        // Distance between ball center and hole center
        float distance = PApplet.dist(ballCenterX, ballCenterY, holeCenter[0], holeCenter[1]);

        if (distance < 32) {
            // Attraction force
            float attractionForceX = (holeCenter[0] - ballCenterX) * 0.005f;
            float attractionForceY = (holeCenter[1] - ballCenterY) * 0.005f;

            ball.setxVelocity(ball.getxVelocity() + attractionForceX);
            ball.setyVelocity(ball.getyVelocity() + attractionForceY);

            //Adjust size for illusion of falling into hole
            float scaleFactor = distance / 32;
            ball.setSize((int) (ball.getOriginalSize() * scaleFactor));

            if (distance <= 16) {
                return true;
            }

        } else {
            ball.setSize(ball.getOriginalSize());
        }
        return false;
    }

    public String getColor(){
        return color;
    }

    public float[] getCenter() {
        return new float[] { x + App.CELLSIZE, y + App.CELLSIZE }; // Center of the 2x2 hole sprite
    }
}
