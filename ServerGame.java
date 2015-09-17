import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.swing.*;

public class ServerGame extends Game {
    
    String playerNames[];
    ServerSocket mainServer = null;
    ClientStream[] players = null;
    ClientByteStream[][] playerByteStreams = null; // [i][i] => controls input/output, [i][j] => shipOutput
    ClientByteStream[] bulletStream = null,
		turretStream = null,
		powerStream = null,
		powerRemover = null,
		fadeLogs = null,
		chatLogs = null,
		serverTime = null;
    
    public int numPlayers() { return playerNames.length; }
    public String getPlayerName(int i) { return playerNames[i - 1]; }
	
	@Override
    public PlayerShip getPlayerShip() { return (PlayerShip) ships.get(0); }
    
	// marker for checking if server is ready to commence
    boolean ready = false;
    public boolean isReady() { return ready; }
    
    @Override
    public String getGameType() { return HostPopup.getGameTypeString(); }
	
	@Override
	public void startTheGame() {
		
		// send time limit first to client
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.putShort((short) getTimeLimit());
		byte[] timeBytes = bb.array();
		
		for (int i = 1; i < numPlayers(); ++i) {
			try {
				// send initial server time
				serverTime[i].write(timeBytes);
				// ping what type of game it is
				players[i].println(getGameType());
				players[i].flush();
			}
			catch (IOException ex) {
				System.err.println("Could not communicate with client: " + ex);
				Global.onException();
			}
		}
		
		super.startTheGame();
		
		// initial ship shield
		for (Ship s : ships)
			s.shield = 10;
		
		// start network timers
		for (FixedTimer timer : timers)
			timer.start();
		
		FixedTimer gameTimer = null;
		if (getTimeLimit() > 0) {
			gameTimer = getHud().startTimer(getTimeLimit());
			gameTimer.stop(true);
		}
		
		startMarquee(getGameType(), 1000);
		startMarquee("3");
		startMarquee("2");
		startMarquee("1");
		
		// receive response that clients are ready
		
		for (int i = 1; i < numPlayers(); ++i) {
			try {
				players[i].readLine();
			}
			catch (IOException ex) {
				System.err.println("Could not communicate with client: " + ex);
				Global.onException();
			}
		}
		
		// ping back that the server is ready
	
		for (int i = 1; i < numPlayers(); ++i) {
			try {
				players[i].println();
				players[i].flush();
			}
			catch (IOException ex) {
				System.err.println("Could not communicate with client: " + ex);
				Global.onException();
			}
		}
		
		
		
			
		for (Ship s : ships)
			s.startSpawningBullets();
		for (Ship t : turrets)
			t.startSpawningBullets();
		collisionDetector.start();
		
		powerSpawner(getPowerSpawnInterval()).start();
		
		if (gameTimer != null)
			gameTimer.start();
		
		startMarquee("START!");
		
	}
	
	@Override
	public void endTheGame(String message) {
		// send end game message to client
		for (int i = 1; i < numPlayers(); ++i) {
			try {
				players[i].println("END:" + message);
				players[i].flush();
			}
			catch (IOException ex) {
				System.err.println("Error sending end message to client: " + ex);
				Global.onException();
			}
		}
			
		super.endTheGame(message);
        addMenu(new EndGameMenu());
		
        // show end of game menu
	}
	
	@Override
	public int getTimeLimit() {
		return HostPopup.getTimeLimit() == 0 ? -1 : HostPopup.getTimeLimit();
	}
	
	public final ArrayList<FixedTimer> timers = new ArrayList<FixedTimer>();
    
	@Override
	public void dispose() {
		closeNetworking();
		super.dispose();
	}
    
    public void closeNetworking() {
        for (FixedTimer timer : timers)
			timer.stop(true);
		timers.clear();
		try {
			mainServer.close();
		}
		catch (IOException ex) {
			System.err.println("Error upon closing main server: " + ex);
		}
		catch (Exception ex) {
			System.err.println("Unknown exception occured: " + ex);
		}
		closeClientArray(players);
		if (playerByteStreams != null) {
			for (int i = 0; i < playerByteStreams.length; ++i) {
				closeClientArray(playerByteStreams[i]);
			}
		}
		closeClientArray(bulletStream);
		closeClientArray(turretStream);
		closeClientArray(powerStream);
		closeClientArray(powerRemover);
		closeClientArray(fadeLogs);
		closeClientArray(serverTime);
    }
	
