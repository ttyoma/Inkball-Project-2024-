package inkball;

import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.*;

public class App extends PApplet {
    //Basic Setup
    public static final int CELLSIZE = 32;
    public static final int CELLHEIGHT = 32;
    public static final int TOPBAR = 64;
    public static int WIDTH = 576;
    public static int HEIGHT = 640;
    public static final int BOARD_WIDTH = WIDTH/CELLSIZE;
    public static final int BOARD_HEIGHT = (HEIGHT - TOPBAR)/CELLSIZE;
    public static final int FPS = 60;

    //Level Setup
    public String configPath;
    private Tile[][] tiles;
    private List<Level> levels;
    public int currentLevel;

    //Ball Queue
    private List<String> ballsToSpawn;
    private int spawnTimer;
    private int displayableBalls = 5;
    private boolean ballSpawned = false;
    private int queueShiftAmount = 0;
    private final int maxShiftAmount = 29;
    private boolean shiftComplete = false;  // Track when the shifting is done
    private int blinkTimer = 0;
    private boolean isBlinkingVisible = true;

    //Game Objects
    private List<Ball> balls;
    private List<Line> lines;
    public Line currentLine;
    private List<Hole> holes;
    private List<Spawner> spawners;

    //Score and Logic
    private int preLevelScore;
    public int score = 0;
    public float levelTimer = 0;
    public float scoreIncreaseModifier;
    public float scoreDecreaseModifier;
    private Map<String, Integer> scoreIncrease = new HashMap<>();
    private Map<String, Integer> scoreDecrease = new HashMap<>();
    public boolean isPaused = false;
    public boolean isEnded = false;
    public boolean levelComplete = false;
    public boolean gameComplete = false;
    public int yellowTileX = 0;
    public int yellowTileY = 0;
    public int yellowTile2X = BOARD_WIDTH-1;
    public int yellowTile2Y = BOARD_HEIGHT-1;
    public boolean scoreAddingComplete = false;

    //Sound & GUI improvements extension
    public static Sound soundManager;
    private boolean levelCompleteSound = false;
    public boolean timesUpMessage = false;
    int timeAtZeroCounter = 0;

    public App() {
        this.levels = new ArrayList<>();
        this.currentLevel = 0;
        this.configPath = "config.json";
    }

	@Override
    public void settings() {
        size(WIDTH, HEIGHT);
    }

	@Override
    public void setup() {
        frameRate(FPS);
        loadConfig("config.json");

        balls = new ArrayList<>();
        lines = new ArrayList<>();
        holes = new ArrayList<>();
        spawners = new ArrayList<>();

        loadLevel(currentLevel);

        soundManager = new Sound(this);
        soundManager.loadSound("bounce", "inkball/bounce.wav");
        soundManager.loadSound("spawnBalls", "inkball/BallSpawn.wav");
        soundManager.loadSound("levelComplete", "inkball/LevelComplete.wav");
        soundManager.loadSound("correctHole", "inkball/CorrectHole.wav");
        soundManager.loadSound("timesUp", "inkball/TimesUp.wav");
        soundManager.loadSound("wrongHole", "inkball/WrongHole.wav");
    }

