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

public class Global {
	public static float FPS = 500;
	public static float AnimateFPS = 500;
	public static float ReceiveFPS = 1000;
	public static float SendFPS = 500;
    public static int WIDTH = 1000;
    public static int HEIGHT = 618;
	public static int PORT = 45000;
    public static String PLAYER_SETTINGS = "info.txt";
    public static String HOST_SETTINGS = "host.txt";
    public static String IP_SETTINGS = "ip.txt";
    public static float EPS = 1e-6f;
    public static float STABLE_MARQUEE = 200;
    public static float fadeTimeout = 4.5f;
	public static boolean debugging = false;
	public static Random rand = new Random();
	public static Socket connectingSocket = null;
    
    public static int playerPort(int i) { return Global.PORT + i + 1; }
	// alternate ports
    public static int bulletPort() { return Global.PORT - 1; }
    public static int turretPort() { return Global.PORT - 2; }
    public static int powerPort() { return Global.PORT - 1; }
	public static int powerRemoverPort() { return Global.PORT - 2; }
	public static int chatLogPort() { return Global.PORT - 1; }
	public static int fadeLogPort() { return Global.PORT - 2; }
	public static int serverTimePort() { return Global.PORT - 1; }
    
    public static final float gold = 1.61803398875f;
    public static String FONT = "Aase";
	public static GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
    
    public static final int poolSize = 100;
    public static ScheduledExecutorService getPool() { return FixedTimer.pool; }
	
	public static void onException() {
		Game game = Game.activeGame();
        if (!game.hasEnded()) {
            System.exit(1);
		}
		else if (game instanceof ServerGame)
			((ServerGame)game).closeNetworking();
		else if (game instanceof ClientGame)
			((ClientGame)game).closeNetworking();
	}
    
        
    static {
        // check if font is available
        String[] fonts = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        boolean found = false;
        String lower = FONT.toLowerCase();
        for (String f : fonts) {
            if (f.equalsIgnoreCase(FONT)) {
                found = true;
                break;
            }
        }
        if (!found)
            registerFont(FONT);
    }
	
