import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.swing.*;


public class ChatBox implements Drawable {
	
	public final Queue<Object> all = new ConcurrentLinkedQueue<Object>();
	public final Queue<Object> capped = new ConcurrentLinkedQueue<Object>();
	public Font font;
	public int cap, bigCap;
	public int paddingX, paddingY;
	public int posX, posY;
	
	public static final int LEFT = -1;
	public static final int RIGHT = 1;
	public static final int UP = -1;
	public static final int DOWN = 1;
	
	Color fill = Color.WHITE;
	
	public ChatBox() {
		this(
			new Font("Consolas", Font.PLAIN, 15),
			1, 6,
			30, 60,
			RIGHT, UP
		);
		fill = Color.GRAY;
	}
	
	public ChatBox(Font font, int cap, int bigCap, int paddingX, int paddingY, int posX, int posY) {
		this.font = font;
		this.cap = cap;
		this.bigCap = bigCap;
		this.paddingX = paddingX;
		this.paddingY = paddingY;
		this.posX = posX;
		this.posY = posY;
	}
	
	FixedTimer truncater = new FixedTimer(new FixedTask() {
		public float FPS() { return 1f / 4; }
		public void run() {
			if (capped.size() > cap) capped.poll();
		}
	}).start();
	
	@Override
	public void draw(Graphics2D g) {
	
		if (capped.isEmpty()) return;
		
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		
		int x = (posX == RIGHT ? Global.WIDTH - paddingX : paddingX);
		int y = (posY == UP ? paddingY + font.getSize() / 2 : Global.HEIGHT - paddingY - font.getSize() / 2);

		int it = 1;
		for (Object o : capped) {
			String s = o.toString();
			g.setColor(Global.transparency(fill, (float) it++ / capped.size() * 0.75f));
			float sw = fm.stringWidth(s);
			g.drawString(s, posX == RIGHT ? x - sw : x, y);
			y += (font.getSize() + 1) * -posY;
		}
		
	}
	
	public void append(Object message) {
		all.offer(message);
		capped.offer(message);
		while (capped.size() > bigCap) capped.poll();
	}
	
	public void clear() {
		all.clear();
		capped.clear();
	}
	
}