import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

public class PlayerShip extends Ship {

    public Game game;
	public Controls controls;
    
	public PlayerShip(String name, Color color, Game game, Controls controls) {
		super(name, color == null ? game.nextColor() : color);
        this.game = game;
        this.controls = controls;
        // apply shield for a while
	}
	
	// draw an arrow pointer above the ship
	Transform arrowPointer = new Transform(0, 0, Color.RED, null, false) {
		public void draw(Graphics2D g) {
			super.draw(g);
			Polygon p = new Polygon(new int[3], new int[3], 3);
			
			p.xpoints[0] = Global.round(x);
			p.xpoints[1] = Global.round(x - 5);
			p.xpoints[2] = Global.round(x + 5);
			
			p.ypoints[0] = Global.round(y);
			p.ypoints[2] = p.ypoints[1] = Global.round(y - 10);
			
			g.setColor(getFill());
			g.fillPolygon(p);
		}
		public void animate() {
			// super.animate(); // ignore usual physics
			y += 4 * Math.sin(System.currentTimeMillis() / 250.0);
		}
	};
		
	@Override
	public void draw(Graphics2D g) {
		super.draw(g);
		arrowPointer.draw(g);
	}
	
	@Override
	public void animate() {
		
		// set controls
		if (getControls() != null && getControls().enabled && !dead) {
			// rotate
			Point2D p = getGame().getScaledMousePosition();
			if (p != null)
				angle = (float) Math.atan2(p.getY() - y, p.getX() - x);
				
			int dx = 0, dy = 0;
			
			if (getControls().isUp) dy--;
			if (getControls().isDown) dy++;
			if (getControls().isLeft) dx--;
			if (getControls().isRight) dx++;
			
			tx = dx * getMaxSpeed();
			ty = dy * getMaxSpeed();
			
		}
		
		arrowPointer.x = x;
		arrowPointer.y = y - 30;
		arrowPointer.animate();
		
		super.animate();
	}
	
	float prevAngle = 0;
    
    public byte[] getControlBytes() {
        ByteBuffer bb = ByteBuffer.allocate(Controls.bufferSize());
        if (dead && life != 0) bb.put((byte) 0);
        else bb.put(getControls().getMask());
		
		
		float an = prevAngle;
		
		Point2D p = getGame().getScaledMousePosition();
		if (p != null)
			an = (float) Math.atan2(p.getY() - y, p.getX() - x);
        
		bb.putFloat(an);
		
		prevAngle = an;
        return bb.array();
    }
	
	public Controls getControls() {
		return controls;
	}
    
    public Game getGame() {
        return game;
    }
	
}

class Controls extends KeyAdapter {
        
    public void setContainer(Container c) {
        c.addKeyListener(this);
		c.requestFocusInWindow();
		if (Game.activeGame() != null)
			Game.activeGame().addKeyListener(this);
        c.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                isUp = isDown = isLeft = isRight = false;
            }
        });
    }
	
	protected static HashMap<String, Integer> keyCodes = new HashMap<String, Integer>() {{
		put("up", KeyEvent.VK_UP);
		put("left", KeyEvent.VK_LEFT);
		put("down", KeyEvent.VK_DOWN);
		put("right", KeyEvent.VK_RIGHT);
	}};
	
	public static int getKeyCode(String key) {
		if (key.length() == 1) {
			return (int) Character.toUpperCase(key.charAt(0));
		}
		else
			return keyCodes.get(key.toLowerCase());
	}
	
	protected int up = getKeyCode("w");
	protected int left = getKeyCode("a");
	protected int down = getKeyCode("s");
	protected int right = getKeyCode("d");
	
	public void setUp(String text) { up = getKeyCode(text); }
	public void setLeft(String text) { left = getKeyCode(text); }
	public void setDown(String text) { down = getKeyCode(text); }
	public void setRight(String text) { right = getKeyCode(text); }
	
	public String getUp() { return KeyEvent.getKeyText(up); }
	public String getLeft() { return KeyEvent.getKeyText(left); }
	public String getDown() { return KeyEvent.getKeyText(down); }
	public String getRight() { return KeyEvent.getKeyText(right); }
	
	public boolean isUp = false, isLeft = false, isDown = false, isRight = false;
	public boolean enabled = true;
    public boolean isTyping = false;
    StringBuilder buffer;
    
    public static int bufferSize() { return 5; }
    public static final int UP = 1 << 0;
    public static final int RIGHT = 1 << 1;
    public static final int DOWN = 1 << 2;
    public static final int LEFT = 1 << 3;
    
	public byte getMask() {
        return (byte) ((isUp ? UP : 0) | (isRight ? RIGHT : 0) | (isDown ? DOWN : 0) | (isLeft ? LEFT : 0));
    }
    
    public Controls() {
	
    }
    
    boolean onShift = false;
    boolean onCaps = false;
    
	public void keyPressed(KeyEvent e) {
        if (!isTyping) {
            toggle(e.getKeyCode(), true);
        }
        else if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            onShift = true;
        }
    }
	public void keyReleased(KeyEvent e) {
        /*
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            isUp = isDown = isLeft = isRight = false;
            boolean wasTyping = isTyping; 
            if (wasTyping) {
                Game.activeGame().getChatBox().setCue("[Click Enter to chat]");
            }
            else {
                buffer = new StringBuilder();
                Game.activeGame().log(buffer);
                Game.activeGame().getChatBox().setCue("[Click Enter again to send message]");
            }
            isTyping ^= true;
        }
        else if (isTyping) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SHIFT:
                    onShift = false;
                    break;
                case KeyEvent.VK_CAPS_LOCK:
                    onCaps ^= true;
                    break;
                case KeyEvent.VK_BACK_SPACE:
                    if (buffer.length() > 0)
                        buffer.deleteCharAt(buffer.length() - 1);
                    break;
                default:
                    char c = e.getKeyChar();
                    if (Character.isDefined(c)) {
                        if (onShift ^ onCaps)
                            buffer.append(Character.toUpperCase(e.getKeyChar()));
                        else
                            buffer.append(e.getKeyChar());
                    }
                    break;
            }
        }
        else  // */
            toggle(e.getKeyCode(), false);
    }
	
	public void toggle(int code, boolean checker) {
		if (code == up) isUp = checker;
		else if (code == left) isLeft = checker;
		else if (code == down) isDown = checker;
		else if (code == right) isRight = checker;
	}
	
	public String toString() {
		return "[up=" + (isUp ? '1' : '0') + ", left=" + (isLeft ? '1' : '0') + ", down=" + (isDown ? '1' : '0') + ", right=" + (isRight ? '1' : '0') + "]";
	}
    
    public static class ArrowKeys extends Controls {
        public ArrowKeys() {
            super();
            setUp("up");
            setLeft("left");
            setDown("down");
            setRight("right");
        }
    }
}