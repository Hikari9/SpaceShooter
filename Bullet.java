import java.awt.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.swing.*;

public class Bullet extends Transform implements Comparable<Bullet> {

	public BulletSet bulletSet;
    public float radius;

	public Bullet(float x, float y, BulletSet bulletSet) {
		super(x, y, bulletSet == null ? Global.transparent : bulletSet.getShip().fill, bulletSet == null ? Global.transparent : bulletSet.getShip().getOutline(), false);
		
		Sounds.shoot.play();
		
		this.bulletSet = bulletSet;
        if (bulletSet != null)
			this.radius = getStyle().radius * (getShip().bulletSizeScale == 0 ? 1 : (float) Math.pow(2, getShip().bulletSizeScale));
	}
    
	// public float getRadius() { return radius * (getShip() == null ? 1 : (float) Math.pow(2, getShip().bulletSizeScale)); }
	public float getRadius() { return radius; }
    public BulletSet getBulletSet() { return bulletSet; }
    public BulletStyle getStyle() { return getBulletSet().getStyle(); }
    public Ship getShip() { return getBulletSet() == null ? null : getBulletSet().getShip(); }
	
	public static int bufferSize() { return 32; }
	public byte[] getBytes() {
		ByteBuffer bb = ByteBuffer.allocate(bufferSize());
		bb.putFloat(x);
		bb.putFloat(y);
		bb.putFloat(tx);
		bb.putFloat(ty);
		vx = tx;
		vy = ty;
		bb.putFloat(getRadius());
		bb.putFloat(getMaxSpeed());
		bb.putFloat(getAcceleration());
		bb.putInt(Global.ColorToInt(getFill()));
		return bb.array();
	}
	
	private Bullet(float x, float y, float tx, float ty, float radius, float maxspeed, float acceleration, int color) {
		this(x, y, null);
		this.vx = this.tx = tx;
		this.vy = this.ty = ty;
		this.radius = radius;
		this.maxspeed = maxspeed;
		this.acceleration = acceleration;
        this.fill = Global.IntToColor(color);
	}
	
	public static Bullet fromBytes(byte[] data) {
		ByteBuffer bb = ByteBuffer.wrap(data);
		return new Bullet(bb.getFloat(), bb.getFloat(), bb.getFloat(),
			bb.getFloat(), bb.getFloat(), bb.getFloat(), bb.getFloat(), bb.getInt());
	}
	
	@Override
	public void draw(Graphics2D g) {
        if (getFill() != null) {
            g.setColor(Global.transparency(getFill(), 0.9f));
            float r = getRadius();
            g.fillOval((int)(x - r), (int)(y - r), (int)(r * 2), (int)(r * 2));
        }
	}
	
	public Circle getCircle() {
		return new Circle(x, y, getRadius());
	}
    
    @Override
    public int compareTo(Bullet b) {
        return hashCode() - b.hashCode();
    }
	
}

class MovingBullet extends Bullet {
		
	public MovingBullet(float x, float y, BulletSet bulletSet, float direction) {
		super(x, y, bulletSet);
		
        this.maxspeed = bulletSet.getStyle().speed;
        this.acceleration = bulletSet.getStyle().acceleration;
        this.tx = bulletSet.style.speed * (float) Math.cos(direction);
        this.ty = bulletSet.style.speed * (float) Math.sin(direction);
    }
	
}

class BulletSet extends AnimaQueue<Bullet> {
	
	public Ship ship;
	public BulletStyle style;
	
	protected final FixedTimer spawner;    
    
    @Override
    public boolean add(Bullet b) {
        boolean added = super.add(b);
        if (added) b.bulletSet = this;
        return added;
    }
    
    @Override
	public void draw(Graphics2D g) {
		for (Bullet b : this)
			b.draw(g);
	}
	
