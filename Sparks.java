import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.nio.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.swing.*;

public class Sparks implements Drawable, Animatable {
    
    public static void main(String[] args) {
        
        JFrame frame = new JFrame("Sparks test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(Global.WIDTH, Global.HEIGHT);
        
        Game game = new Game();
        frame.add(game);
        
        frame.pack();
        frame.setVisible(true);
        
        game.repainter.start();
        game.animator.start();
        
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            int x = sc.nextInt();
            int y = sc.nextInt();
            game.addLayer(new Sparks(x, y, 300));
        }
        
        
    }
    
    Transform[] T;
    
    public Sparks(float x, float y, int numSparks) {
        T = new Transform[numSparks];
        for (int i = 0; i < numSparks; ++i) {
            T[i] = new Transform(x, y,
            Global.transparency(new Color(Global.random(0x1000000)), 0.5f), null, false) {
				@Override
				public void animate() {
					super.animate();
					if (Math.abs(vx) < 10 && Math.abs(vy) < 10) {
						fill = Global.transparency(fill, 1 - deltaTime);
					}
				}
			};
            T[i].maxspeed = Global.random(20, 250);
            T[i].acceleration = Global.random(0.1f, 20f);
            double angle = Math.toRadians(Global.random(360));
            T[i].vx = (float) (T[i].maxspeed * Math.cos(angle));
            T[i].vy = (float) (T[i].maxspeed * Math.sin(angle));
            T[i].tx = 0;
            T[i].ty = 0;
        }
    }
    
    @Override
    public void draw(Graphics2D g) {
        for (Transform t : T) {
            float radius = 4;
            g.setColor(t.getFill());
            g.fillOval((int) (t.x - radius), (int) (t.y - radius), (int) (radius * 2), (int) (radius * 2));
        }
    }
    
    @Override
    public void animate() {
        for (Transform t : T) {
            t.animate();
        }
    }
    
}
