import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

interface Drawable {
	public void draw(Graphics2D g);
}

interface Animatable {
	public void animate();
}

public interface Design extends Drawable {
	public void draw(Graphics2D g);
	public Polygon collider();
    
    public class Turret implements Design {
        public Ship ship;
        public Polygon poly;
        public Design glow;
        
        int rocks = 8;
        
        public Turret(Ship ship) {
            this.ship = ship;
            this.poly = new Polygon(new int[rocks], new int[rocks], rocks);
            this.glow = new Design.ShipGlow(ship);
        }
        
        float radius() { return ship.getRadius(); }
        float rockRadius() { return radius() * 0.4f; }
        
        public Polygon updatePolygon() {
            for (int i = 0; i < rocks; ++i) {
                float deg = 2 * (float) Math.PI * i / rocks;
                float x = ship.x + (radius() - rockRadius()) * (float) Math.cos(deg);
                float y = ship.y + (radius() - rockRadius()) * (float) Math.sin(deg);
                poly.xpoints[i] = Global.round(x);
                poly.ypoints[i] = Global.round(y);
            }
            return poly;
        }
        
        public Polygon collider() {
            return updatePolygon();
        }
        
        public void draw(Graphics2D g) {
            if (glow != null)
                glow.draw(g);
			Stroke firstStroke = g.getStroke();
            Color fill = ship.fill;
            Color outline = ship.getOutline();
            
            // System.out.println(fill);
            
            // draw 12 fixed surrounding "rocks"
            updatePolygon();
            float rad = radius();
            float rockRad = rockRadius();
            Color trance = Global.transparency(fill, 0.5f);
			g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < rocks; ++i) {
                g.setColor(trance);
                g.fillOval(poly.xpoints[i] - (int) rockRad, poly.ypoints[i] - (int) rockRad, (int) (rockRad * 2), (int) (rockRad * 2));
            }
            // for (int i = 0; i < rocks; ++i) {
                // g.setColor(Color.WHITE);
                // g.drawOval(poly.xpoints[i] - (int) rockRad, poly.ypoints[i] - (int) rockRad, (int) (rockRad * 2), (int) (rockRad * 2));
            // }
            
            // draw turret body
            float bodyRadius = rad - rockRad;
            Paint grad = new GradientPaint(0, ship.y - ship.getRadius(), fill.brighter().brighter(), 0, ship.y + ship.getRadius(), Color.BLACK);
            g.setPaint(grad);
            g.fillOval(Global.round (ship.x - bodyRadius), Global.round (ship.y - bodyRadius), Global.round (bodyRadius * 2), (int) (bodyRadius * 2));
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(Color.WHITE);
            g.drawOval(Global.round (ship.x - bodyRadius), Global.round (ship.y - bodyRadius), Global.round (bodyRadius * 2), Global.round (bodyRadius * 2));
            
            
            // draw cannon thing
			
			/*
            float angle = ship.getAngle();
            float sx = ship.x + bodyRadius * (float) Math.cos(angle);
            float sy = ship.y + bodyRadius * (float) Math.sin(angle);
            float ex = ship.x + rad * (float) Math.cos(angle);
            float ey = ship.y + rad * (float) Math.sin(angle);
            g.setStroke(new BasicStroke(5.5f));
            g.setColor(Color.WHITE);
            g.drawLine(Global.round(sx), Global.round(sy), Global.round(ex), Global.round(ey));
            g.setStroke(new BasicStroke(2.5f));
            g.setColor(Color.BLACK);
            g.drawLine(Global.round(sx), Global.round(sy), Global.round(ex), Global.round(ey));
			// */
			
