package inkball;

import processing.core.PApplet;
import org.junit.jupiter.api.Test;
import processing.core.PImage;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;


public class SampleTest {

    @Test
    public void simpleTest(){
        App app = new App();
        app.loop();
        PApplet.runSketch(new String[] { "App" }, app);
        app.delay(1000);
        app.setup();
        app.delay(10000); // delay is to give time to initialise stuff before drawing begins + also free coverage
    }

    @Test
    public void testFrameRate() { //Testing Framerate
        assertEquals(60, App.FPS);
    }

    @Test
    public void createBall(){ //Tests if ball is being created correctly
        App app = new App();
        PImage testSprite = new PImage(32, 32);

        Ball ball = new Ball(100, 200, 5.0f, testSprite, "Green", app);

        assertEquals(100, ball.x);
        assertEquals(200, ball.y);
        assertEquals("Green", ball.getColor());
    }


    @Test
    public void testAddingSegment(){ //Tests if l;ine seg,emts are added correctly
        Line line0 = new Line();

        line0.addSegment(10,20);
        line0.addSegment(20,30);
        List<float[]> segments = line0.getLineSegments();
        assertEquals(2, segments.size());
        assertArrayEquals(new float[]{10, 20}, segments.get(0));
        assertArrayEquals(new float[]{20, 30}, segments.get(1));
    }

    @Test
    public void testPointxLineSegmentTRUE(){//Test if Point on Line returns true
        Line line0 = new Line();
        line0.addSegment(0,0);
        line0.addSegment(0,100);
        assertTrue(line0.containsPoint(0, 50));
    }

    @Test
    public void testPointxLineSegmentFALSE(){//Test if Point on Line returns false
        Line line0 = new Line();
        line0.addSegment(0,0);
        line0.addSegment(0,100);
        assertFalse(line0.containsPoint(0, 150));
    }

    @Test
    public void testSpawnerGetters(){
        Spawner birthgiver = new Spawner(200, 200);
        birthgiver.getX();
        birthgiver.getY();

        assertEquals(200, birthgiver.getX());
        assertEquals(200, birthgiver.getY());
    }

    @Test
    public void testTileGetters(){
        PImage testSprite = new PImage(32, 32);
        Tile blah = new Tile (220, 220, 'X', testSprite);
        assertEquals(220, blah.getX());
        assertEquals(220, blah.getY());
    }

    @Test
    public void testHoleGetter(){

        PImage testSprite = new PImage(64, 64);
        Hole hole = new Hole(250, 0, "Blue", testSprite);

        assertEquals("Blue", hole.getColor());
    }

    @Test
    public void testBallCapture(){//Tests if ballCaptured
        App app = new App();
        PImage testHoleSprite = new PImage(64, 64);

        Hole hole = new Hole (300, 0, "Green", testHoleSprite);

        PImage testBallSprite = new PImage(32,32);
        Ball ball = new Ball(310, 10, 1.0f, testBallSprite, "Green", app);

        assertTrue(hole.isBallCaptured(ball));
    }

    @Test
    public void testBallXLineRemoval(){//Checks if line is removed after a ball collision
        App app = new App();
        PImage testBallSprite = new PImage(32,32);
        Ball ball = new Ball(50, 50, 1.0f, testBallSprite, "Green", app);

        ball.setxVelocity(1);
        ball.setyVelocity(1);

        Line line0 = new Line();
        line0.addSegment(55,55);
        line0.addSegment(75,55);

        List<Line> lines = new ArrayList<>();
        lines.add(line0);

        ball.lineCollision(lines);

        assertFalse(lines.contains(line0)); //Line should be removed after collision
    }

    @Test
    public void testPauseKey(){//Tests if pause key works
        App app = new App();

        app.keyPressed(new KeyEvent(app, 0, 0, 0, ' ', ' '));
        assertTrue(app.isPaused);

    }

    //@Test
    public void testRestartKey(){
        App app = new App();

        app.keyPressed(new KeyEvent(app, 0, 0, 0, 'r', 'r'));

        app.levelComplete = true;
        app.gameComplete = true;

        //ADD AN ASSERT - I Dont even know how to test this :(

    }

    @Test
    public void testMousePressed_DrawLine() {//Was meant to test if drawing the line worked
        App app = new App();

        //Couldnt figure out how to make this work and also ran out of time
        MouseEvent leftClick = new MouseEvent(app, MouseEvent.PRESS, 0, 0, 100, 100, 1, MouseEvent.CLICK);

        app.mousePressed(leftClick);

        assertNotNull(app.getCurrentLine());
    }

    @Test
    public void testAddRemainingTimeToScore() {
        App app = new App();
        app.levelTimer = 5;
        app.addRemainingTimeToScore();
        assertEquals(1, app.getScore()); // Score should increase by 1

        app.levelTimer = 0;
        app.addRemainingTimeToScore();
        assertTrue(app.scoreAddingComplete);
    }


    @Test
    public void testYellowTiles(){//Was meant to check if the tiles were moving but fails
        App app = new App();
        app.yellowTileX = 0;
        app.yellowTileY = 0;
        app.yellowTileX = app.BOARD_WIDTH-1;
        app.yellowTileY = app.BOARD_HEIGHT-1;

        app.moveYellowTiles();

        assertEquals(1, app.getYellowTileX());
        assertEquals(0, app.getYellowTileY());
        assertEquals(app.BOARD_WIDTH - 1, app.getYellowTile2X());
        assertEquals(app.BOARD_HEIGHT - 2, app.getYellowTile2Y());
    }

    @Test
    public void testEndGame(){
        App app = new App();

        app.gameComplete = true;

        assertTrue(app.isEnded);
    }

    @Test
    public void testLoadNextLevel() {
        App app = new App();
        app.currentLevel = 2; // Set to the last level
        app.loadNextLevel();

        assertTrue(app.gameComplete); // Check if the game is marked complete

        app.currentLevel = 0; // Reset for another test
        app.loadNextLevel(); // Load the next level
        assertFalse(app.gameComplete); // Ensure game is not complete
    }


}

// gradle run						Run the program
// gradle test						Run the testcases

// Please ensure you leave comments in your testcases explaining what the testcase is testing.
// Your mark will be based off the average of branches and instructions code coverage.
// To run the testcases and generate the jacoco code coverage report: 
// gradle test jacocoTestReport
