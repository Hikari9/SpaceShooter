import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import javax.imageio.*;
import java.io.*;

public class Menu implements Drawable {
	
	// contains all menu items
    public final ArrayList<MenuItem> items = new ArrayList<MenuItem>();
	
	// allow visibility to be set
	protected boolean visible = true;
	public boolean isVisible() { return visible; }
	public void setVisible(boolean v) { visible = v; }
	
	public void draw(Graphics2D g) {
		if (isVisible())
			for (MenuItem m : items)
				if (m.isVisible())
					m.draw(g);
	}
	
	// allow first game instance attached to the menu to be accessed
	Game _game = null;
	public Game getGame() {
		if (_game == null)
			_game = Game.activeGame();
		return _game;
	}
	    
	public Menu() {}
	
	private Point2D getMousePosition() {
		if (getGame() == null) return null;
		return getGame().getScaledMousePosition();
	}
	
    // for allow menu items to be clicked
    public MouseListener getMouseListener() {
        return new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
				if (isVisible()) {
					for (final MenuItem item : items) {
						if (item.isVisible() && item.isHovered()) {
							FixedTimer.schedule(new Runnable() { public void run() {item.onClick();}});
						}
					}
				}
            }
        };
    }

	// get images from menu folder
	private BufferedImage getImage(String file) {
		try { return ImageIO.read(new File("menu/" + file)); }
		catch (IOException ex) { System.err.println("Could not read: " + ex); return null; }
	}
    
    // standard menu item: has bounds
    public abstract class MenuItem implements Drawable {
        public Rectangle bounds;
        public MenuItem(int x, int y, int w, int h) {
            bounds = new Rectangle(x, y, w, h);
        }
        public boolean isHovered() {
            Point2D mouse = getMousePosition();
            if (mouse == null) return false;
            return bounds.contains(mouse);
        }
		private boolean visible = true;
		public boolean isVisible() { return visible; }
		public void setVisible(boolean v) { visible = v; }
        public abstract void draw(Graphics2D g);
        public abstract void onClick();
    }
	
	// a colored menu box
	public class MenuBox extends MenuItem {
		public Color fill;
		public MenuBox(int x, int y, int w, int h, Color fill) {
			super(x, y, w, h);
			this.fill = fill;
		}
		public void draw(Graphics2D g) {
			g.setColor(fill);
			g.fill(bounds);
			g.setColor(Color.WHITE);
			g.draw(bounds);
		}
		public void onClick() {}
	}
	
	// constants for text offset in menu label
	public static final float LEFT = 0;
	public static final float CENTER = 0.5f;
	public static final float RIGHT = 1;
	
	// a textbox
	public class MenuLabel extends MenuItem {
		public String text;
		public Color fill;
		public Font font;
		public float offset;
		
		public MenuLabel(int x, int y, String text, Color fill, String fontName, int fontSize, float offset) {
			super(x, y - fontSize / 2, 0, fontSize * 2);
			this.text = text;
			this.fill = fill;
			this.offset = offset;
			this.font = new Font(fontName, Font.PLAIN, fontSize);
		}
		
		public void draw(Graphics2D g) {
			g.setColor(fill);
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics();
			float sw = fm.stringWidth(text);
			if (bounds.width == 0)
				bounds.width = (int) sw;
			g.drawString(text, bounds.x - sw * offset, bounds.y + font.getSize() / 2);
		}
		
		public void onClick() {}
	}
	
	// a textbox with two texts and two fonts
	public class MenuLabel2 extends MenuLabel {
		public String text2;
		public Font font2;
		
		public MenuLabel2(int x, int y, String text, String text2, Color fill, String fontName, int fontSize1, int fontSize2, float offset) {
			super(x, y, text, fill, fontName, fontSize1, offset);
			this.text2 = text2;
			this.font2 = new Font(fontName, Font.PLAIN, fontSize2);
		}
		
		public void draw(Graphics2D g) {
			bounds.height = Math.max(font.getSize(), font2.getSize());
			g.setColor(fill);
			g.setFont(font2);
			FontMetrics fm = g.getFontMetrics();
			float sw2 = fm.stringWidth(text2);
			g.setFont(font);
			fm = g.getFontMetrics();
			float sw1 = fm.stringWidth(text);
			float nx = bounds.x - (sw1 + sw2) * offset;
			if (bounds.width == 0)
				bounds.width = (int) (sw1 + sw2);
			g.drawString(text, nx, bounds.y);
			g.setFont(font2);
			g.drawString(text2, nx + sw1, bounds.y);
		}
	}
	
	// an image
	public class ImageMenuItem extends MenuItem {
		public String file;
        public BufferedImage image;
        public ImageMenuItem(String file, int x, int y, int w, int h) {
			super(x, y, w, h);
			this.file = file;
            this.image = getImage(file);
		}
		public void draw(Graphics2D g) {
			// transform image based on set width and height
			AffineTransform at = new AffineTransform();
			at.setToIdentity();
			double scaleX = bounds.getWidth() / image.getWidth();
			double scaleY = bounds.getHeight() / image.getHeight();
			at.translate(bounds.getX(), bounds.getY());
			at.scale(scaleX, scaleY);
            g.drawImage(image, at, null);
		}
		public void onClick() {}
	}
    
	// an image that is filtered when hovered
    public class HoverableImage extends ImageMenuItem {
		public BufferedImage hoveredImage, original;
        
		private RescaleOp brighter = new RescaleOp(1.3f, 0, null);
		
        public HoverableImage(String file, int x, int y, int w, int h) {
            super(file, x, y, w, h);
			this.original = image;
            this.hoveredImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            ((Graphics2D) hoveredImage.getGraphics()).drawImage(image, null, null);
            brighter.filter(hoveredImage, hoveredImage);
        }
        public void draw(Graphics2D g) {
            if (isHovered()) {
				image = hoveredImage;
				super.draw(g);
				image = original;
			}
			else {
				image = original;
				super.draw(g);
			}
        }
        public void onClick() {}
    }
}

// menu template with a black box and a close button
class MenuWithBackground extends Menu {
    
    protected int padding;
    protected Menu previous;
    protected MenuBox background;
	protected MenuBox closeButton;
	public Menu getPreviousMenu() { return previous; }
       
	// default constructor with padding to the bounded box
    public MenuWithBackground(Menu previous) { this(previous, 60); }
    
	// overrde this method when you want some event to occur on close
    public void onClose() {
        // go back to previous menu
        MenuWithBackground.this.setVisible(false);
        if (getPreviousMenu() != null)
            getPreviousMenu().setVisible(true);
        // interrupt game
        if ((Game.activeGame() instanceof ServerGame) || (Game.activeGame() instanceof ClientGame)) {
            Game.activeGame().dispose();
        }
    }
    
    public MenuWithBackground(Menu previous, int padding) {
        this.previous = previous;
        this.padding = padding;
		
		int closeSize = 30;
		
        this.background = new MenuBox(padding, padding, Global.WIDTH - padding * 2, Global.HEIGHT - padding * 2, Global.transparency(Color.BLACK, 0.88f));
		this.closeButton = new MenuBox(Global.WIDTH - padding - closeSize, padding, closeSize, closeSize, Color.RED) {
			@Override
			public void draw(Graphics2D g) {
				super.draw(g);
				g.setColor(Color.WHITE);
				g.drawLine(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);
				g.drawLine(bounds.x + bounds.width, bounds.y, bounds.x, bounds.y + bounds.height);
			}
			
            @Override
            public void onClick() {
                onClose();
            }
        };
        items.add(background);
		items.add(closeButton);
    }
    
    
}






