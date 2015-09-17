import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.nio.ByteBuffer;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

public class Ship extends Transform implements Comparable<Ship> {
    
	public final String name;
	public float radius = 18f;
	public float angle = 0f;
	public int index;
	public int getIndex() { return index; }
	
	public boolean dead = false;
	public boolean visible = true;
    
    // scaling and timers
	public short shield = 0;
	public short kills = 0;
	public short life = -1;
    public byte speedScale = 0;
	public byte sizeScale = 0;
	public byte fireRateScale = 0;
	public byte bulletSizeScale = 0; 
	
	public Design design = new Design.Arrow(this);
	public BulletSet bullets = new BulletSet(this, BulletStyle.SINGLE());
	
	public static Font FONT = new Font(Global.FONT, Font.PLAIN, 12);
	public static Color FONT_COLOR = Color.WHITE;
	
	public String getName() { return name; }
	
	public final ArrayList<FixedTimer> timers = new ArrayList<FixedTimer>();
	
	public Ship(String name, Color color) {
		super(Global.random((int) edgePadding, Global.WIDTH - (int) edgePadding), Global.random((int) edgePadding, Global.HEIGHT - (int) edgePadding), color, color == null ? null : color.darker(), true);
        this.name = name;
        
        this.maxspeed = 150;
        this.acceleration = 1.5f;
        
        // timer for shield
		timers.add(
        new FixedTimer(new FixedTask() {
            public float FPS() { return 1; }
            boolean hasShield = false;
            public void run() {
                if(hasShield) {
                    if (shield > 0) {
                        --shield;
                    }
                    if (shield == 0) hasShield = false;
                }
                else if(shield > 0)
                    hasShield = true;
            }
        }).start());
		
		// timer for power-ups
		timers.add(
		new FixedTimer(new FixedTask() {
			public float FPS() { return 1 / 7f; } // 
			byte prevSpeedScale = 0;
			byte prevSizeScale = 0;
			byte prevFireRateScale = 0;
			byte prevBulletSizeScale = 0;
			public void run() {
				if (prevSpeedScale != 0)
					speedScale -= Integer.signum(speedScale);
				if (prevSizeScale != 0)
					sizeScale -= Integer.signum(sizeScale);
				if (prevFireRateScale != 0)
					fireRateScale -= Integer.signum(fireRateScale);
				if (prevBulletSizeScale != 0)
					bulletSizeScale -= Integer.signum(bulletSizeScale);
					
				if (speedScale > 3) prevSpeedScale = 3;
				else if (speedScale < -3) speedScale = -3;
				
				if (sizeScale > 3) sizeScale = 3;
				else if (sizeScale < -3) sizeScale = -3;
				
				if (fireRateScale > 3) fireRateScale = 3;
				else if (fireRateScale < -3) fireRateScale = -3;
				
				if (bulletSizeScale > 3) bulletSizeScale = 3;
				else if (bulletSizeScale < -3) bulletSizeScale = -3;
				
				prevSpeedScale = speedScale;
				prevSizeScale = sizeScale;
				prevFireRateScale = fireRateScale;
				prevBulletSizeScale = bulletSizeScale;
			}
		}).start());
        
	}
	
	public void reset() {
		for (FixedTimer timer : timers)
			timer.stop();
		// timers.clear();
		if (getBulletSet() != null)
			getBulletSet().spawner.stop();
	}
	
	public void dispose() {
		reset();
		if (getBulletSet() != null) {
			getBulletSet().clear();
		}
	}
    
    @Override
    public float getMaxSpeed() { return maxspeed * (float) Math.pow(2, speedScale); }
    
    @Override
    public float getAcceleration() { return speedScale > 0 ? acceleration * 2 : acceleration; }
	
	public static int bufferSize() { return 44; }
	
	public byte[] getBytes() {
        ByteBuffer bb = ByteBuffer.allocate(bufferSize());
		// 0
        bb.putInt(Global.ColorToInt(fill));
        bb.putFloat(x);
        bb.putFloat(y);
        bb.putFloat(vx);
		// 16
        bb.putFloat(vy);
        bb.putFloat(tx);
        bb.putFloat(ty);
        bb.putFloat(getAngle());
		// 32
        bb.putShort(shield);
		bb.putShort(kills);
		bb.putShort(life);
		// 38
        bb.put(speedScale);
		bb.put(sizeScale);
		bb.put(fireRateScale);
		bb.put(bulletSizeScale);
		bb.put((byte) getStyle().bullets);
        bb.put((byte) (((dead ? 2 : 0) | (visible ? 1 : 0))));
		// 44
		return bb.array();
	}
	
	float dsmooth = 0.0028f;
	float dt = dsmooth;
	float trust = 12;
	
	
	public void fromBytes(byte[] data) { fromBytes(data, true); }
	
