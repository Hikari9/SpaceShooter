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

public class Game extends JComponent implements Animatable {

	static Game _active;
    public static Game activeGame() { return _active; }
	public static void setActiveGame(Game g) { _active = g; }
	
	
    public static void main(String[] args) {
        
		Global.debugging = true;
        
        // make loading frame
        
        JFrame loading = Global.getLoadingFrame();
		
        loading.setVisible(true);
        
        // make frame
                
		final JFrame frame = new JFrame("Test");
        
        // make game
        
		final Game game = new Game();
                
        // add player ship
        
		final PlayerShip player = new PlayerShip("Rico", game.nextColor(), game, new Controls());
        game.addShip(player);
		
        player.getControls().setContainer(frame);
		
        final Ship opponent = new Ship("Opponent", game.nextColor());
        game.addShip(opponent);
		
		game.addShip(Global.spawnRandomTurret());
		
        // add full screen thingamajig
        
		FullScreenToggle.addToggleToGame(frame, game, KeyEvent.VK_F11);
		
        // add game to frame
		
        frame.add(game);
        
        
        // show frame
        
        loading.setVisible(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.pack();		 
        frame.setVisible(true);
		
        // start the game
        
		game.startTheGame();
		
		game.collisionDetector.start();
		for (Ship s : game.ships)
			s.startSpawningBullets();
		for (Ship t : game.turrets)
			t.startSpawningBullets();
			
		game.powerSpawner(1).start();
	}
	
	public int getPowerSpawnInterval() {
		return 5;
	}
	
	public int getTimeLimit() {
		return 10;
	}
	
    public String getGameType() {
        return "Default";
    }
    
	public PlayerShip getPlayerShip() {
		for (Ship s : ships)
			if (s instanceof PlayerShip)
				return (PlayerShip) s;
		return null;
	}
	
	public void startTheGame() {
		
		repainter.start();
		animator.start();
				
		PlayerShip player = getPlayerShip();
		if (player != null) {
			RuledPlane.addDefault(this, player);
			addHud(new Hud(player));
		}
		addCrosshair();
		
		
		
	}
    
    public void dispose() {
		if (isDisposed()) return;
		_disposed = true;
        chat.clear();
		chat.truncater.stop();
        animator.stop();
        repainter.stop();
        collisionDetector.stop();
        cShip.clear();
        sShip.clear();
        fadeList.clear();
        layers.clear();
		if (powerSpawnerInstance != null)
			powerSpawnerInstance.stop();
		for (Ship s : ships) {
			s.dispose();
		}
		for (Ship s : turrets) {
			s.dispose();
		}
        ships.clear();
        turrets.clear();
    }
	
	protected boolean _disposed = false;
	public boolean isDisposed() {
		return _disposed;
	}
	
    
    public JFrame frame;
	Hud hud = null;
    ChatBox chat = new ChatBox();
    public ChatBox getChatBox() { return chat; }
	public void setChatBox(ChatBox cb) { chat = cb; }
    public void log(Object message) {
        getChatBox().append(message);
    }
	
	
    public final FixedTimer animator;
	public final FixedTimer repainter;
	public final FixedTimer collisionDetector;
	
    public final AnimaVector<Ship> ships = new AnimaVector<Ship>();
	public final AnimaQueue<Power> powers = new AnimaQueue<Power>() {
		@Override
		public void animate() {
			super.animate();
			for (Iterator<Power> it = iterator(); it.hasNext();) {
				Power p = it.next();
				if (p.outOfBounds())
					it.remove();
			}
		}
	};
    public final AnimaQueue<Ship> turrets = new AnimaQueue<Ship>() {
        @Override
        public void animate() {
            super.animate();
            for (Iterator<Ship> it = iterator(); it.hasNext(); ) {
				Ship s = it.next();
                if (!s.restrictBounds && s.outOfBounds()) {
					s.stopSpawningBullets();
					if (s.getBulletSet().isEmpty())
						it.remove();
				}
            }
        }
    };
	
	final HashMap<Color, Ship> cShip = new HashMap<Color, Ship>();
	final HashMap<String, Ship> sShip = new HashMap<String, Ship>();
	
	public final BulletDrawer bulletDrawer = new BulletDrawer();
	final public FadeList fadeList = new FadeList();
    
    public final ConcurrentSkipListMap<Integer, Drawable> layers = new ConcurrentSkipListMap<Integer, Drawable>() {{
        // background
        put(Integer.MIN_VALUE, new Drawable() {public void draw(Graphics2D g) {g.setColor(Color.BLACK); g.fillRect(0, 0, Global.WIDTH, Global.HEIGHT);}});
        put(0, chat);
		put(1, bulletDrawer);
		put(2, powers);
        put(3, turrets);
		put(4, ships);
        put(5, fadeList);
		// draw cursor
		// put(10001, new Crosshair());
    }};
	
	public void addCrosshair() { addLayer(new Crosshair(), 10001); }
    
    public Game() {
        super();
		_active = this;
        this.setPreferredSize(new Dimension(Global.WIDTH, Global.HEIGHT));
        this.setSize(Global.WIDTH, Global.HEIGHT);
        this.setOpaque(true);
        
        this.repainter = new FixedTimer(new Repainter());
        this.animator = new FixedTimer(new Animator());
        this.collisionDetector = new FixedTimer(new CollisionDetector());
    }
    
    public void addLayer(Drawable layer, int Z) {
        layers.put(Z, layer);
    }
    
    public void addLayer(Drawable layer) {
        layers.put(layers.lastKey() + 1, layer);
    }
	
	public void removeLayer(Drawable layer) {
		layers.values().remove(layer);
	}
    
	public void addMenu(Menu menu) {
		addLayer(menu);
        addMouseListener(menu.getMouseListener());
	}
	
	public JFrame getFrame() {
		for (Container c = this; c != null; c = c.getParent()) {
			if (c instanceof JFrame)
				return (JFrame) c;
		}
		return null;
	}
	
    public void addShip(Ship s) {
        if (s.getDesign() instanceof Design.Turret) {
            s.restrictBounds = false;
            turrets.offer(s);
        }
        else {
            cShip.put(s.fill, s);
            sShip.put(s.getName(), s);
            s.index = ships.size();
            ships.add(s);
        }
    }
	
	public void addPower(Power p) {
		powers.add(p);
	}
	
	public void addHud(Hud hud) {
		this.hud = hud;
		addLayer(hud, 10002);
	}
	
	public Hud getHud() { return hud; }
    
	public void removePower(Power p) {
		p.x = p.y = -100000;
	}
	
	public Ship getShip(Color color) {
		return cShip.get(color);
	}
    
    public Ship getShip(String name) {
		return sShip.get(name);
    }
	
	public java.util.List<Ship> getShips() { return ships; }
    
	@Override
    public void animate() {
		for (Drawable layer : layers.values()) {
			if (layer instanceof Animatable) {
				((Animatable) layer).animate();
			}
		}
    }
	
	public double getScaleX() {
		return (double) getWidth() / Global.WIDTH;
	}
	
	public double getScaleY() {
		return (double) getHeight() / Global.HEIGHT;
	}
	
	public Point2D.Double getScaledMousePosition() {
		// Point mouse = getMousePosition();
		Point mouse = MouseInfo.getPointerInfo().getLocation();
		SwingUtilities.convertPointFromScreen(mouse, this);
		if (mouse == null) return null;
		return new Point2D.Double(mouse.x / getScaleX(), mouse.y / getScaleY());
	}
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHints(Global.AntiAliasing);
		g2.scale(getScaleX(), getScaleY());
        for (Drawable layer : layers.values())
            layer.draw(g2);
    }
    