	void closeClientArray(AbstractClient[] array) {
		try {
		if (array != null) {
			for (int i = 0; i < array.length; ++i) {
				if (array[i] != null) {
					try {
						array[i].close();
					}
					catch (IOException ex) {
						System.err.println("Error upon closing of client: " + ex);
					}
				}
			}
		}
		}
		catch (Exception ex) {
			System.err.println("Unknown exception occured: " + ex);
		}
	}
	
    public ServerGame(int numPlayers, String firstPlayerName) throws IOException  {
        
        super();
        
        playerNames = new String[numPlayers];
        playerNames[0] = firstPlayerName;
        
		createMainServer();
		createBytePorts();
		createShips();
		mainServer.close();
		Global.log("Successfully created Server Game.");
        
        ready = true;
        
    }
	
	@Override
	public void addShip(Ship s) {
		if (s.getDesign() instanceof Design.Turret)
			sendTurretData(s);
		super.addShip(s);
		
	}
    
    void createMainServer() throws IOException {
        
        Global.log("Creating main server...");
        
        int n = numPlayers();
        mainServer = new ServerSocket(Global.PORT);
        
        Global.log("Created server socket at address " + InetAddress.getLocalHost().getHostAddress());
        
        players = new ClientStream[n];
        
        for (int i = 1; i < n; ++i) {
            
            Global.log("Accepting clients... (waiting for " + (n - i) + " more)");
            Socket s = mainServer.accept();
            Global.log("Client found! Creating stream...");
            ClientStream client = new ClientStream(s);
            
            // get name of client
            String name = "";
        
            Global.log("Getting client's name...");
            name = client.readLine();
            boolean exists = false;
            if (name != null) {
                for (int j = 0; j < i; ++j) {
                    if (name.equalsIgnoreCase(playerNames[j])) {
                        exists = true;
                    }
                }
            }
            if (exists || name == null) {
                Global.log("Error: A player already has the name [" + name + "]!");
                client.println("NO");
                client.flush();
                --i;
                continue;
            }
            
            playerNames[i] = name;
            Global.log(name + " registered as Player " + (i + 1) + "!");
            client.println("OK");
            client.flush();
            
            players[i] = client;
            
        }
        
        Global.log("Done accepting clients!");
        
        for (int i = 1; i < n; ++i) {
            ClientStream player = players[i];
            
            Global.log("Sending information to Player " + (i + 1) + " [" + playerNames[i] + "]...");
            
            // first line is number of players
            player.println(n);
            
            // send all player names
            for (int j = 0; j < n; ++j)
                player.println(playerNames[j]);
            
           
            player.flush();
            
            // ping for an OK receive
            Global.log("Confirming reply...");
            String ok = player.readLine();
            Global.log("Player confirmed!");
            
        }
                
        Global.log("Done creating main server!");
    }
    
    void createBytePorts() throws IOException {
        
        Global.log("Creating byte ports...");
        
        int n = numPlayers();
        playerByteStreams = new ClientByteStream[n][n];
        
        Global.log("Creating player ports...");
        
        for (int i = 0; i < n; ++i) {
            
            Global.log("Creating server socket for Player " + (i + 1) + " [" + playerNames[i] + "] at port " + (Global.playerPort(i)) + "...");
            ServerSocket ss = new ServerSocket(Global.playerPort(i));
            Global.log("Created server socket.");
            
            for (int j = 1; j < n; ++j) {
                
                // accept a client
                Global.log("Accepting clients...");
                ClientByteStream client = new ClientByteStream(ss.accept(), Controls.bufferSize());
                
                Global.log("Client found!");
                Global.log("Getting player information...");
                
                // first byte is player ID
                int ID = client.in.read();
                playerByteStreams[i][ID] = client;
                // playerByteStreams[ID][0] = client;
                
                Global.log("Player " + ID + " confirmed.");
            }
            
            ss.close();
            
        }
        
        Global.log("Done creating player ports.");
        
        // get bullet ports
        
        bulletStream = createPorts("bullet", Global.bulletPort());
        turretStream = createPorts("turret", Global.turretPort());
        powerStream = createPorts("power", Global.powerPort());
		powerRemover = createPorts("power remover", Global.powerRemoverPort());
        chatLogs = createPorts("chat log", Global.chatLogPort());
        fadeLogs = createPorts("fade log", Global.fadeLogPort());
        serverTime = createPorts("server time", Global.serverTimePort());
        
        Global.log("Done creating byte ports!");
        
    }
    
