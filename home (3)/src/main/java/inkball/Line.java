package inkball;

import processing.core.PApplet;
import java.util.*;

public class Line {
    private List<float[]> lineSegments;

    public Line(){
        lineSegments = new ArrayList<>();
    }

    public void addSegment(float x, float y){
        lineSegments.add(new float[]{x, y});
    }

    public void display(PApplet app) {

        app.pushStyle();
        app.stroke(0);  // Line color set to black
        app.strokeWeight(10);  //As per assignment detes
        for (int i = 0; i < lineSegments.size() - 1; i++) {
            float[] start = lineSegments.get(i);
            float[] end = lineSegments.get(i + 1);
            app.line(start[0], start[1], end[0], end[1]);
        }
        app.popStyle();
    }

    //Check if a point p is on the line segment (So that it can be removed by right-clicking)
    public boolean containsPoint(float px, float py) {
        for (int i = 0; i < lineSegments.size() - 1; i++) {
            float[] start = lineSegments.get(i);
            float[] end = lineSegments.get(i + 1);

            if(pointNearLineSegment(px, py, start[0], start[1], end[0], end[1])){
                return true;
            }
        }
        return false;
    }

    private boolean pointNearLineSegment(float px, float py, float x1, float y1, float x2, float y2) {
        // Calculate the distances
        float distP1ToP = distance(px, py, x1, y1);  // dist from point -> start of segment
        float distP2ToP = distance(px, py, x2, y2);  // dist from point -> end of segment
        float distP1ToP2 = distance(x1, y1, x2, y2); // segment distance

        return distP1ToP + distP2ToP <= distP1ToP2 + 10; //Allows player to click within 10 px of the line to remove
    }

    public List<float[]> getLineSegments() {
        return lineSegments;
    }

    //Helper for calculating distance
    private float distance(float Ax, float Ay, float Bx, float By) {
        return (float) Math.sqrt((Bx - Ax) * (Bx - Ax) + (By - Ay) * (By - Ay));
    }
}
