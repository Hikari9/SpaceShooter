import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

public class RuledPlane extends Transform {

	public float stroke;
	public float size;
	public float acceleration = 0.4f;
	public Ship relativeShip;
	public float velocityScale = 1f;
	
	public RuledPlane(float x, float y, float stroke, float size, Color outline, Ship relativeShip) {
		super(x, y, null, outline, false);
		this.stroke = stroke;
		this.size = size;
		this.relativeShip = relativeShip;
	}
	
	public static void addDefault(Game game, Ship player) {
		RuledPlane big = new RuledPlane(0, 0, 1, 90, Global.transparency(Color.BLUE, 0.5f), player) {{
			velocityScale = 0.3f;
			acceleration = 0.1f;
		}};
		RuledPlane small = new RuledPlane(0, 0, 0.3f, 35, Global.transparency(Color.BLUE, 0.4f), player) {{
			velocityScale = 0.15f;
			acceleration = 0.1f;
		}};
		game.addLayer(small, -10);
		game.addLayer(big, -9);
	}
	
	@Override
	public void draw(Graphics2D g) {
		super.draw(g);
		g.setColor(getOutline());
		g.setStroke(new BasicStroke(stroke));
		for (float xpos = x; xpos < Global.WIDTH; xpos += size)
			g.drawLine((int) xpos, 0, (int) xpos, Global.HEIGHT);
		for (float ypos = y; ypos < Global.HEIGHT; ypos += size)
			g.drawLine(0, (int) ypos, Global.WIDTH, (int) ypos);
	}
	
	@Override
	public void animate() {
		super.animate();
		synchronized(this) {
			while (x < 0)
				x += size;
			while (x >= size)
				x -= size;
			while (y < 0)
				y += size;
			while (y >= size)
				y -= size;
		}
		if (relativeShip != null) {
			this.tx = -relativeShip.tx * velocityScale;
			this.ty = -relativeShip.ty * velocityScale;
		}
	}
}