    public static Color[] COLORS = new Color[] {
		Color.GREEN,
        Color.RED,
        Color.CYAN,
		Color.MAGENTA,
		new Color(255, 72, 0),
		new Color(7, 32, 241),
        Color.ORANGE,
        Color.WHITE
    };
    
    public Color nextColor() {
		for (Color c : COLORS) {
			if (getShip(c) == null)
				return c;
		}
		return new Color(
			Global.random(255),
			Global.random(255),
			Global.random(255)
		);
    }
	
	
	boolean ended = false;
	
	public boolean hasEnded() {
		return ended || isDisposed();
	}
	
	public void endTheGame(final String text) {
		endTheGame();
        final CountDownLatch latch = new CountDownLatch(1);
        Transform m = new Transform(Global.WIDTH - 1, Global.HEIGHT / 2, Color.WHITE, null, false) {
			{
				maxspeed = 1e6f;
                vx = tx = -300;
			}
			Font font = new Font(Global.FONT, Font.PLAIN, 100);
			float sw = -1;
			public void draw(Graphics2D g) {
				super.draw(g);
				g.setColor(getFill());
				g.setFont(font);
				if (sw == -1) {
					FontMetrics fm = g.getFontMetrics();
					sw = fm.stringWidth(text);
				}
				g.drawString(text, (int) x, (int) y);
			}
			public void animate() {
				super.animate();
				if (x + sw / 2 <= Global.WIDTH / 2) {
                    tx = vx = 0;
                    latch.countDown();
                }
			}
		};
        addLayer(m);
        try {
            latch.await();
			Sounds.gameOver.play();
            // sleep for a little
            Thread.currentThread().sleep(4500);
        }
        catch (InterruptedException ex) {
            System.err.println("Interrupted end of game message: " + ex);
            Global.onException();
        }
        // no need to remove layer
        removeLayer(m);
	}
	