    private void loadConfig(String filename) {
        JSONObject config = loadJSONObject(filename);
        JSONArray levelsArray = config.getJSONArray("levels");

        for (int i = 0; i < levelsArray.size(); i++) {
            JSONObject levelData = levelsArray.getJSONObject(i);
            String layout = levelData.getString("layout");
            int time = levelData.getInt("time");
            int spawnInterval = levelData.getInt("spawn_interval");
            float scoreIncreaseModifier = levelData.getFloat("score_increase_from_hole_capture_modifier");
            float scoreDecreaseModifier = levelData.getFloat("score_decrease_from_wrong_hole_modifier");
            JSONArray ballsArray = levelData.getJSONArray("balls");
            List<String> balls = new ArrayList<>();

            for (int j = 0; j < ballsArray.size(); j++) {
                balls.add(ballsArray.getString(j));
            }

            //Score Increase Multipliers
            JSONObject scoreIncreaseData = config.getJSONObject("score_increase_from_hole_capture");
            scoreIncrease.put("grey", scoreIncreaseData.getInt("grey"));
            scoreIncrease.put("orange", scoreIncreaseData.getInt("orange"));
            scoreIncrease.put("blue", scoreIncreaseData.getInt("blue"));
            scoreIncrease.put("green", scoreIncreaseData.getInt("green"));
            scoreIncrease.put("yellow", scoreIncreaseData.getInt("yellow"));

            //Score Decrease Multipliers
            JSONObject scoreDecreaseData = config.getJSONObject("score_decrease_from_wrong_hole");
            scoreDecrease.put("grey", scoreDecreaseData.getInt("grey"));
            scoreDecrease.put("orange", scoreDecreaseData.getInt("orange"));
            scoreDecrease.put("blue", scoreDecreaseData.getInt("blue"));
            scoreDecrease.put("green", scoreDecreaseData.getInt("green"));
            scoreDecrease.put("yellow", scoreDecreaseData.getInt("yellow"));

            Level level = new Level(layout, time, spawnInterval, scoreIncreaseModifier, scoreDecreaseModifier, balls);
            levels.add(level);
        }
    }

	@Override
    public void keyPressed(KeyEvent e){
        if (e.getKey() == ' ') {
            isPaused = !isPaused;
        }
        if (e.getKey() == 'r'){
            if (levelComplete && gameComplete){
                restartGame();
                System.out.println("Game Restarting!");
            } else if (!levelComplete) {
                restartLevel();
                System.out.println("Level Restarting!");
                }
            }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(!levelComplete){
            if (e.getButton() == LEFT) {
                if (e.isControlDown()) {
                    // Remove Line (Ctrl + Left)
                    Iterator<Line> lineIterator = lines.iterator();
                    while (lineIterator.hasNext()) {
                        Line line = lineIterator.next();
                        if (line.containsPoint(e.getX(), e.getY())) {
                            lineIterator.remove();
                            break;
                        }
                    }
                } else {
                    //Start drawing line
                    currentLine = new Line();
                    currentLine.addSegment(e.getX(), e.getY());
                    lines.add(currentLine);
                }
            } else if (e.getButton() == RIGHT) {
                // Remove Line (Normal)
                Iterator<Line> lineIterator = lines.iterator();
                while (lineIterator.hasNext()) {
                    Line line = lineIterator.next();
                    if (line.containsPoint(e.getX(), e.getY())) {
                        lineIterator.remove();
                        break;
                    }
                }
            }
        }
    }

