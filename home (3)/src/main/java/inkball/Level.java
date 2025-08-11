package inkball;

import java.util.*;

public class Level {
    private String layout;
    private int time;
    private int spawnInterval;
    private float scoreIncreaseModifier;
    private float scoreDecreaseModifier;
    private List<String> balls;

    public Level(String layout, int time, int spawnInterval, float scoreIncreaseModifier, float scoreDecreaseModifier, List<String> balls) {
        this.layout = layout;
        this.time = time;
        this.spawnInterval = spawnInterval;
        this.scoreIncreaseModifier = scoreIncreaseModifier;
        this.scoreDecreaseModifier = scoreDecreaseModifier;
        this.balls = balls;
    }

    public String getLayout() {
        return layout;
    }

    public int getTime() {
        return time;
    }

    public int getSpawnInterval() {
        return spawnInterval;
    }

    public float getScoreIncreaseModifier() {
        return scoreIncreaseModifier;
    }

    public float getScoreDecreaseModifier() {
        return scoreDecreaseModifier;
    }

    public List<String> getBalls() {
        return balls;
    }
}