    @Override
	public void animate() {
		for (Iterator<Bullet> it = iterator(); it.hasNext();) {
			Bullet b = it.next();
			b.animate();
			if (b.outOfBounds())
				it.remove();
		}
	}
    	
	public BulletSet(Ship ship, BulletStyle style) {
        super();
		this.ship = ship;
		this.style = style;
        this.spawner = new FixedTimer(new Spawner());
	}
		    
    public BulletStyle getStyle() {
        return style;
    }
    
    public Ship getShip() {
        return ship;
    }
		
	class Spawner extends FixedTask {
		public boolean fixedRate() { return true; }
		public float FPS() {
			float fps = getStyle().BPS * ((getShip() == null || getShip().fireRateScale == 0) ? 1 : (float) Math.pow(2, getShip().fireRateScale));
			return fps;
		}
        public float delay() { return 1f / FPS(); }
		public boolean reschedulable() { return !(getShip().getDesign() instanceof Design.Turret); }
		public void run() {
			if (getShip().dead) return;
			Bullet[] newbullets = getStyle().generate(getShip());
			for (Bullet b : newbullets) {
				if (Game.activeGame() instanceof ServerGame && ((ServerGame) Game.activeGame()).isReady()) {
					((ServerGame) Game.activeGame()).sendBulletData(b);
				}
			}
			for (Bullet b : newbullets)
				add(b);
		}
	}
	
}

class BulletStyle {
	
	public float radius = 5;
	public float speed = 136; // pixels per second
    // public float acceleration = 1.4f; // speed per second
	public float acceleration = 5f;
	public float BPS = 2; // bullets per second
    public float angleAllowance = 0f; // in degrees, 0 = more accurate
	
	public int bullets;
	public boolean spread;
	
	public static final boolean SPREAD = true;
	public static BulletStyle SINGLE(){ return new BulletStyle(1, false); }
	public static BulletStyle DOUBLE() { return new BulletStyle(2, false); }
	public static BulletStyle TRIPLE() { return new BulletStyle(3, false); }
	public static BulletStyle DOUBLE_SPREAD() { return new BulletStyle(2, true); }
	public static BulletStyle TRIPLE_SPREAD() { return new BulletStyle(3, true); }
    public static BulletStyle HEAVY_SHOT() { return new BulletStyle(1, false, 25, 120, 0.7f, 999999, 0); } // big, slow rate, constant speed
    public static BulletStyle SNIPER_SHOT() { return new BulletStyle(1, false, 2.5f, 300, 0.7f, 999999, 5); } // small, a little fast, constant speed
	
	public BulletStyle(int bullets, boolean spread) {
		this.bullets = bullets;
		this.spread = spread;
	}
	
	public BulletStyle(int bullets, boolean spread, float radius, float speed, float BPS, float acceleration, float angleAllowance) {
		this.bullets = bullets;
		this.spread = spread;
		this.radius = radius;
		this.BPS = BPS;
		this.speed = speed;
        this.acceleration = acceleration;
        this.angleAllowance = angleAllowance;
	}
	
	public Bullet[] generate(Ship ship) {
		Bullet[] b = new Bullet[bullets];
		float R = ship.getRadius();
		float theta = ship.getAngle();
		float rad = spread ? radius : (float) (radius * Math.pow(2, ship.bulletSizeScale));
		float alpha = (float) Math.atan(rad / R);
        float angleAllowanceRadians = (float) Math.toRadians(angleAllowance);
		for (int i = -bullets + 1, j = 0; j < bullets; i += 2, ++j) {
			float[] p = Global.rotatePoint(
				ship.x, ship.y,
				theta + alpha * i,
				(spread ? R : (float) Math.sqrt(R*R + rad*rad*i*i))
			);
			b[j] = new MovingBullet(p[0], p[1], ship.getBulletSet(), (spread ? theta + alpha * i : theta) + (float) (Math.random() * 2 - 1) * angleAllowanceRadians);
		}
		return b;
	}
	
}