	@Override
    public void mouseDragged(MouseEvent e) {
        //Continues the current line
        if (e.getButton() == LEFT && currentLine != null) {
            currentLine.addSegment(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        //Stop drawing line
        if (e.getButton() == LEFT && currentLine != null) {
            currentLine = null;
        }
    }

	@Override
    public void draw() {
        background(0);

        fill(0);
        rect(0,0, WIDTH, TOPBAR);

        fill(255);
        textSize(20);
        text("Score: " + score, 450, 30);

        if (levelTimer > 0){
            if (!isPaused){
                levelTimer -= 1.0/FPS;
                timeAtZeroCounter = 0;
            }
        }else{
            if (!gameComplete){
                timeAtZeroCounter++;
                if ((timeAtZeroCounter >= 3) && !timesUpMessage){ //When the level is won and the remaining time is converted to score, there is 1 frame where the timer hits 0, thus the times up sound plays. Hence I have it only play when the timer has been at 0 for more than 1 frame
                    System.out.println("Times Up !");
                    soundManager.play("timesUp");
                    timesUpMessage = true;
                }
                isEnded = true;
                fill(255);
                textSize(24);
                text("Time's Up!", 250, 35);

                fill(255);
                textSize(14);
                text("Press R to restart!", 250, 55);
            }
        }
        text("Timer: " +  (int) levelTimer, 450, 55);

        if (gameComplete){
            fill(255);
            textSize(24);
            text("=== ENDED ===", 250, 40);
        }

        displayQueue();

        //Draw the Grid
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (tiles[i][j] != null) {
                    tiles[i][j].display(this);
                }
            }
        }

        //Draw Ball Objects
        for (Ball ball : balls) {
            ball.display(this);
        }

        //Draw Line Objects (When level not complete)
        if (!isEnded){
            for (Line line : lines) {
                line.display(this);
            }

            if (!isPaused) {
                Iterator<Ball> ballIterator = balls.iterator();
                while (ballIterator.hasNext()) {
                    Ball ball = ballIterator.next();

                    for (Hole hole : holes) {
                        if (hole.isBallCaptured(ball)) {
                            updateScore(ball, hole);
                            System.out.println("Ball Captured by Hole!");

                            ballIterator.remove();
                            break;
                        }
                    }
                }

                //Ball Logic (Only happens when game is in play)
                for (Ball ball : balls) {
                    ball.update();
                    ball.collisionCheck(tiles, lines, holes);
                }

                //Ball Queue Anim Logic
                if (ballSpawned){
                    queueShiftAmount++;
                    if (queueShiftAmount > maxShiftAmount){
                        queueShiftAmount = 0;
                        ballSpawned = false;
                        shiftComplete = true;
                    }
                }
                if (shiftComplete && !ballsToSpawn.isEmpty()){
                    ballsToSpawn.remove(0);
                    shiftComplete = false;
                }

                spawnBalls();

                if (balls.isEmpty() && ballsToSpawn.isEmpty()){
                    levelComplete = true;
                }

            } else {
                fill(0, 150);
                rect(0, 0, WIDTH, HEIGHT);

                fill(255);
                textSize(24);
                text("*** PAUSED ***", 250, 45);
            }
        }

        if (levelComplete){
            if (!levelCompleteSound && !gameComplete){
                soundManager.play("levelComplete");
                levelCompleteSound = true;
            }

            if (!scoreAddingComplete){
                lines.clear();
                addRemainingTimeToScore();
                moveYellowTiles();
            } else {
                loadNextLevel();
                levelCompleteSound = false;
            }

            PImage yellowWall = loadImage("inkball/wall4.png");
            image(yellowWall, yellowTileX * CELLSIZE, yellowTileY * CELLHEIGHT + TOPBAR, CELLSIZE, CELLHEIGHT);
            image(yellowWall, yellowTile2X * CELLSIZE, yellowTile2Y * CELLHEIGHT + TOPBAR, CELLSIZE, CELLHEIGHT);
        }
    }



    private void displayQueue() {
        fill(0);
        PImage ballHolder = loadImage("inkball/queue.png");
        image(ballHolder, 2, 10);

            //Display the ball queue
            for (int i = 0; i < Math.min(ballsToSpawn.size(), displayableBalls); i++) {
                String color = ballsToSpawn.get(i);
                PImage ballSprite = getBallSprite(color);

                boolean isLastBallBlinking = (i == 0 && spawnTimer <= FPS);
                if (!isLastBallBlinking || isBlinkingVisible || ballSpawned) {
                    int xPos = 20 + i * (24 + 5) - queueShiftAmount;

                    if (xPos >= 20){
                        image(ballSprite, xPos, 20, 24, 24);
                    }
                }
        }
        // Countdown timer
        fill(255);
        textSize(16);
        float countdown = spawnTimer/ (float)FPS;

        text(String.format("%.1f", countdown), 200, 35);
    }

