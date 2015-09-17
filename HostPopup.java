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


class GameType {
    public static final int DEATHMATCH = 0;
    public static final int SURVIVAL = 1;
    public static int fromString(String s) {
        if (s.equals("DeathMatch"))
            return DEATHMATCH;
        else if (s.equals("Survival"))
            return SURVIVAL;
        return -1;
    }
}

public class HostPopup {
    
    public static final JLabel a = new JLabel("Player name:");
    public static final JTextField b = new JTextField("Player", 20);       
    public static final JLabel c = new JLabel("Time limit:");
    
    
    static MaskFormatter timeFormat;
    
    static {
        try {
            timeFormat = new MaskFormatter("##:##");
            timeFormat.setPlaceholderCharacter('0');
        }
        catch (ParseException ex) {
            System.err.println(ex);
        }
    }
    
    public static final JLabel e = new JLabel("Game type:");
    public static final JRadioButton f = new JRadioButton("DeathMatch", true);
    public static final JRadioButton g = new JRadioButton("Survival", false);
    
    public static final JLabel gameTypeLabel = new JLabel("Game type:");
    public static final JLabel deathMatchLabel = new JLabel("          DeathMatch - most kills");
    public static final JLabel survivalLabel = new JLabel("          Survival - last man standing");
    public static final JLabel zero = new JLabel("Note: ZERO time/life means unlimited");
	
	public static final JToggleButton custom = new JToggleButton("Custom");
    
    static class PresetChooser implements PropertyChangeListener, ActionListener, ChangeListener, Runnable {
        public void run() {
			if (!presetClick)
				custom.setSelected(true);
        }
        public void propertyChange(PropertyChangeEvent e) { run(); }
        public void stateChanged(ChangeEvent e) { run(); }
        public void actionPerformed(ActionEvent e) { run(); }
        private PresetChooser() {}
        static PresetChooser instance = null;
        public static PresetChooser getInstance() {
            if (instance == null)
                instance = new PresetChooser();
            return instance;
        }
    }
    
    static final PresetChooser presetChooser = PresetChooser.getInstance();
    
        
    // public static final JLabel i = new JLabel("Kill cap:");
    // public static final JSpinner j = new JSpinner(new SpinnerNumberModel(20, 0, 100, 1));
    public static final JLabel k = new JLabel("Life:");
    public static final JSpinner l = new JSpinner(new SpinnerNumberModel(20, 0, 100, 1)) {
        {
            addChangeListener(presetChooser);
        }
    };
    
    public static final JLabel m = new JLabel("Players:");
    public static final JSpinner n = new JSpinner(new SpinnerNumberModel(2, 2, 8, 1)) {
        {
            addChangeListener(presetChooser);
        }
    };
    
    public static void setTimeLimit(String s) {
        d.setText(s);
    }
    public static void setTimeLimit(int minutes, int seconds) {
        setTimeLimit(String.format("%02d:%02d", minutes, seconds));
    }
    
    public static void setPlayerName(String s) {
        b.setText(s);
    }
    
    public static void setPlayers(int p) {
        n.setValue(p);
    }
    
    // public static void setKillCap(int cap) {
        // j.setValue(cap);
    // }
    
    public static void setLife(int life) {
        l.setValue(life);
    }
    
    public static void setGameType(int type) {
        if (type == GameType.DEATHMATCH)
            f.setSelected(true);
        else if (type == GameType.SURVIVAL)
            g.setSelected(true);
    }
    
    public static final ButtonGroup presetButtons = new ButtonGroup();
    
    
    
    
    public static final JFormattedTextField d = new JFormattedTextField(timeFormat) {
        {
            setText("05:00");
            setHorizontalAlignment(JTextField.CENTER);
            addPropertyChangeListener("value", presetChooser);
        }
    };
    
