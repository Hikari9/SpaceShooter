import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.plaf.*;
import java.text.*;

public class ClientPopup {
	
	public static final JLabel a = new JLabel("Player name:");
	public static final JTextField b = new JTextField("Player", 20);  
	public static final JLabel c = new JLabel("Host Name (or IP):");
	public static final JTextField d = new JTextField();
	
	public static final JPanel ab = new JPanel(new GridLayout(1, 2)) {
		{
			add(a);
			add(b);
			setPreferredSize(new Dimension(254, 30));
		}
	};
	
	public static final JPanel cd = new JPanel(new GridLayout(1, 2)) {
		{
			add(c);
			add(d);
			setPreferredSize(new Dimension(254, 30));
		}
	};
	
	public static String getPlayerName() {
		return b.getText();
	}
	
	public static String getHost() {
		return d.getText();
	}
	
	public static void setPlayerName(String s) {
		b.setText(s);
	}
	
	public static void setHost(String s) {
		d.setText(s);
	}
	
	public static void getInfo() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(Global.PLAYER_SETTINGS)));
            String player = br.readLine();
            setPlayerName(player);
            br.close();
        }
        catch (IOException ex) {
            System.err.println("Did not get player name: " + ex);
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(Global.IP_SETTINGS)));
			setHost(br.readLine());
            br.close();
        }
        catch (IOException ex) {
            System.err.println("Did not get ip settings: " + ex);
        }
    }
	
	
    public static void saveInfo() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(Global.PLAYER_SETTINGS));
            pw.println(getPlayerName());
            pw.close();
        }
        catch (IOException ex) {
            System.err.println("Unable to save player name: " + ex);
        }
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(Global.IP_SETTINGS));
            pw.println(getHost());
            pw.close();
        }
        catch (IOException ex) {
            System.err.println("Unable to save ip settings: " + ex);
        }
    }
	
	static {
		Font font = new Font("Cambria", Font.PLAIN, 14);
		a.setFont(font);
		b.setFont(font);
		c.setFont(font);
		d.setFont(font);
	}
	
	public static int displayDialog(JFrame frame) {
		getInfo();
		int res = JOptionPane.showOptionDialog(frame, new Object[] {ab, cd}, "Join game", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, HostPopup.options, HostPopup.options[1]);
		saveInfo();
		return res;
	}
	
}