    private void spawnBalls() {

        if (!ballsToSpawn.isEmpty()) {
            spawnTimer--;

            //Next-to-spawn ball blinking logic (It looks cool)
            if (spawnTimer <= FPS) {
                blinkTimer++;
                if (blinkTimer % 12 == 0) {
                    isBlinkingVisible = !isBlinkingVisible;
                }
            }
        } else {
            spawnTimer = 0;
        }

        if (spawnTimer <= 0 && !ballsToSpawn.isEmpty()) {
            // Reset the timer as per lvl config
            spawnTimer = levels.get(currentLevel).getSpawnInterval() * FPS;

            blinkTimer = 0;
            isBlinkingVisible = true;

            //Choose a random spawner
            Spawner randomSpawner = spawners.get((int) random(spawners.size()));

            // Spawn the next ball at the randomly chosen spawner
            String ballColor = ballsToSpawn.get(0);
            PImage ballSprite = getBallSprite(String.valueOf(ballColor));
            Ball newBall = new Ball(randomSpawner.getX(), randomSpawner.getY(), 2, ballSprite, ballColor, this);
            balls.add(newBall);

            queueShiftAmount = 0;
            ballSpawned = true;
            soundManager.play("spawnBalls");
            shiftComplete = false;
        }
    }

    public void loadLevel(int currentLevel) {
        if (currentLevel >= 0 && currentLevel < levels.size()) {
            Level level = levels.get(currentLevel);
            String layoutFile = level.getLayout();
            System.out.println("Now playing - Level " + currentLevel + "!");


            tiles = new Tile[BOARD_WIDTH][BOARD_HEIGHT];
            balls.clear();
            lines.clear();
            holes.clear();
            spawners.clear();

            preLevelScore = score;
            levelTimer = level.getTime();
            spawnTimer = level.getSpawnInterval() * FPS;
            ballsToSpawn = new ArrayList<>(level.getBalls());

            String[] lines = loadStrings(layoutFile);
            loadTiles(lines);

            scoreIncreaseModifier = level.getScoreIncreaseModifier();
            scoreDecreaseModifier = level.getScoreDecreaseModifier();
        }
    }