    public static final JPanel presets = new JPanel(new GridLayout(3, 3)) {
        {
            JToggleButton def = new PresetButton("Default", "03:00", 2, 10, GameType.SURVIVAL);
            JToggleButton lon = new PresetButton("2 min", "02:00", 2, 0, GameType.DEATHMATCH);
            JToggleButton tim = new PresetButton("5 min", "05:00", 2, 0, GameType.DEATHMATCH);
            JToggleButton sur = new PresetButton("Survival", "00:00", 2, 20, GameType.SURVIVAL);
            JToggleButton thr = new PresetButton("3-p Match", "8:00", 3, 35, GameType.DEATHMATCH);
            JToggleButton sma = new PresetButton("4-p Smash", "10:00", 4, 50, GameType.SURVIVAL);
            JToggleButton roy = new PresetButton("5-p Rumble", "12:00", 5, 65, GameType.SURVIVAL);
            JToggleButton six = new PresetButton("6-p GG", "15:00", 6, 80, GameType.DEATHMATCH);
            add(def); add(lon); add(tim);
            add(sur); add(thr); add(sma);
            add(roy); add(six); add(custom);
            for (Component comp : getComponents()) {
                if (comp instanceof JToggleButton)
                    presetButtons.add((JToggleButton) comp);
            }
        }
    };
    
    public static final JPanel panel = new JPanel(new GridBagLayout()) {
        {
            GridBagConstraints left = new GridBagConstraints();
            GridBagConstraints right = new GridBagConstraints();
            
            GridBagConstraints pad = new GridBagConstraints();
            
            
            left.weightx = 1.5;
            left.gridx = 0;
            left.gridy = 0;
            left.ipadx = 40;
            left.ipady = 30;
            left.insets = new Insets(0, 50, 0, 0);
            right.weightx = 0.7;
            right.gridx = 1;
            right.gridy = 0;
            right.ipadx = 40;
            right.insets = new Insets(0, 0, 0, 50);
            left.fill = GridBagConstraints.PAGE_END;
            right.fill = GridBagConstraints.HORIZONTAL;
            
            
            add(c, left); add(d, right);
            inc(left, right);
            
            
            left.ipady = 0;
            
            add(m, left); add(n, right);
            inc(left, right);
            
            // add(i, left); add(j, right);
            // inc(left, right);

            add(k, left); add(l, right);
            inc(left, right);
            
            left.ipady = right.ipady = 20;
            left.insets = new Insets(0, 45, 0, 0);
            right.insets = new Insets(0, 18, 0, 0);
            add(f, left); add(g, right);
            ButtonGroup bg = new ButtonGroup();
            bg.add(f); bg.add(g);
            
            f.addActionListener(presetChooser);
            g.addActionListener(presetChooser);
            
        }
        void inc(GridBagConstraints left, GridBagConstraints right) {
            left.gridy ++;
            right.gridy++;
        }
    };
    
	static boolean presetClick = false;
    
    public static class PresetButton extends JToggleButton implements ActionListener {
        String timeLimit;
        int players, life, gameType;
        public PresetButton(String name, String timeLimit, int players, int life, int gameType) {
            super(name);
            this.timeLimit = timeLimit;
            this.players = players;
            this.life = life;
            this.gameType = gameType;
            addActionListener(this);
        }
        public void actionPerformed(ActionEvent e) {
			presetClick = true;
            setTimeLimit(timeLimit);
            setPlayers(players);
            setLife(life);
            setGameType(gameType);
			presetClick = false;
        }
        public boolean isSet() {
            return getTimeString().equals(timeLimit) && getPlayers() == players && getLife() == life && getGameType() == gameType;
        }
    }

    
    public static String getPlayerName() {
        return b.getText();
    }
    
    public static String getTimeString() {
        return d.getText();
    }
    
    public static int getTimeLimit() { // seconds
        String text = d.getText();
        int minutes = Integer.parseInt(text.substring(0, 2));
        int seconds = Integer.parseInt(text.substring(3));
        return minutes * 60 + seconds;
    }
    
    // public static int getKillCap() {
        // return (int) j.getValue();
    // }
    
    public static int getLife() {
        return ((Integer) l.getValue()).intValue();
    }
    
    public static int getPlayers() {
        return ((Integer) n.getValue()).intValue();
    }
    
    public static int getGameType() {
        return isDeathMatch() ? GameType.DEATHMATCH : GameType.SURVIVAL;
    }
    
    public static String getGameTypeString() {
        return isDeathMatch() ? "DeathMatch" : "Survival";
    }
    
    public static boolean isDeathMatch() {
        return f.isSelected();
    }
    