    public static void registerFont(String s) {
        try {
            URL fontUrl = new File(FONT + ".ttf").toURI().toURL();
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontUrl.openStream());
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);
        }
        catch (IOException ex) {
            System.out.println("Error: font \"" + FONT + "\" is not located on your machine.\n\t" + ex);
        }
        catch (FontFormatException ex) {
            System.out.println("Error: font \"" + FONT + "\" is not truetype.\n\t" + ex);
        }
    }
    
    // 1st, 2nd, 3rd, 4th, etc
    public static String ordinal(int number) {
        switch (number % 10) {
            case 1:
                return number + "st";
            case 2:
                return number + "nd";
            case 3:
                return number + "rd";
            default:
                return number + "th";
        }
    }
	
	public static Drawable getLoadingScreen() {
		Drawable loading = new Drawable() {
			Color fill = Global.transparency(Color.BLUE, 0.65f);
			Font font = new Font(Global.FONT, Font.PLAIN, 80);
			String text = "Loading";
			int padding = 60;
			long time = System.currentTimeMillis();
			@Override
			public void draw(Graphics2D g) {
				g.setColor(fill);
				g.fillRect(padding, padding, Global.WIDTH - padding * 2, Global.HEIGHT - padding * 2);
				g.setColor(Color.WHITE);
				g.drawRect(padding, padding, Global.WIDTH - padding * 2, Global.HEIGHT - padding * 2);
				g.setFont(font);
				FontMetrics fm = g.getFontMetrics();
				float sw = fm.stringWidth(text);
				int dots = (int) ((System.currentTimeMillis() - time) / 1000) % 5;
				g.drawString(text + "...", (Global.WIDTH - sw) / 2, Global.HEIGHT / 2);
			}
		};
		return loading;
	}
	
	public static JFrame getLoadingFrame() {
		JFrame loading = new JFrame("Loading...");
        loading.setSize(500, 300);
        loading.add(new JComponent() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHints(Global.AntiAliasing);
                                
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(Global.FONT, Font.PLAIN, 80));
                int w = (int) g2.getFontMetrics().getStringBounds("Loading...", g2).getWidth();
                g2.drawString("Loading...", (getWidth() - w) / 2, getHeight() / 2 + 10);
            }
        });
		return loading;
	}
	
	public static Ship spawnRandomTurret() {
		Ship turret = null;
		
		switch (random(7)) {
			case 0: // heavy
				turret = new TurretShip("Heavy Turret", Color.YELLOW, 19f);
				turret.setStyle(BulletStyle.HEAVY_SHOT());
				break;
			case 1: // spread
				turret = new TurretShip("Spread Turret", Color.YELLOW, 45);
				turret.setStyle(BulletStyle.DOUBLE_SPREAD());
				turret.getStyle().BPS *= 2f;
                // turret.getStyle().speed = 110;
				break;
			case 2: // sniper
				turret = new FarSniperShip("Sniper Turret", Color.YELLOW, Game.activeGame());
				break;
            case 3: // wave
                turret = new TurretShip("Shower Turret", Color.YELLOW, 30f) {
                    final long start = System.currentTimeMillis();
                    @Override
                    public float getAngle() {
                        return (float) (Math.PI * Math.sin((System.currentTimeMillis() - start) / 900f)) + super.getAngle();
                    }
                };
				turret.setStyle(BulletStyle.TRIPLE_SPREAD());
				turret.getStyle().radius /= 3;
				turret.getStyle().BPS *= 2.5f;
                break;
            case 4: // pulse
                turret = new TurretShip("Pulse Turret", Color.YELLOW, 0f);
                turret.getStyle().bullets = 20;
                turret.getStyle().radius = turret.getRadius() * (float) Math.tan(Math.PI / turret.getStyle().bullets);
                turret.getStyle().BPS = 0.35f;
                turret.getStyle().spread = true;
                turret.getStyle().speed = 70f;
                break;
            case 5:
                turret = new TurretShip("Lazer Turret", Color.YELLOW, 0f);
                turret.angle = Global.random(0f, (float) (2 * Math.PI));
                turret.getStyle().BPS = 20;
                break;
			case 6: // follows crosshair
                turret = new TurretShip("Support Turret", Color.YELLOW, 0f) {
					@Override
					public float getAngle() {
						if (Game.activeGame() != null) {
							Point2D mouse = Game.activeGame().getScaledMousePosition();
							return (float) Math.atan2(mouse.getY() - y, mouse.getX() - x);
						}
						else {
							return super.getAngle();
						}
					}
				};
                break;
		}
		
		
		float sweepSpeed = random(16, 45);
		turret.maxspeed = sweepSpeed;
		switch (random(4)) {
			case 0: // up -> down
				turret.x = random(Transform.edgePadding, WIDTH - Transform.edgePadding);
				turret.y = 0;
				turret.tx = 0;
				turret.ty = sweepSpeed;
				break;
			case 1: // left -> right
				turret.x = -Transform.allowance + 1;
				turret.y = random(Transform.edgePadding, HEIGHT - Transform.edgePadding);
				turret.tx = sweepSpeed;
				turret.ty = 0;
				break;
			case 2: // down -> up
				turret.x = random(Transform.edgePadding, WIDTH - Transform.edgePadding);
				turret.y = HEIGHT;
				turret.tx = 0;
				turret.ty = -sweepSpeed;
				break;
			case 3: // right -> left
				turret.x = WIDTH + Transform.allowance - 1;
				turret.y = random(Transform.edgePadding, HEIGHT - Transform.edgePadding);
				turret.tx = -sweepSpeed;
				turret.ty = 0;
				break;
		}
		
		turret.vx = turret.tx;
		turret.vy = turret.ty;
        
		return turret;
		
	}
	
	
	public static Game randomGame() {
		
		Game game = new Game();
		
		Ship player = new Ship("Player", game.nextColor()) {
            @Override
            public void animate() {
                super.animate();
                angle += deltaTime;
            }
        };
		
        ArrayList<FixedTimer> timers = new ArrayList<FixedTimer>();
        timers.add(new FixedTimer(new RandomShipDirection(player, 100, 1)).start());
		player.startSpawningBullets();
		game.addShip(player);
		
		RuledPlane.addDefault(game, player);
		
		for (int i = 0; i < 7; ++i) {
			Ship turret = Global.spawnRandomTurret();
			turret.tx = turret.ty = 0;
			turret.spawnRandom();
			turret.fill = game.COLORS[i + 1];
			turret.startSpawningBullets();
			game.addShip(turret);
            turret.restrictBounds = true;
            timers.add(new FixedTimer(new RandomShipDirection(turret, 100, 1)).start());
		}
		
		return game;
		
	}
    	
	public static void powerUp(final Power power, final Ship ship) {
		power.powerUp(ship);
		if (ship instanceof PlayerShip)
			Sounds.powerUp.play();
        // for testing
        String[] lines = power.toString().trim().split("\t");
        for (int i = 0; i < lines.length; ++i) {
            if (i == 0)
                Global.log(ship + " got [" + lines[i] + "]");
            else
                Global.log(" :::" + lines[i]);
        }
        
	}
	
	public static final ConcurrentLinkedQueue<String> Log = new ConcurrentLinkedQueue<String>();
		
	public static void log(Object message) {
		Log.add(message.toString());
		while (Log.size() > 100)
			Log.poll();
		if (debugging)
			System.out.print(message.toString() + "\n");
		if (Game.activeGame().getHud() != null)
			Game.activeGame().log(message);
	}
    
    public static void fadeLog(Object message, float x, float y, Color color) {
        if (Game.activeGame() != null)
            Game.activeGame().fadeLog(message.toString(), x, y, color);
    }
	
	public static RenderingHints AntiAliasing;
    static {
        // anti-alias text
        AntiAliasing = new RenderingHints(
            RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // anti-alias quality of images
		AntiAliasing.put(
			RenderingHints.KEY_RENDERING,
			RenderingHints.VALUE_RENDER_QUALITY);
        // anti-alias edges
		AntiAliasing.put(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		AntiAliasing.put(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
	
	public static final Color transparent = new Color(0,0,0,0);
	
	public static Color transparency(Color c, float alpha) {
		if (c == null) return new Color(0, 0, 0, 0);
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, (int) (c.getAlpha() * alpha))));
	}
    
    public static int ColorToInt(Color c) {
        return c.getRGB();
    }
    
    public static Color IntToColor(int v) {
        return new Color(v);
    }
    
    public static Color brighten(Color c, int addend) {
        if (c == null) return new Color(0, 0, 0, 0);
        return new Color(
            Math.min(c.getRed() + addend, 255),
            Math.min(c.getGreen() + addend, 255),
            Math.min(c.getBlue() + addend, 255),
            c.getAlpha()
        );
    }
	
	public static int round(float x) { return (int) (x + 0.5f); }
	public static int random(int max) { return rand.nextInt(max); }
	public static int random(int min, int max) { return random(max - min) + min; }
	
	public static float random(float max) { return rand.nextFloat() * max; }
	public static float random(float min, float max) { return random(max - min) + min; }
	
	public static float[] rotatePoint(float x, float y, float theta, float radius) {
		return new float[]{
			x + radius * (float) Math.cos(theta),
			y + radius * (float) Math.sin(theta)
		};
	}
    
    public static Power spawnRandomPower() {
        Power power = null;
        int randomScale = random(1, 4);
        if (random(2) == 0) randomScale *= -1;
        
		switch (random(8)) {
			case 0:
				power = new BulletSizePower(randomScale);
				break;
			case 1:
				power = new FireRatePower(randomScale);
				break;
			case 2:
				power = new ShipSizePower(randomScale);
				break;
			case 3:
				power = new ShieldPower(Global.random(1, 3) * 5);
				break;
			case 4:
				power = new ShipSpeedPower(randomScale);
				break;
            case 5:
                power = new BulletShotPower(random(1, 6));
                break;
            case 6:
                power = new QuestionPower();
				break;
			case 7:
				power = new TurretPower();
				break;
		}
        return power;
    }
	
	public static Power spawnRandomPower(float sweepSpeed) {
		
        Power power = spawnRandomPower();
		power.maxspeed = sweepSpeed;
		switch (random(4)) {
			case 0: // up -> down
				power.x = random(Transform.edgePadding, WIDTH - Transform.edgePadding);
				power.y = 0;
				power.tx = 0;
				power.ty = sweepSpeed;
				break;
			case 1: // left -> right
				power.x = -Transform.allowance + 1;
				power.y = random(Transform.edgePadding, HEIGHT - Transform.edgePadding);
				power.tx = sweepSpeed;
				power.ty = 0;
				break;
			case 2: // down -> up
				power.x = random(Transform.edgePadding, WIDTH - Transform.edgePadding);
				power.y = HEIGHT;
				power.tx = 0;
				power.ty = -sweepSpeed;
				break;
			case 3: // right -> left
				power.x = WIDTH + Transform.allowance - 1;
				power.y = random(Transform.edgePadding, HEIGHT - Transform.edgePadding);
				power.tx = -sweepSpeed;
				power.ty = 0;
				break;
		}
		
		power.vx = power.tx;
		power.vy = power.ty;
		return power;
	}
	
	public static boolean collides(Polygon p, Circle c) {
        if (p.contains(c.x, c.y))
			return true;
		for (int i = 0, j = p.npoints - 1; i < p.npoints; j = i++) {
			float x1 = p.xpoints[j];
			float y1 = p.ypoints[j];
			float x2 = p.xpoints[i];
			float y2 = p.ypoints[i];
			if (Line2D.ptSegDistSq(x1, y1, x2, y2, c.x, c.y) <= c.r * c.r + 1e-6f)
				return true;
		}
		return false;
    }
	
    public static boolean collides(Circle c, Polygon p) {
        return collides(p, c);
    }
	
	public static boolean collides(Polygon a, Polygon b) {
		for (int i = 0; i < a.npoints; ++i)
			if (b.contains(a.xpoints[i], a.ypoints[i]))
				return true;
		for (int i = 0; i < b.npoints; ++i)
			if (a.contains(b.xpoints[i], b.ypoints[i]))
				return true;
		for (int i = 0, j = a.npoints - 1; i < a.npoints; j = i++) {
			Line2D.Float A = new Line2D.Float(a.xpoints[j], a.ypoints[j], a.xpoints[i], a.ypoints[i]);
			for (int k = 0, l = b.npoints - 1; k < b.npoints; l = k++) {
				Line2D.Float B = new Line2D.Float(b.xpoints[l], b.ypoints[l], b.xpoints[k], b.ypoints[k]);
				if (A.intersectsLine(B))
					return true;
			}
		}
		return false;
	}
    
    public static String normalizeFloat(float f) {
        String s = Float.toString(f);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return s;
    }
	
	public static boolean collides(Circle a, Circle b) {
		return Math.hypot(a.x - b.x, a.y - b.y) <= a.r + b.r;
	}
	
	
}


