import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

public class Transform implements Drawable, Animatable {
	public float x, y;
	public boolean restrictBounds = false;
	protected Color fill = null, outline = null;
	
	public Transform(float x, float y, Color fill, Color outline, boolean restrictBounds) {
		this.x = x;
		this.y = y;
		this.fill = fill;
		this.outline = outline;
		this.restrictBounds = restrictBounds;
	}
    
	public float vx = 0, vy = 0; // velocity
	public float tx = 0, ty = 0; // target velocity
	public float maxspeed = Float.MAX_VALUE; // max speed (for x or y)
	public float acceleration = 1; // pixels per second per second
    protected long timeStamp = System.currentTimeMillis();
	protected float deltaTime = 0f;
	
	public void spawnRandom() {
		x = Global.random(Global.WIDTH);
		y = Global.random(Global.HEIGHT);
	}
	
	public void updatePhysics() {
		long time = System.currentTimeMillis();
		deltaTime = (time - timeStamp) / 1000f;
		timeStamp = time;
        
        float a = getAcceleration();
        float s = getMaxSpeed();
		
        if (Float.compare(tx, vx) != 0 || Float.compare(ty, vy) != 0) {
            float dvx = (tx - vx) * Math.min(1f, a * deltaTime);
            float dvy = (ty - vy) * Math.min(1f, a * deltaTime);
            
            vx += dvx;
            vy += dvy;
            
			if (vx < -s) vx = -s;
			else if (vx > s) vx = s;
			if (vy < -s) vy = -s;
			else if (vy < -s) vy = s;
        }
        if (Float.compare(x, vx) != 0 || Float.compare(y, vy) != 0) {
            x += vx * deltaTime;
            y += vy * deltaTime;
        }
        
	}
	
	public void animate() {
		updatePhysics();
        if (restrictBounds) {
            x = Math.max(Math.min(x, Global.WIDTH - edgePadding), edgePadding);
            y = Math.max(Math.min(y, Global.HEIGHT - edgePadding), edgePadding);
        }
	}
    
    @Override
	public void draw(Graphics2D g) {}
	public Color getFill() { return fill; }
	public Color getOutline() { return outline; }
    public float getMaxSpeed() { return maxspeed; }
    public float getAcceleration() { return acceleration; }
	
	public static float edgePadding = 20f;
	public static float allowance = 20f;
	
    public boolean outOfBounds() {
		return
			x > Global.WIDTH + allowance || x < -allowance ||
			y > Global.HEIGHT + allowance || y < -allowance;
	}
	
	public float dist2(Transform other) {
		float dx = x - other.x;
		float dy = y - other.y;
		return dx * dx + dy * dy;
	}
    		
}