	public void endTheGame() {
		ended = true;
		collisionDetector.stop();
		if (powerSpawnerInstance != null)
			powerSpawnerInstance.stop();
		for (Ship s : ships) {
			s.stopSpawningBullets();
			s.tx = 0;
			s.ty = 0;
			if (s instanceof PlayerShip) {
				PlayerShip p = (PlayerShip) s;
				p.getControls().enabled = false;
				p.tx = p.ty = 0;
			}
		}
		for (Ship s : turrets) {
			s.stopSpawningBullets();
			s.tx = 0;
			s.ty = 0;
		}
		for (Power p : powers) {
			p.tx = 0;
			p.ty = 0;
		}
	}
	
	public void kill(final Ship killer, final Ship ship) {
        
		if (ship.dead || ship.shield > 0) return;
        if (killer != null) {
            killer.kills++;
            Global.log(killer + " killed " + ship + "!");
        }
        else {
            Global.log(ship + " was killed!");
        }

		
		if (ship.life > 0) {
			ship.life--;
			if (ship.life == 0) {
				Global.log(ship + " ran out of life!");
				ship.dead = true;
				ship.reset();
				
				// check if there is only one player left
				int alive = 0;
				for (Ship s : ships) {
					if (s.life != 0)
						alive++;
				}
				
				if (alive <= 1 && getHud() != null) {
					endTheGame("Game Over!");
				}
				
				
				return;
			}
		}
        // addLayer(new Sparks(ship.x, ship.y, 400), -3);
        
		ship.dead = true;
		if (ship instanceof PlayerShip) {
			((PlayerShip) ship).getControls().enabled = false;
			ship.tx = ship.ty = 0;
		}
		final int killTime = 2000;
		final int shieldTime = 4000;
		final long timeStamp = System.currentTimeMillis();
        
        // ship.speedScale = 3;
		
		final FixedTimer blink = new FixedTimer(new FixedTask() {
			public long getTime() { return System.currentTimeMillis() - timeStamp; }
			public float FPS() {
				return 12f;
			}
			public void run() {
				ship.visible ^= true;
			}
		});
		
		final DelayTimer shield = new DelayTimer(killTime / 1000f) {
			public void run() {
                blink.stop();
                // ship.speedScale = 0;
				ship.visible = true;
				ship.dead = false;
				if (ship instanceof PlayerShip && !hasEnded()) {
					((PlayerShip) ship).getControls().enabled = true;
				}
				ship.shield = shieldTime / 1000;
			}
		};
        
        blink.start();
        shield.start();
				
	}
	
	public void threadedMarquee(final String text) {
		FixedTimer.schedule(new Runnable() {
			public void run() {
				startMarquee(text);
			}
		});
	}
	
	public void threadedMarquee(final String text, final float speed) {
		FixedTimer.schedule(new Runnable() {
			public void run() {
				startMarquee(text, speed);
			}
		});
	}
	
	public void startMarquee(final String text) {
		startMarquee(text, 2500);
	}
	
	public void startMarquee(final String text, final float speed) {
		final CountDownLatch latch = new CountDownLatch(1);
		Transform m = new Transform(Global.WIDTH - 1, Global.HEIGHT / 2, Color.WHITE, null, false) {
			{
				maxspeed = 1e6f;
				acceleration = 10;
			}
			Font font = new Font(Global.FONT, Font.PLAIN, (speed <= Global.STABLE_MARQUEE ?  40 : 100));
			float sw = -1;
			public void draw(Graphics2D g) {
				super.draw(g);
				g.setColor(getFill());
				g.setFont(font);
				if (sw == -1) {
					FontMetrics fm = g.getFontMetrics();
					sw = fm.stringWidth(text);
				}
				g.drawString(text, (int) x, (int) y);
			}
			public void animate() {
				super.animate();
				if (Math.abs(x - Global.WIDTH / 2 - 75) < 150)
					tx = -Global.STABLE_MARQUEE;
				else
					tx = -speed;
				if (x < -sw) {
					latch.countDown();
				}
			}
		};
		addLayer(m);
		try { latch.await(); }
		catch (InterruptedException ex) {
			System.err.println("Interrupted marquee: " + ex);
			Global.onException();
		}
		removeLayer(m);
	}
    
    class Animator extends FixedTask {
		public boolean fixedRate() { return true; }
		public float FPS() { return Global.AnimateFPS; }
        public void run() {
            animate();
        }
    }
    
    class Repainter extends FixedTask {
        public boolean fixedRate() { return true; }
		public float FPS() { return Global.FPS; }
        public void run() {
            repaint();
        }
    }
	
	FixedTimer powerSpawnerInstance = null;
    
