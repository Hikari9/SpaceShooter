import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

// snipes farthest ship
public class FarSniperShip extends Ship {
    
    protected Game game;
    
    public FarSniperShip(String name, Color color, Game game) {
        super(name, color);
        this.game = game;
		this.visible = false;
        setStyle(BulletStyle.SNIPER_SHOT());
        setDesign(new Design.Turret(this));
    }
	
	@Override
	public Color getFill() {
		return fill;
	}
    
    @Override
    public void animate() {        
        super.animate();
        Ship farthest = null;
        for (Ship s : getGame().ships) {
            if (!s.visible || s.dead || s.fill.equals(this.fill)) continue;
            if (farthest == null || dist2(s) > dist2(farthest))
                farthest = s;
        }
		if (farthest == null) return;
        float dx = farthest.x - x;
        float dy = farthest.y - y;
        angle = (float) Math.atan2(dy, dx);
    }
    
    public Game getGame() {
        return game;
    }
    
}
