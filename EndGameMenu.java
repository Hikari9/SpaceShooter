import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;
import javax.sound.sampled.*;

public class EndGameMenu extends MenuWithBackground implements Animatable, Comparator<Ship> {
    
    final ArrayList<Ship> ships = new ArrayList<Ship>();
    float cellX, cellY, lastCellY, deltaY;
    int gameType;
    
	FixedTimer sparksTimer = new FixedTimer(new FixedTask() {
		public float FPS() { return 1f; }
		@Override
		public void run() {
			if (isVisible()) {
				sparks.offer(new Sparks(Global.random(Global.WIDTH), Global.random(Global.HEIGHT), 100));
				while (sparks.size() > 100)
					sparks.poll();
			}
			else {
				sparks.clear();
				stop();
			}
		}
	}).start();
	
	AnimaQueue<Sparks> sparks = new AnimaQueue<Sparks>();
    
    public EndGameMenu() {
        super(null);
		Sounds.bgm.loop(Clip.LOOP_CONTINUOUSLY);
		
		// sparks
		items.add(new MenuItem(0, 0, 1, 1) {
			public void draw(Graphics2D g) {
				sparks.draw(g);
			}
			public void onClick() {}
		});
		
        this.cellX = background.bounds.x + 250;
        this.cellY = background.bounds.y + 175;
        this.lastCellY = background.bounds.y + background.bounds.height - 175;
        this.deltaY = 70;
        
        items.add(new MenuLabel(Global.WIDTH / 2, 150, "Results [" + getGame().getGameType() + "]", Color.WHITE, Global.FONT, 40, CENTER));
        
		
        this.gameType = GameType.fromString(getGame().getGameType());
        
        // sort the ships
        
        for (Ship s : getGame().getShips())
            ships.add(s);
        
        Collections.sort(ships, this);
        
        float curY = cellY + 10;
        int rank = 0;
        Ship prev = null;

		for (Ship s : ships) {
            if (prev == null || prev.kills != s.kills || prev.life != s.life)
                rank++;
            items.add(
                new MenuLabel((int) cellX + 100, (int) curY, Global.ordinal(rank), Color.WHITE, Global.FONT, 30, LEFT)
            );
            items.add(
                new MenuLabel2((int) cellX + 200, (int) curY + 15, "" + s.kills, " KILLS", Color.WHITE, Global.FONT, 30, 15, LEFT)
            );
            items.add(
                new MenuLabel2((int) cellX + 300, (int) curY + 15, s.life == -1 ? "---" : ("" + s.life), " LIFE", Color.WHITE, Global.FONT, 30, 15, LEFT)
            );
            curY += deltaY;
            
            prev = s;
        }
		
        // remove ships layer to draw them on top of results screen later
        // getGame().removeLayer(getGame().ships);
    }   
    
    
    public int compare(Ship a, Ship b) {
        switch (gameType) {
            case GameType.DEATHMATCH:
                if (a.kills == b.kills)
                    return b.life - a.life;
                return b.kills - a.kills;
            case GameType.SURVIVAL:
                if (a.life == b.life)
                    return b.kills - a.kills;
                return b.life - a.life;
        }
        return 0;
    }
    
    long time = System.currentTimeMillis();
    float deltaTime = 0;
    
    @Override
    public void animate() {        
        long nTime = System.currentTimeMillis();
        deltaTime = (nTime - time) / 1000f;
        time = nTime;
        
        // put all ships in order
        float curY = cellY;
        for (Ship s : ships) {
            float dx = cellX - s.x;
            float dy = curY - s.y;
            s.vx = s.vy = s.tx = s.ty = 0;
            s.animate(); // for the sake of the player ship's arrow thing
            float speed = Math.min(1f, deltaTime * 5);
            s.x += dx * speed;
            s.y += dy * speed;
            s.angle += deltaTime;
            curY += deltaY;
        }
		
		sparks.animate();
    }
    
    @Override
    public void draw(Graphics2D g) {
        super.draw(g);
        // redraw ships on top
        for (Ship s : ships) {
			s.draw(g);
		}
    }
    
    @Override
    public void onClose() {
		
		Sounds.bgm.stop();
        Game original = getGame();
        JFrame frame = original.getFrame();
                
        setVisible(false);
        
        // dispose game
		frame.remove(original);
        original.dispose();
        
        // open main menu
        Game game = Global.randomGame();
		frame.add(game);
        
		FullScreenToggle.addToggleToGame(frame, game, KeyEvent.VK_F11);
        
        Menu menu = new MainMenu();
        
		game.addMenu(menu);
        
        // force frame to resize components
        frame.setVisible(true);
        
		game.startTheGame();
		
    }
}