    public static boolean isSurvival() {
        return g.isSelected();
    }
    
    static Object[] playerNameArray() {
        return new Object[] {a, b};
    }
    
    static Object[] hostSettingsArray() {
        return new Object[] {presets, panel, "\n", gameTypeLabel, deathMatchLabel, survivalLabel, zero};
    }
    
    public static void main(String[] args) {
        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        
        JButton button = new JButton("Click me");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // System.out.println(playerNameDialog(frame));
                int settings = hostSettings(frame);
                System.out.println("Players: "  + getPlayers());
                // if (settings == JOptionPane.CLOSED_OPTION)
                    // JOptionPane.showMessageDialog(frame, "Closed", "Result", JOptionPane.INFORMATION_MESSAGE);
                // else
                    // JOptionPane.showMessageDialog(frame, options[settings], "Result", JOptionPane.INFORMATION_MESSAGE);
                
            }
        });
        
        frame.add(button);
        frame.setVisible(true);
    }
    
    public static final Object[] options = new Object[] {"Back", "Next"};
    
    static {
        Font font = new Font("Cambria", Font.PLAIN, 14);
        for (Component comp : presets.getComponents()) {
            comp.setFont(font);
        }
        a.setFont(font);
        b.setFont(font);
        c.setFont(font);
        d.setFont(font);
        e.setFont(font);
        f.setFont(font);
        g.setFont(font);
        // i.setFont(font);
        // j.setFont(font);
        k.setFont(font);
        l.setFont(font);
        m.setFont(font);
        n.setFont(font);
        font = font.deriveFont(12f).deriveFont(Font.ITALIC);
        gameTypeLabel.setFont(font);
        deathMatchLabel.setFont(font);
        survivalLabel.setFont(font);
        zero.setFont(font);
        // h.setFont(font);
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
            BufferedReader br = new BufferedReader(new FileReader(new File(Global.HOST_SETTINGS)));
            try {
                String time = br.readLine();
                setTimeLimit(time);
            }
            catch (NumberFormatException ex) {
                System.err.println("Invalid time count: " + ex);
                // leave at default
            }
            try {
                int players = Integer.parseInt(br.readLine());
                setPlayers(players);
            }
            catch (NumberFormatException ex) {
                System.err.println("Invalid player count: " + ex);
            }
            try {
                int life = Integer.parseInt(br.readLine());
                setLife(life);
            }
            catch (NumberFormatException ex) {
                System.err.println("Invalid life count: " + ex);
                // leave at default
            }
            String type = br.readLine();
            if (type.equals("DeathMatch")) {
                setGameType(GameType.DEATHMATCH);
            }
            else if (type.equals("Survival")) {
                setGameType(GameType.SURVIVAL);
            }
            br.close();
        }
        catch (IOException ex) {
            System.err.println("Did not get host settings: " + ex);
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
            PrintWriter pw = new PrintWriter(new FileWriter(Global.HOST_SETTINGS));
            pw.println(getTimeString());
            pw.println(getPlayers());
            pw.println(getLife());
            pw.println(getGameTypeString());
            pw.close();
        }
        catch (IOException ex) {
            System.err.println("Unable to save host settings: " + ex);
        }
    }
    
    public static String playerNameDialog(JFrame frame) {
        getInfo();
        int result = JOptionPane.showOptionDialog(frame, playerNameArray(), "Host Game", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        if (result == 1) {
            saveInfo();
            return getPlayerName();
        }
        else
            return null;
    }
    
    public static int hostSettings(JFrame frame) {
        getInfo();
		for (Component comp : presets.getComponents()) {
			if (comp instanceof PresetButton) {
				PresetButton pb = (PresetButton) comp;
				if (pb.isSet()) {
					pb.setSelected(true);
					break;
				}
			}
		}
		int result = -1;
		do {
			result = JOptionPane.showOptionDialog(frame, hostSettingsArray(), "Host Game Settings", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
			if (result == 1) {
				saveInfo();
				if (isSurvival() && getLife() == 0) {
					JOptionPane.showMessageDialog(frame, "Life should not be zero for survival", "Error", JOptionPane.ERROR_MESSAGE);
				}
				else {
					break;
				}
			}
			else break;
		} while (true);
        return result;
    }
    
}