    ClientByteStream[] createPorts(String portName, int portNum) throws IOException {
        
        Global.log("Creating " + portName + " ports...");
        Global.log("Creating server socket for " + portName + "s at port " + portNum + "...");
        
        int n = numPlayers();
        
        ClientByteStream[] array = new ClientByteStream[n];
        ServerSocket ss = new ServerSocket(portNum);
        
        Global.log("OK");
        
        for (int i = 1; i < n; ++i) {
            Global.log("Accepting clients...");
            ClientByteStream client = new ClientByteStream(ss.accept(), 1); // all output. no need for buffer size
            
            Global.log("Client connected.");
            // no need for client information. can stay as anonymous
            array[i] = client;
        }
        
        Global.log("Closing server socket...");
        ss.close();
        Global.log("Done!");
        return array;
        
    }
    
    
    @Override
    public void fadeLog(String text, float x, float y, Color color) {
        super.fadeLog(text, x, y, color);
        ByteBuffer bb = ByteBuffer.allocate(12);
        bb.putFloat(x);
        bb.putFloat(y);
        bb.putInt(Global.ColorToInt(color));
        byte[] data = bb.array();
        // send log data to other players
        for (int i = 1; i < numPlayers(); ++i) {
            ClientByteStream client = fadeLogs[i];
            try {
                client.write(data);
                client.println(text);
                client.flush();
            }
            catch (IOException ex) {
                System.err.println("Error in writing fade logs: " + ex);
                Global.onException();
            }
        }
    }
	
    void createShips() throws IOException {
        
        Global.log("Creating ship for Player 1 [" + playerNames[0] + "]...");
        
        int n = numPlayers();
        
        // add host's ship
        
        final PlayerShip first = new PlayerShip(playerNames[0], nextColor(), this, new Controls());
		first.life = (short) (HostPopup.getLife() == 0 ? -1 : HostPopup.getLife());
		
		timers.add(
		new FixedTimer(new FixedTask() {
			public float FPS() { return Global.SendFPS; }
			public boolean fixedRate() { return true; }
			public void run() {
				byte[] data = first.getBytes();
				int n = numPlayers();
				for (int i = 1; i < n; ++i) {
					final ClientByteStream cbs = playerByteStreams[0][i];
					try { cbs.write(data); }
					catch (IOException ex) {
						System.err.println("An exception occured via the ship of Player " + i + " [" + playerNames[i] + "]: " + ex.toString());
						Global.onException();
						stop();
						return;
					}
				}
            }
		}));
        
        addShip(first);
        
        Global.log("Done!");
        
        // add other players
        
        for (int i = 1; i < n; ++i) {
            final int I = i;
            Global.log("Creating ship for Player " + (i + 1) + " [" + playerNames[i] + "]...");
            final Ship ship = new Ship(playerNames[i], nextColor());
			ship.life = (short) (HostPopup.getLife() == 0 ? -1 : HostPopup.getLife());
			timers.add(
			new FixedTimer(new FixedTask() {
				public float FPS() { return Global.ReceiveFPS; }
				public boolean fixedRate() { return true; }
				public void run() {
                    if (hasEnded()) { stop(); return; }
                    byte[] data = receiveMessage(I);
					if (data == null) return;
                    if (hasEnded()) {
                        stop();
                        return;
                    }
					ship.fromControlBytes(data);
				}
			}));
			timers.add(
			new FixedTimer(new FixedTask() {
				public float FPS() { return Global.SendFPS; }
				public boolean fixedRate() { return true; }
				public void run() {
					// sendMessage(I, ship.getBytes());
					byte[] data = ship.getBytes();
					int n = numPlayers();
					for (int i = 1; i < n; ++i) {
						final ClientByteStream cbs = playerByteStreams[I][i];
						try { cbs.write(data); }
						catch (IOException ex) {
							System.err.println("An exception occured via the ship of Player " + i + " [" + playerNames[i] + "]: " + ex.toString());
							Global.onException();
							stop();
							return;
						}
					}
				}
			}));
            ship.spawnRandom();
            addShip(ship);
            Global.log("Done!");
        }
        
        Global.log("Done adding ships!");
        
    }
    
