import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.swing.*;

class TurretPower extends Power {
    
    String turretName = "Turret";
    
	public TurretPower() {
		super("Turret");
	}
    
	@Override
	public void powerUp(Ship ship) {
		Ship turret = Global.spawnRandomTurret();
        turretName = turret.getName();
		turret.fill = ship.fill;
		turret.restrictBounds = false;
		
		if (turretName.equals("Support Turret")) {
			// mirror player's style
			turret.setStyle(ship.getStyle());
		}
		
		turret.startSpawningBullets();
		Game.activeGame().addShip(turret);
	}
	@Override
	public int code() {
		return 0;
	}
    @Override
    public String getName() {
        return turretName;
    }
}

class QuestionPower extends Power {
    int powersToAdd;
    StringBuilder sb = new StringBuilder("?");
    public QuestionPower() {
        super("Question");
        this.powersToAdd = Global.random(1, 5);
    }
    @Override
    public void powerUp(Ship ship) {
        // get random powers to add to ship
        for (int i = 0; i < powersToAdd; ++i) {
            Power p = null;
            do { p = Global.spawnRandomPower(); }
            while (p instanceof QuestionPower);
            Global.powerUp(p, ship);
            sb.append('\t').append(p);
        }
    }
	@Override
	public int code() {
		return 1;
	}
    @Override
    public String getName() {
        return sb.toString();
    }
}

class ShieldPower extends Power {
    int timer;
	public ShieldPower(int timer) {
		super(timer == 5 ? "_Shield" : "Shield");
        this.timer = timer;
	}
	@Override
	public void powerUp(Ship ship) {
		ship.shield += timer;
	}
	@Override
	public int code() {
		return timer == 5 ? ~2 : 2;
	}
    @Override
    public String getName() {
        return "Shield +" + timer + " sec";
    }
}

class ShipSpeedPower extends Power.Resettable {
	public ShipSpeedPower(int delta) {
		super("ShipSpeed", delta);
	}
	@Override
	public void powerUp(Ship ship) {
        ship.speedScale += delta;
	}
	@Override
	public int code() {
		return delta >= 0 ? 3 : ~3;
	}
}

class ShipSizePower extends Power.Resettable {
	public ShipSizePower(int delta) {
		super("ShipSize", delta);
	}
	@Override
	public void powerUp(Ship ship) {
		ship.sizeScale += delta;
	}
	@Override
	public int code() {
		return delta >= 0 ? 4 : ~4;
	}
}

class FireRatePower extends Power.Resettable {
	public FireRatePower(int delta) {
		super("FireRate", delta);
	}
	@Override
	public void powerUp(Ship ship) {
		ship.fireRateScale += delta;
	}
	@Override
	public int code() {
		return delta >= 0 ? 5 : ~5;
	}
    
}

class BulletSizePower extends Power.Resettable {
	public BulletSizePower(int delta) {
		super("BulletSize", delta);
	}
	@Override
	public void powerUp(Ship ship) {
		ship.bulletSizeScale += delta;
	}
	@Override
	public int code() {
		return delta >= 0 ? 6 : ~6;
	}
}

class BulletShotPower extends Power {

	int bullets;
	boolean spread;
	
	public BulletShotPower(int bullets) {
        this(bullets, Global.random(2) == 1);
	}
	
	public BulletShotPower(int bullets, boolean spread) {
		super((spread ? "_" : "") + (bullets = Math.min(Math.max(1, bullets), 5)) + "x");
		this.bullets = bullets;
		this.spread = spread;
		spawnRandom();
	}
	
	@Override
	public void powerUp(Ship ship) {
		ship.getStyle().bullets = bullets;
		ship.getStyle().spread = spread;
	}
	
	@Override
	public int code() {
		return !spread ? (6 + bullets) : ~(6 + bullets);
	}
    
    @Override
    public String getName() {
        return "Bullets x" + bullets;
    }
    
	
}


public abstract class Power extends Transform {
	
	public float omega = 20; // degrees per second rotation
	public float radius = 18;
	public float angle = 0f; // radians
    public static short _ID = 0;
    public short ID;
	
	public BufferedImage icon = null;
	
	String name = "", parsedName = null;
	
	// override this
	public abstract void powerUp(Ship ship);
	public abstract int code();
	