    public FixedTimer powerSpawner(final float intervalTime) {
		if (powerSpawnerInstance != null)
			powerSpawnerInstance.stop();
        powerSpawnerInstance = new FixedTimer(new FixedTask() {
            public float FPS() { return 1 / intervalTime; }
            public void run() {
                Power power = Global.spawnRandomPower(Global.random(50f, 100f));
                addPower(power);
            }
        });
		return powerSpawnerInstance;
    }
    
    public FadeList getFadeList() { return fadeList; }
    
    public void fadeLog(String text, float x, float y, Color color) {
        fadeList.add(new FadeText(text, x, y, Global.fadeTimeout, color));
    }
	
	class CollisionDetector extends FixedTask {
		public boolean fixedRate() { return true; }
		public float FPS() { return Global.AnimateFPS; }
		public void run() {
            for (Ship current : ships) {
                if (current != null && current.visible && !current.dead && current.life != 0) {
                
                    // check collision with power ups
                    
                    Transform nearest = null;
                    
                    for (Power power : powers) {
                        if (nearest == null || current.dist2(power) < current.dist2(nearest))
                            nearest = power;
                    }
                    if (nearest != null && current.collidesWith(nearest)) {
                        performCollision(current, nearest);
                    }
                    
                    // check collision of current ship with bullets/other ships
                    if (current.shield == 0) {
                        nearest = null;
                        
                        for (Ship test : ships) {
                            if (current == test) continue;
                            if (!test.dead && test.visible && (nearest == null || current.dist2(test) < current.dist2(nearest)))
                                nearest = test;
                            BulletSet bullets = test.getBulletSet();
                            for (Bullet bullet : bullets) {
                                if (nearest == null || current.dist2(bullet) < current.dist2(nearest))
                                    nearest = bullet;
                            }
                        }
                        
                        for (Ship test : turrets) {
                            BulletSet bullets = test.getBulletSet();
                            for (Bullet bullet : bullets) {
                                if (nearest == null || current.dist2(bullet) < current.dist2(nearest))
                                    nearest = bullet;
                            }
                        }
                        
                        if (nearest != null && current.collidesWith(nearest)) 
                            performCollision(current, nearest);
                        
                    }
                }
            }
		}
		
		public void performCollision(final Ship ship, final Transform other) {
			if (other instanceof Ship) {
				Ship s = (Ship) other;
				if (!s.dead && s.shield == 0 && !ship.dead && ship.shield == 0) {
					Global.log(s + " and " + ship + " bumped with each other!");
					// don't add kills
					s.kills--;
					ship.kills--;
				}
                if (!s.dead && s.shield == 0)
                    kill(ship, s);
                if (!ship.dead && ship.shield == 0)
                    kill(s, ship);
			}
			else if (other instanceof Bullet) {		
				Bullet b = (Bullet) other;
				Ship killer = cShip.get(b.fill);
				kill(killer, ship);
			}
			else if (other instanceof Power) {
				Power power = (Power) other;
				Global.powerUp(power, ship);
                Global.fadeLog(power.toString(), power.x, power.y, ship.fill);
                removePower(power);
			}
		}
	}
	
    
	class BulletDrawer implements Drawable {
		@Override
		public void draw(Graphics2D g) {
            for (Ship t : turrets) {
                t.getBulletSet().draw(g);
            }
			for (Ship s : ships) {
				s.getBulletSet().draw(g);
			}
		}
	}
	
	class Crosshair implements Drawable {
		
		BufferedImage image;
		public Crosshair() {
			try { image = ImageIO.read(new File("icons/crosshair.png")); }
			catch (IOException ex) { System.err.println("No crosshair image found! " + ex); }
		}
		float radius = 35;
		public void draw(Graphics2D g) {
			Point2D mouse = getScaledMousePosition();
			if (image == null || mouse == null) return;
			AffineTransform at = new AffineTransform();
			at.translate(mouse.getX() - radius, mouse.getY() - radius);
			
			double scaleX = 2 * radius / image.getWidth();
			double scaleY = 2 * radius / image.getHeight();
			
			at.scale(scaleX, scaleY);
			
			g.drawImage(image, at, null);
		}
	}
    
}

class AnimaVector<T extends Drawable & Animatable> extends CopyOnWriteArrayList<T> implements Drawable, Animatable {
	@Override
	public void draw(Graphics2D g) {
		for (T object : this) {
            object.draw(g);
        }
	}
	@Override
	public void animate() {
		for (T object : this) {
            object.animate();
        }
	}
}

class AnimaQueue<T extends Drawable & Animatable> extends ConcurrentLinkedQueue<T> implements Drawable, Animatable {
	@Override
	public void draw(Graphics2D g) {
		for (T object : this) {
            object.draw(g);
        }
	}
	@Override
	public void animate() {
		for (T object : this) {
            object.animate();
        }
	}
}