    public void sendMessage(int player, final byte[] data) {
        
    }
	
	public void sendBulletData(Bullet b) {
		byte[] data = b.getBytes();
		int n = numPlayers();
		for (int i = 1; i < n; ++i) {
			ClientByteStream cbs = bulletStream[i];
			try { cbs.write(data); cbs.flush(); }
			catch (IOException ex) {
				System.err.println("An exception occured upon sending bullet info to Player " + i + " [" + playerNames[i] + "]: " + ex);
				Global.onException();
			}
		}
	}
	
	public void sendTurretData(Ship s) {
		byte[] data = s.getBytes();
		int n = numPlayers();
		for (int i = 1; i < n; ++i) {
			ClientByteStream cbs = turretStream[i];
			try { cbs.write(data); cbs.println(s.getName()); cbs.flush(); }
			catch (IOException ex) { System.err.println("Could not write to turret stream " + i ); }
		}
	}
	
	public void sendPowerData(Power p) {
        byte[] data = p.getBytes();
        for (int i = 1; i < numPlayers(); ++i) {
			ClientByteStream cbs = powerStream[i];
            try { cbs.write(data); cbs.flush(); }
            catch (IOException ex) { System.err.println("Could not write to power stream " + i); }
        }
	}
    
    @Override
    public void addPower(Power p) {
		sendPowerData(p);
        super.addPower(p);
    }
    
    @Override
    public void removePower(Power p) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort(p.ID);
        byte[] array = bb.array();
        for (int i = 1; i < numPlayers(); ++i) {
            try {
                powerRemover[i].write(array);
                powerRemover[i].flush();
            }
            catch (IOException ex) {
                System.err.println("Could not write to power stream " + i);
            }
        }
		super.removePower(p);
    }
    
    public byte[] receiveMessage(int player) {
        ClientByteStream cbs = playerByteStreams[player][player];
		byte[] data = null;
		try { data = cbs.read(); } catch (IOException ex) {}
        return data;
    }
	
	public void updateServerTime(int time) {
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.putShort((short) time);
		byte[] data = bb.array();
		for (int i = 1; i < numPlayers(); ++i) {
			try {
				serverTime[i].write(data);
				serverTime[i].flush();
			}
			catch (IOException ex) {
				System.err.println("Error sending server time information to client: " + ex);
				Global.onException();
			}
		}
	}
    
    @Override
    public void log(Object message) {
        super.log(message);
        if (message == null) return;
        for (int i = 1; i < numPlayers(); ++i) {
            try {
                chatLogs[i].println(message);
                chatLogs[i].flush();
            }
            catch (IOException ex) {
                System.err.println("Error writing to player " + i + ": " + ex);
                Global.onException();
            }
        }
    }
	
	// main method for testing server game
    public static void main(String[] args) {
        
        Global.debugging = true;
        
        JFrame loading = Global.getLoadingFrame();
        loading.setVisible(true);
        
        // make frame
        
        final JFrame frame = new JFrame("Test Server Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // make game
        
        int players = (args.length == 1 ? 4 : Integer.parseInt(args[1]));
		
		ServerGame serverGame = null;
		try {
			serverGame = new ServerGame(players, "Rico") {
				// {
					@Override
					public int getTimeLimit() {
						return 1;
					}
				// }
			};
		}
		catch (IOException ex) {
			System.err.println("An exception occured in server game: " + ex);
			Global.onException();
		}
		
        final ServerGame game = serverGame;
		
		
        // add controls to frame
        
        game.getPlayerShip().getControls().setContainer(frame);
        
        // add full screen toggle
        
        FullScreenToggle.addToggleToGame(frame, game, KeyEvent.VK_F11);
		
        // add game to frame
        
        frame.add(game);
        
        // show frame
        
        loading.setVisible(false);
        
        frame.pack();
        frame.setVisible(true);
        
		game.startTheGame();
		
    }
    
    
}

