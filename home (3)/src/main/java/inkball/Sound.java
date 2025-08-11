package inkball;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import processing.core.PApplet;
import java.util.*;

public class Sound {
    private Minim minim;
    private Map<String, AudioPlayer> audios;

    public Sound (PApplet app) {
        minim = new Minim(app);
        audios = new HashMap<>();
    }

    public void loadSound(String audioName, String filepath) {
        AudioPlayer ap = minim.loadFile(filepath);
        audios.put(audioName, ap);
    }

    public void play(String audioName) {
        if (audios.containsKey(audioName)) {
            audios.get(audioName).play();
            audios.get(audioName).rewind();
        }
    }
}