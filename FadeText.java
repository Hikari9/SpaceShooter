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

class FadeList extends ConcurrentLinkedQueue<FadeText> implements Drawable {
    @Override
    public void draw(Graphics2D g) {
        for (Iterator<FadeText> it = iterator(); it.hasNext(); ) {
            FadeText ft = it.next();
            ft.draw(g);
            if (ft.color.getAlpha() == 0)
                it.remove();
        }
    }
}

public class FadeText implements Drawable {
    
    public static Font font = new Font("Cambria", Font.PLAIN, 12);
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("FadeText");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 300);
        
        final ArrayList<FadeText> fadeList = new ArrayList<FadeText>();
        
        final JComponent comp = new JComponent() {
            
            {
                setPreferredSize(new Dimension(Global.WIDTH, Global.HEIGHT));
                setSize(Global.WIDTH, Global.HEIGHT);
            }
            
            ArrayList<FadeText> list = fadeList;
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, Global.WIDTH, Global.HEIGHT);
                for (FadeText ft : list) {
                    ft.draw(g2);
                }
            }
        };
        
        new FixedTimer(new FixedTask() {
            public void run() {
                comp.repaint();
            }
        }).start();
        
        frame.add(comp);
        frame.pack();
        frame.setVisible(true);
        
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            String text = sc.next();
            int x = Global.random(Global.WIDTH);
            int y = Global.random(Global.HEIGHT);
            float timer = 5;
            fadeList.add(new FadeText(text, x, y, timer));
        }
        
        
    }
    
    public final String text, lines[];
    public Color color;
    public final float x, y, timeout;
    
    public FadeText(String text, float x, float y, final float timeout, Color color) {
        this.text = text;
        lines = text.trim().split("\t");
        this.x = x;
        this.y = y;
        this.timeout = timeout;
        this.color = color;
        final float frames = 255 / timeout;
        new FixedTimer(new FixedTask() {
            FadeText fade = FadeText.this;
            public float FPS() { return 255 / timeout; }
            public void run() {
                int alpha = fade.color.getAlpha();
                fade.color = new Color(fade.color.getRed(), fade.color.getGreen(), fade.color.getBlue(), --alpha);
                if (alpha == 0)
                    stop();
            }
        }).start();
    }
    public FadeText(String text, float x, float y, float timeout) {
        this(text, x, y, timeout, Color.WHITE);
    }
    
    float sw = Float.MAX_VALUE;
    
    @Override
    public void draw(Graphics2D g) {
        if (sw == Float.MAX_VALUE) {
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            sw = fm.stringWidth(lines[0]);
        }
        g.setFont(font);
        g.setColor(color);
        for (int i = 0; i < lines.length; ++i) {
            g.drawString(lines[i], x - sw / 2, y + font.getSize() * i);
        }
    }
}
