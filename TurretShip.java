import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

public class TurretShip extends Ship {
    
    public float omega = 30f; // degrees per second
    
    public TurretShip(String name, Color color) {
        super(name, color);
        setDesign(new Design.Turret(this));
        this.visible = false;
    }
    
    public TurretShip(String name, Color color, float omega) {
        this(name, color);
        this.omega = omega;
    }
	
    @Override
    public Color getFill() { return fill; } // show color even not invisible
    
    @Override
    public void animate() {
        super.animate();
        angle += deltaTime * (float) Math.toRadians(omega);
    }
    
}
