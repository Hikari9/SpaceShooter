import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.swing.*;

public class Hud implements Drawable {
	
	public static void main(String[] args) {
		JFrame f = new JFrame("Heads Up Display");
		f.setResizable(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		Game g = new Game();
		PlayerShip ship = new PlayerShip("Rico", null, g, new Controls());
		ship.getControls().setContainer(f);
		g.addShip(ship);
		
		RuledPlane.addDefault(g, ship);
		Hud hud = new Hud(ship);
		FixedTimer timer = hud.startTimer(3 * 60);
		
		g.addLayer(hud);
		
		g.animator.start();
		g.repainter.start();
		g.collisionDetector.start();
		ship.startSpawningBullets();
		
		f.add(g);
		f.pack();
		f.setVisible(true);
		
        Scanner sc = new Scanner(System.in);
		
	}
	
	private Font font = new Font(Global.FONT, Font.PLAIN, 18);
	// private Font font = new Font("Bernard MT Condensed", Font.PLAIN, 18);
	private int time = -1;
	public void setTime(int t) {
		if (time != t) {
			time = t;
			switch (t) {
				case 30:
					Game.activeGame().threadedMarquee("30 Seconds Remaining", Global.STABLE_MARQUEE);
					break;
				case 121: // +1 to not display if initial time was 2 mins
					Game.activeGame().threadedMarquee("2 Minutes Remaining", Global.STABLE_MARQUEE);
					break;
				case 301:
					Game.activeGame().threadedMarquee("5 Minutes Remaining", Global.STABLE_MARQUEE);
					break;
				case 601:
					Game.activeGame().threadedMarquee("10 Minutes Remaining", Global.STABLE_MARQUEE);
					break;
			}
		}
	}
	public Ship ship;
	private String bulletType;
	
	public Font getFont() { return font; }
	public String getPlayerName() { return ship == null ? "-" : ship.getName(); }
	public int getPlayerNumber() { return ship == null ? 0 : ship.getIndex() + 1; }
	public int getKills() { return ship == null ? 0 : ship.kills; }
	public Object getLife() { return (ship == null || ship.life == -1 ? "---" : ship.life); }
	public int getTime() { return time; }
	public String getBPS() { return Global.normalizeFloat(ship == null ? 0 : (float) Math.pow(2, ship.fireRateScale)); }
	public String getBulletRadius() { return Global.normalizeFloat(ship == null ? 0 : (float) Math.pow(2, ship.bulletSizeScale)); }
	public String getShipRadius() { return Global.normalizeFloat(ship == null ? 0 : (float) Math.pow(2, ship.sizeScale)); }
    public String getShipSpeed() { return Global.normalizeFloat(ship == null ? 0 : (float) Math.pow(2, ship.speedScale)); }
	
	
	public int getNumberOfBullets() { return ship == null ? 0 : ship.getStyle().bullets; }
    public String getShield() {
		int s = (ship == null ? 0 : ship.shield);
		if (s == 0) return "none";
		else return s + " sec";
	}
		
	public String getTimeString() { return getTime() == -1 ? "---" : String.format("%02d:%02d", getTime() / 60, getTime() % 60); }
	
	public Hud(Ship playerShip) {
		this.ship = playerShip;
	}
	
	public FixedTimer startTimer(int seconds) {
		if (seconds <= 0) {
			setTime(seconds);
			return null;
		}
		time = (int) seconds;
		final FixedTimer timer = new FixedTimer(new FixedTask() {
			public float delay() { return 1f; }
			public boolean fixedRate() { return true; }
			public float FPS() { return 1f; }
			public void run() {
				Game game = Game.activeGame();
				if (game instanceof ServerGame)
					((ServerGame) game).updateServerTime(time - 1);
				setTime(time - 1);
				
				if (time == 0)
					game.endTheGame("Time's Up!");
				if (game.hasEnded())
					stop();
			}
		});
		timer.start();
		return timer;
	}
	
	public static final float LEFT = 0;
	public static final float CENTER = 0.5f;
	public static final float RIGHT = 1f;
	
	@Override
	public void draw(Graphics2D g) {
		g.setFont(font);
		g.setColor(Color.WHITE);
		
		int w = Global.WIDTH;
		int h = Global.HEIGHT;
		
		drawString(g, 30, 30, LEFT, "PLAYER", getPlayerNumber(), "SIZE", getShipRadius() + " x", "SPEED", getShipSpeed() + " x");
		drawString(g, 30, h - 40, LEFT, "KILLS", getKills(), "LIVES", getLife(),
			"bul. SIZE", getBulletRadius() + " x", "fire RATE", getBPS() + " x", "SHOTS", getNumberOfBullets() + " x", "SHIELD", getShield());
		drawString(g, w / 2, 30, CENTER, "TIME", getTimeString());
    }
	
	void drawString(Graphics2D g, int curX, int curY, float pos, Object... strings) {
		int dx = 110, dy = 20;
		boolean bottom = 2 * curY > Global.HEIGHT;
		boolean alternate = false;
		FontMetrics fm = g.getFontMetrics();
		if (bottom) dy *= -1;
		for (int i = 0; i < strings.length; ++i) {
			String s = strings[i].toString();
			float width = fm.stringWidth(s);
            /*
			if (!alternate) {
				// draw black glow
				Paint oldPaint = g.getPaint();
				RadialGradientPaint paint = new RadialGradientPaint(curX - width * pos + width / 2f, curY, 0.6f * dx, new float[]{0.2f, 1f}, new Color[]{Color.BLACK, Global.transparent});
				g.setPaint(paint);
				Point2D gPoint = paint.getCenterPoint();
				double gx = gPoint.getX();
				double gy = gPoint.getY();
				float gr = paint.getRadius();
				g.fillOval((int) (gx - gr), (int) (gy - gr), (int) (gr * 2f), (int) (gr * 2f));
				g.setPaint(oldPaint);
			}
            */
			g.drawString(s, curX - width * pos, curY);
			if (alternate) {
				curX += dx;
				curY -= dy;
			}
			else {
				curY += dy;
			}
			alternate ^= true;
		}
	}
		
	
	
}