	// cache
	static final HashMap<String, BufferedImage> cache = new HashMap<String, BufferedImage>();
	public static String[] files = {
		"Turret",
		"Question",
		"Shield",
		"ShipSpeed",
		"ShipSize",
		"FireRate",
		"BulletSize",
		"1x",
		"2x",
		"3x",
		"4x",
		"5x"
	};
	
    public static int bufferSize() { return 34; }
    
    public byte[] getBytes() {
        ByteBuffer bb = ByteBuffer.allocate(bufferSize());
		bb.putInt(code());
        bb.putFloat(x);
        bb.putFloat(y);
        bb.putFloat(tx);
        bb.putFloat(ty);
		vx = tx;
		vy = ty;
        bb.putFloat(radius);
        bb.putFloat(omega);
        bb.putFloat(maxspeed);
        bb.putShort(ID);
        return bb.array();
    }
    
    public static Power fromBytes(byte[] data) {
        final ByteBuffer bb = ByteBuffer.wrap(data);
		final int cod = bb.getInt();
        Power p = new Power(cod) {
            {
                x = bb.getFloat();
                y = bb.getFloat();
                vx = tx = bb.getFloat();
                vy = ty = bb.getFloat();
                radius = bb.getFloat();
                omega = bb.getFloat();
                maxspeed = bb.getFloat();
                ID = bb.getShort();
            }
            public void powerUp(Ship s) {}
			public int code() { return cod; }
        };
        return p;
    }
    
    
	public String getName() {
		if (parsedName == null) {
			StringBuilder sb = new StringBuilder();
			StringBuilder temp = new StringBuilder();
			
			String s = (name.charAt(0) == '_' ? name.substring(1) : name);
			
			temp.append(s.charAt(0));
			
			for (int i = 1; i < s.length(); ++i) {
				char c = s.charAt(i);
				if (Character.isUpperCase(c)) {
					if (sb.length() > 0) sb.append(' ');
					sb.append(temp);
					temp = new StringBuilder();
					temp.append(c);
				}
				else {
					temp.append(c);
				}
			}
			
			if (temp.length() > 0) {
				if (sb.length() > 0) sb.append(' ');
				sb.append(temp);
			}
			parsedName = sb.toString();
		}
		return parsedName;
	}
	
	public Power(int code) {
		this(code >= 0 ? files[code] : "_" + files[~code]);	
	}
	
	public Power(String file) {
		super(0, 0, Color.WHITE, Color.WHITE, false);
        
		this.ID = _ID++;
		this.name = file;
		if (cache.containsKey(name)) {
			icon = cache.get(name);
		}
		else {
			file = "icons/" + file + ".png";
			try {
				icon = ImageIO.read(new File(file));
				cache.put(name, icon);
			}
			catch (IOException ex) { System.err.println("Could not load image [" + file + "]!\n\t" + ex); }
		}
	}
	
	
	@Override
	public void draw(Graphics2D g) {
	
		if (icon == null) return;
		
		// draw glow
		glow.draw(g);
		
		// draw image
		
		AffineTransform at = new AffineTransform();
		
		// Paint prev = g.getPaint();
		// AffineTransform trans = g.getTransform();
		// g.setPaint(new TexturePaint(icon, new Rectangle2D.Float(0, 0, icon.getWidth(), icon.getHeight())));
		
		at.setToIdentity();
		at.translate(x, y);
		at.translate(-getRadius(), -getRadius());
		at.rotate(getAngle(), getRadius(), getRadius());
		
		float scaleX = 2 * getRadius() / icon.getWidth();
		float scaleY = 2 * getRadius() / icon.getHeight();
		
		at.scale(scaleX, scaleY);
		
		g.drawImage(icon, at, null);
	}
	
	Design glow = new Design.PowerGlow(this);
	
	
	@Override
	public void animate() {
		super.animate();
		this.angle += deltaTime * Math.toRadians(omega);
	}
	
	public Circle getCircle() { return new Circle(x, y, getRadius()); }	
	public float getRadius() { return radius; }
	public float getAngle() { return angle; }
	
	public String toString() {
		return getName();
	}
	
	public static abstract class Resettable extends Power {
		public int delta;
		public Resettable(String name, int delta) {
			super(delta < 0 ? "_" + name : name);
			this.delta = delta;
		}
        @Override
        public String getName() {
            return super.getName() + " x" + Global.normalizeFloat((float) Math.pow(2, delta));
        }
	}
}