    public void loadTiles(String[] lines) {
        tiles = new Tile[BOARD_HEIGHT][BOARD_WIDTH];
        PImage greywall = loadImage("inkball/wall0.png");
        PImage orangeWall = loadImage("inkball/wall1.png");
        PImage blueWall = loadImage("inkball/wall2.png");
        PImage greenWall = loadImage("inkball/wall3.png");
        PImage yellowWall = loadImage("inkball/wall4.png");
        PImage greyhole = loadImage("inkball/hole0.png");
        PImage orangeHole = loadImage("inkball/hole1.png");
        PImage blueHole = loadImage("inkball/hole2.png");
        PImage greenHole = loadImage("inkball/hole3.png");
        PImage yellowHole = loadImage("inkball/hole4.png");
        PImage birthgiver = loadImage("inkball/entrypoint.png");
        PImage ball = loadImage("inkball/ball0.png");
        PImage orangeBall = loadImage("inkball/ball1.png");
        PImage blueBall = loadImage("inkball/ball2.png");
        PImage greenBall = loadImage("inkball/ball3.png");
        PImage yellowBall = loadImage("inkball/ball4.png");
        PImage tile = loadImage("inkball/tile.png");

        for (int i = 0; i < lines.length; i++) {
            for (int j = 0; j < lines[i].length(); j++) {
                char tileType = lines[i].charAt(j);
                switch (tileType) {
                    case 'X':  // Grey Wall
                        tiles[i][j] = new Tile(j * CELLSIZE, i * CELLSIZE + TOPBAR, 'X', greywall);
                        break;
                    case '1':  // Orange Wall
                        tiles[i][j] = new Tile(j * CELLSIZE, i * CELLSIZE + TOPBAR, '1', orangeWall);
                        break;
                    case '2': //Blue Wall
                        tiles[i][j] = new Tile(j * CELLSIZE, i * CELLSIZE + TOPBAR, '2', blueWall);
                        break;
                    case '3': //Green Wall
                        tiles[i][j] = new Tile(j * CELLSIZE, i * CELLSIZE + TOPBAR, '3', greenWall);
                        break;
                    case '4'://Yellow Wall
                        tiles[i][j] = new Tile(j * CELLSIZE, i * CELLSIZE + TOPBAR, '4', yellowWall);
                        break;
                    case 'S'://Spawner
                        tiles[i][j] = new Tile(j * CELLSIZE, i * CELLSIZE + TOPBAR, 'S', birthgiver);
                        spawners.add(new Spawner(j * CELLSIZE, i * CELLSIZE + TOPBAR));
                        break;
                    case 'H':
                        if (j + 1 < lines[i].length()) {
                            char holeColor = lines[i].charAt(j + 1);
                            String color = getColorString(String.valueOf(holeColor));
                            PImage holeSprite = getHoleSprite(holeColor);

                            Hole hole = new Hole(j * CELLSIZE, i * CELLHEIGHT + TOPBAR, color, holeSprite);
                            holes.add(hole);

                            //Here we are 'reserving' the space for the 2x2 with type O for occupied (I was thinking T for taken, but that might cause confusion with T for tile)
                            tiles[i][j] = new Tile(j * CELLSIZE, i * CELLHEIGHT + TOPBAR, 'H', holeSprite);
                            tiles[i][j + 1] = new Tile((j + 1) * CELLSIZE, i * CELLHEIGHT + TOPBAR, 'O', null);
                            tiles[i + 1][j] = new Tile(j * CELLSIZE, (i + 1) * CELLHEIGHT + TOPBAR, 'O', null);
                            tiles[i + 1][j + 1] = new Tile((j + 1) * CELLSIZE, (i + 1) * CELLHEIGHT + TOPBAR, 'O', null);

                            j++;
                        }
                        break;
                    case 'B':
                        if (j + 1 < lines[i].length()) {
                            char ballColor = lines[i].charAt(j + 1);
                            PImage ballSprite;
                            String color = getColorString(String.valueOf(ballColor));
                            switch (ballColor) {
                                case '1':
                                    ballSprite = orangeBall;
                                    break;
                                case '2':
                                    ballSprite = blueBall;
                                    break;
                                case '3':
                                    ballSprite = greenBall;
                                    break;
                                case '4':
                                    ballSprite = yellowBall;
                                    break;
                                default:
                                    ballSprite = ball;
                                    break;
                            }

                            //This spawns a tile sprite just underneath the ball spawn so that there isn't just a white hole
                            tiles[i][j] = new Tile(j * CELLSIZE, i * CELLHEIGHT + TOPBAR, 'T', tile); //Spawns a tile on 'B' from Bn
                            tiles[i][j + 1] = new Tile((j + 1) * CELLSIZE, i * CELLHEIGHT + TOPBAR, 'T', tile); //Spawns a tile on 'n' from Bn

                            Ball newBall = new Ball(j * CELLSIZE, i * CELLHEIGHT + TOPBAR, 2, ballSprite, color, this);
                            balls.add(newBall);

                            j++;
                        }
                        break;

                    default://Empty spaces turn into tiles
                        if (tiles[i][j] == null || tiles[i][j].getType() != 'O') {  // Skip if part of an occupied hole
                            tiles[i][j] = new Tile(j * CELLSIZE, i * CELLSIZE + TOPBAR, 'T', tile);
                        }
                        break;
                }
            }
        }
    }

    private void updateScore(Ball ball, Hole hole) {
        String ballColor = ball.getColor();
        String holeColor = hole.getColor();

        if (ballColor.equals("grey") || holeColor.equals("grey")){
            score += (int)(scoreIncrease.get(ballColor) * scoreIncreaseModifier);
            soundManager.play("correctHole");
            System.out.println("Universal Grey! Score increased!");
        }
        else if (ballColor.equals(holeColor)) {
            // Increase score if the ball enters the correct hole
            score += (int)(scoreIncrease.get(ballColor) * scoreIncreaseModifier);
            soundManager.play("correctHole");
            System.out.println("Nice! Score increased!");
        } else {
            // Decrease score if the ball enters the wrong hole
            score -= (int)(scoreDecrease.get(ballColor) * scoreDecreaseModifier);
            ballsToSpawn.add(ballColor); //Whatever the color of the ball is before incorrect sink
            soundManager.play("wrongHole");
            System.out.println("Oops! Ball re-added to queue: " + ballColor);
        }
    }