	public void fromBytes(byte[] data, boolean filter) {
				
        ByteBuffer bb = ByteBuffer.wrap(data);
        int color = bb.getInt();
        float fx = bb.getFloat();
        float fy = bb.getFloat();
        float fvx = bb.getFloat();
		
        float fvy = bb.getFloat();
        float ftx = bb.getFloat();
        float fty = bb.getFloat();
        float fangle = bb.getFloat();
		// 32
        short fshield = bb.getShort();
		short fkills = bb.getShort();
		short flife = bb.getShort();
        byte fspeedScale = bb.get();
		byte fsizeScale = bb.get();
		byte ffireRateScale = bb.get();
		byte fbulletSizeScale = bb.get();
		byte fbullets = bb.get();
        byte misc = bb.get();
		
        fill = Global.IntToColor(color);
		dead = ((misc & 2) == 0 ? false : true);
		visible = ((misc & 1) == 0 ? false : true);
		
        shield = fshield;
        speedScale = fspeedScale;
		sizeScale = fsizeScale;
		fireRateScale = ffireRateScale;
		bulletSizeScale = fbulletSizeScale;
		getStyle().bullets = fbullets;
		kills = fkills;
		life = flife;
		
		
		if (filter) {
			// smoothen position
			tx = ftx;
			ty = fty;
			
			if (Math.abs(fvx - vx) < 30)
				vx = fvx;
			if (Math.abs(fvy - vy) < 30)
				vy = fvy;
						
			float ex = fx - x;
			float ey = fy - y;
			float mag = ex * ex + ey * ey;
			x += ex / trust;
			y += ey / trust;
			
			
			// for angle, use cos/sin approach
			double cosA = Math.cos(angle);
			double sinA = Math.sin(angle);
			double cosF = Math.cos(fangle);
			double sinF = Math.sin(fangle);
			cosA += (cosF - cosA) * dsmooth * Math.PI;
			sinA += (sinF - sinA) * dsmooth * Math.PI;
			angle = (float) Math.atan2(sinA, cosA);
		}
		else {
			tx = ftx;
			ty = fty;
			vx = fvx;
			vy = fvy;
			x = fx;
			y = fy;
			angle = fangle;
		}
				
	}

    
    public void fromControlBytes(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        int mask = bb.get();
        float fangle = bb.getFloat();
        
        angle = fangle;
        
        int dx = 0, dy = 0;
        if ((mask & Controls.UP) != 0) dy--;
        if ((mask & Controls.DOWN) != 0) dy++;
        if ((mask & Controls.LEFT) != 0) dx--;
        if ((mask & Controls.RIGHT) != 0) dx++;
        
        tx = dx * getMaxSpeed();
        ty = dy * getMaxSpeed();
    }
    
	public Color getFill() { return visible ? (dead && life == 0 ? Global.transparency(fill, 0.1f) : fill) : Global.transparent; }
	public Color getOutline() { return Global.brighten(getFill(), 100); }
	public float getRadius() { return radius * (sizeScale == 0 ? 1 : (float) Math.pow(2, sizeScale)); }
	public float getAngle() { return angle; }
	
	public boolean collidesWith(Transform test) {
		if (!visible || dead) return false;
		if (test instanceof Ship) {
			Ship other = (Ship) test;
			if (!other.visible || other.dead) return false;
			return Global.collides(design.collider(), other.design.collider());
		}
		else if (test instanceof Bullet) {
			Bullet other = (Bullet) test;
			if (fill.equals(other.fill)) return false;
			return Global.collides(design.collider(), other.getCircle());
		}
		else if (test instanceof Power) {
			Power other = (Power) test;
			return Global.collides(design.collider(), other.getCircle());
		}
		return false;
	}
	
	@Override
	public void draw(Graphics2D g) {
		design.draw(g);
		drawName(g);
	}
	
	protected boolean wasDead = false;
	
	@Override
	public void animate() {
		super.animate();
		if (!wasDead && dead)
			Sounds.death.play();
		wasDead = dead;
		bullets.animate();
	}
	
	public void drawName(Graphics2D g) {
		g.setFont(FONT);
		g.setColor(Color.WHITE);
		String display = (dead && life == 0 ? name + " - DEAD" : name);
		double len = g.getFontMetrics().getStringBounds(display, g).getWidth();
		g.drawString(display, (int) (x - len / 2), (int) (y - getRadius()));
	}
    
    public BulletSet getBulletSet() {
        return bullets;
    }
    
    public BulletStyle getStyle() {
        return getBulletSet().getStyle();
    }
    
    public BulletStyle setStyle(BulletStyle bs) {
        getBulletSet().style = bs;
        return bs;
    }
    
    public Design setDesign(Design d) {
        design = d;
        return d;
    }
    
    public Design getDesign() {
        return design;
    }
    
    public void startSpawningBullets() {
        getBulletSet().spawner.start();
    }
    
    public void stopSpawningBullets() {
        getBulletSet().spawner.stop();
    }
	
	public String toString() {
		return getName();
		// return "[Ship: " + getName() + "]";
	}
    
    public int compareTo(Ship s) { return name.compareTo(s.name); }
       
	
}


class RandomShipDirection extends FixedTask {
    Ship ship;
    float speed, fps;
    public RandomShipDirection(Ship ship, float speed, float time) {
        super();
        this.ship = ship;
        this.speed = speed;
        this.fps = 1 / time;
    }
    public float FPS() { return fps; }
    public void run() {
		if (ship == null) stop();
        float direction = Global.random(0, (float) Math.PI * 2);
        float x = (float) Math.cos(direction);
        float y = (float) Math.sin(direction);
        ship.tx = x * speed;
        ship.ty = y * speed;
    }
}