			// reset stroke
			g.setStroke(firstStroke);
        }      
    }
	
	
	public class Arrow implements Design {
	
		public Ship ship;
        public Design glow;
		
		public Arrow(Ship ship) {
			this.ship = ship;
            this.glow = new Design.ShipGlow(ship);
		}
		
		public Polygon getPolygon(float scale) {
			float D = ship.getAngle();
			float R = ship.getRadius() * scale;
			Polygon p = new Polygon(new int[4], new int[4], 4);
			setPoint(p, 0, 0, R, D);
			setPoint(p, 1, 1/3f, R, D);
			setPoint(p, 2, 1/2f, 0.2f * R, D);
			setPoint(p, 3, 2/3f, R, D);
			return p;
		}
		
		public void setPoint(Polygon poly, int id, float arc, float radius, float off) {
			double ndeg = 2 * Math.PI * arc + off;
			poly.xpoints[id] = (int) (ship.x + radius * Math.cos(ndeg));
			poly.ypoints[id] = (int) (ship.y + radius * Math.sin(ndeg));
		}
        

		
		@Override
		public void draw(Graphics2D g) {
            // Global.log("Drawing glow...");
            if (glow != null)
                glow.draw(g);
            if (!ship.visible) return;
            // Global.log("Filling polygon...");
			
			Polygon poly = getPolygon(1);
			
			Color fill = ship.getFill();
			Paint old = g.getPaint();
			Paint grad = new GradientPaint(0, ship.y - ship.getRadius(), fill.brighter().brighter(), 0, ship.y + ship.getRadius(), Color.BLACK);
			g.setPaint(grad);
			g.fill(poly);
			g.setPaint(old);
			
			// Global.log("Drawing outline...");
            
			Color outline = ship.getOutline();
            
			Stroke oldS = g.getStroke();
			g.setStroke(new BasicStroke(1.1f));
			g.setColor(outline);
			g.draw(poly);
			g.setStroke(oldS);
			
		}
        
        protected Color getMixedColor(Color c1, float pct1, Color c2, float pct2) {
            float[] clr1 = c1.getComponents(null);
            float[] clr2 = c2.getComponents(null);
            for (int i = 0; i < clr1.length; i++) {
                clr1[i] = (clr1[i] * pct1) + (clr2[i] * pct2);
            }
            return new Color(clr1[0], clr1[1], clr1[2], clr1[3]);
        }

		@Override
		public Polygon collider() {
			return getPolygon(1 / Global.gold);
		}
	}
    
	public abstract class AbstractGlow implements Design {
		
		public abstract float getX();
		public abstract float getY();
		public abstract Color[] getColors();
		public abstract float getRadius();
		
		float[] glowFloats = new float[] {0.0f,  0.3f, 1f};
		// Color[] glowColors = new Color[] {null, null, Global.transparent};
		
		public void draw(Graphics2D g) {
            if (getRadius() < Global.EPS) return;
			RadialGradientPaint glowPaint = getGlowPaint();
			g.setPaint(glowPaint);
			g.fillOval((int) (getX() - getRadius()), (int) (getY() - getRadius()), (int) (getRadius() * 2), (int) (getRadius() * 2));
		}
		
		public RadialGradientPaint getGlowPaint() {
			Color[] glowColors = getColors();
			return new RadialGradientPaint(
				new Point2D.Float(getX(), getY()),
				// ship.getRadius() * 2.4f,
				getRadius(),
				glowFloats,
				glowColors,
				MultipleGradientPaint.CycleMethod.NO_CYCLE
			);
		}
		
		public Polygon collider() { return null; } // no collider
		
	}
	
	// for ships
	public class ShipGlow extends AbstractGlow {
	
		public final Ship ship;
		public ShipGlow(Ship ship) { this.ship = ship; }
		
		public float getX() { return ship.x; }
		public float getY() { return ship.y; }
        
        boolean flickerShield = false;
        
        FixedTimer flicker = new FixedTimer(new FixedTask() {
            public float FPS() { return 8; }
            public boolean fixedRate() { return false; }
            public void run() {
                if (ship.shield == 0 || ship.shield > 2) {
                    flickerShield = false;
                    stop();
                }
                else {
                    flickerShield ^= true;
                }
            }
        });
        
		public Color[] getColors() {
            if (ship.shield == 2 && !flicker.isRunning())
                flicker.start();
			if (ship.shield == 0 || flickerShield)
				return new Color[] {
					Global.transparency(ship.fill, 0.18f),
					Global.transparency(ship.fill, 0.25f),
					Global.transparent
				};
			else
				return new Color[] {
					Global.transparency(ship.fill, 0.1f),
					Global.transparency(ship.fill, 0.2f),
					Global.transparency(ship.fill, 0.45f)
				};
		}
		public float getRadius() { return ship.getRadius() * 2.4f; }
		
	}
	
	public class PowerGlow extends AbstractGlow {
		
		Color[] colors = new Color[] {
			Global.transparency(Color.BLUE, 0.5f),
			Global.transparency(Color.BLUE, 0.3f),
			Global.transparent
		};
		
		public final Power power;
		public PowerGlow(Power power) { this.power = power; }
		
		public float getX() { return power.x; }
		public float getY() { return power.y; }
		public Color[] getColors() { return colors; }
		public float getRadius() { return power.getRadius() * 2f; }
		
	}
    
}