    public void addRemainingTimeToScore() {
        if (levelTimer > 0) {
            score += 1;
            levelTimer -= 0.067;
        }
        else {
            scoreAddingComplete = true;
        }
    }

    public void moveYellowTiles() {

        // Move the top-left yellow tile
        if (yellowTileY == 0 && yellowTileX < BOARD_WIDTH - 1) {  // Top row, moving right
            yellowTileX++;
        } else if (yellowTileX == BOARD_WIDTH - 1 && yellowTileY < BOARD_HEIGHT - 1) {  // Right column, moving down
            yellowTileY++;
        } else if (yellowTileY == BOARD_HEIGHT - 1 && yellowTileX > 0) {  // Bottom row, moving left
            yellowTileX--;
        } else if (yellowTileX == 0 && yellowTileY > 0) {  // Left column, moving up
            yellowTileY--;
        }

        // Move the bottom-right yellow tile (Same thing, but order is changed)
        if (yellowTile2Y == BOARD_HEIGHT - 1 && yellowTile2X > 0) {  // Bottom row, moving left
            yellowTile2X--;
        } else if (yellowTile2X == 0 && yellowTile2Y > 0) {  // Left column, moving up
            yellowTile2Y--;
        } else if (yellowTile2Y == 0 && yellowTile2X < BOARD_WIDTH - 1) {  // Top row, moving right
            yellowTile2X++;
        } else if (yellowTile2X == BOARD_WIDTH - 1 && yellowTile2Y < BOARD_HEIGHT - 1) {  // Right column, moving down
            yellowTile2Y++;
        }
    }

    public void loadNextLevel() {
        currentLevel++;
        if (currentLevel < levels.size()) {
            loadLevel(currentLevel);
            levelComplete = false;
            isEnded = false;
            scoreAddingComplete = false;
            yellowTileX = 0;
            yellowTileY = 0;
            yellowTile2X = BOARD_WIDTH - 1;
            yellowTile2Y = BOARD_HEIGHT - 1;
        } else {
            gameComplete = true;
        }
    }

    private void restartLevel(){
        score = preLevelScore;
        isEnded = false;
        timesUpMessage = false;
        loadLevel(currentLevel);
    }

    private void restartGame(){
        score = 0;
        currentLevel = 0;
        levelComplete = false;
        gameComplete = false;
        isEnded = false;
        loadLevel(currentLevel);
    }

    //GETTERS
    private PImage getBallSprite(String ballColor) {
        switch (ballColor) {
            case "orange":
                return loadImage("inkball/ball1.png");
            case "blue":
                return loadImage("inkball/ball2.png");
            case "green":
                return loadImage("inkball/ball3.png");
            case "yellow":
                return loadImage("inkball/ball4.png");
            default:
                return loadImage("inkball/ball0.png");
        }
    }

    private PImage getHoleSprite(char holeColor) {
        switch (holeColor) {
            case '1':
                return loadImage("inkball/hole1.png");
            case '2':
                return loadImage("inkball/hole2.png");
            case '3':
                return loadImage("inkball/hole3.png");
            case '4':
                return loadImage("inkball/hole4.png");
            default:
                return loadImage("inkball/hole0.png");
        }
    }

    private String getColorString(String ballColor) {
        switch (ballColor) {
            case "1": return "orange";
            case "2": return "blue";
            case "3": return "green";
            case "4": return "yellow";
            default:  return "grey";
        }
    }

    public Line getCurrentLine(){
        return currentLine;
    }

    public int getScore() {
        return score;
    }

    public static void main(String[] args) {
        PApplet.main("inkball.App");
    }

    public int getYellowTileX() {
        return yellowTileX;
    }

    public int getYellowTileY() {
        return yellowTileY;
    }
    public int getYellowTile2X() {
        return yellowTile2X;
    }
    public int getYellowTile2Y() {
        return yellowTile2Y;
    }
}
