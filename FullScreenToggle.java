import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

public class FullScreenToggle extends KeyAdapter {
    
    public static JFrame frame = null;
    public int keyCode;
    public static int prevX, prevY, prevWidth, prevHeight;
    public static boolean full;
    
    private FullScreenToggle(JFrame frame, int keyCode) {
        this.keyCode = keyCode;
		
		FullScreenToggle.frame = frame;
		full = false;
		
		frame.addKeyListener(this);
    }
	
	public static void addToggleToGame(JFrame frame, Game game, int key) {
		Drawable text = new Drawable() {
            public void draw(Graphics2D g2) {
                String message = "Press F11 to toggle full screen";
                g2.setFont(new Font(Global.FONT, Font.PLAIN, 16));
                FontMetrics fm = g2.getFontMetrics();
                int h = Global.round(fm.getMaxAscent() - fm.getMaxDescent());
                int w = Global.round(fm.stringWidth(message));
                int pad = 10;
                g2.setColor(Color.WHITE);
                g2.drawString(message, Global.WIDTH - w - pad, h + pad);
            }
        };
		game.addLayer(text, -7);
		if (frame != FullScreenToggle.frame)
			new FullScreenToggle(frame, key);
	}
    
    boolean toggling = false;
    
    public void keyReleased(KeyEvent e) {
        
        if ((e.getKeyCode() == keyCode || (e.getKeyCode() == KeyEvent.VK_ESCAPE && full))) {
			if (!toggling) return;
            toggling = false;
            full ^= true;
            if (full) {
                prevX = frame.getX();
                prevY = frame.getY();
                prevWidth = frame.getWidth();
                prevHeight = frame.getHeight();
                
                frame.setVisible(false);
                frame.dispose();
                frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
                frame.setUndecorated(true);
                frame.setVisible(true);
            }
            else {
                frame.setVisible(false);
                frame.dispose();
                frame.setExtendedState(frame.getExtendedState() & ~JFrame.MAXIMIZED_BOTH);
                frame.setUndecorated(false);
                frame.setBounds(prevX, prevY, prevWidth, prevHeight);
                frame.setVisible(true);
            }
        }
    }
    
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == keyCode || (e.getKeyCode() == KeyEvent.VK_ESCAPE && full))
            toggling = true;
    }
    
}