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

class ClientNameException extends Exception {
    public ClientNameException() { super(); }
    public ClientNameException(String explanation) { super(explanation); }
}

public class ClientGame extends Game {
    
    String ip, name;
    String playerNames[];
    
    ClientStream mainServer;
    ClientByteStream playerByteStreams[], bulletStream, powerStream, turretStream, powerRemover, chatLog, fadeLog, serverTime;
    
    int playerID; // 0-based
    
    public static void main(String[] args) {
        
        Global.debugging = true;
        
        // make frame
        
        final JFrame frame = new JFrame("Test Client Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // make game
        ClientGame game = null;
        String[] names = {
            "Alpha", "Beta", "Charlie", "Delta", "Epsilon", "Foxtrot", "Gamma", "Hurricane"
        };
        int ptr = 0;
		
        while (true) {
            try {
                game = new ClientGame(names[ptr++], (args.length > 1 ? args[1] : "localhost"));
                break;
            }
            catch (ClientNameException ex) {
                System.err.println(ex);
            }
			catch (IOException ex) {
				System.err.println("Could not connect to server");
			}
        }
		
        // add controls to frame
        
        game.getPlayerShip().getControls().setContainer(frame);
        
        // add full screen toggle
        
        FullScreenToggle.addToggleToGame(frame, game, KeyEvent.VK_F11);
        
        // add game to frame
        
        frame.add(game);
        
        
        // show frame
        
        frame.pack();
        frame.setVisible(true);
        
		game.startTheGame();
		
    }
    
    String gameType = "";
    
    @Override
    public String getGameType() {
        return gameType;
    }
	
	@Override
	public void startTheGame() {
		
		gameType = "";
		
		try {
			// receive ping from server
			gameType = mainServer.readLine();
		}
		catch (IOException ex) {
			System.err.println("Could not communicate with server: " + ex);
			Global.onException();
		}
		
		super.startTheGame();
		
		for (FixedTimer timer : timers)
			timer.start();
		
		startMarquee(getGameType(), 1000);
		startMarquee("3");
		startMarquee("2");
		startMarquee("1");
		
		// receive ping that the game is starting
		
		try {
			// ping back that client is ready
			mainServer.println();
			mainServer.flush();
			// wait for if server is ready
			mainServer.readLine();
		}
		catch (IOException ex) {
			System.err.println("Could not communicate with server: " + ex);
			Global.onException();
		}
		
		turnOnEndGameReceiver();
		startMarquee("START!");
		
		
	}
    
    @Override
    public void endTheGame(String message) {
		super.endTheGame(message);
		closeNetworking();
        addMenu(new EndGameMenu());
    }
    
    public int getPlayerID() { return playerID + 1; }
    public int numPlayers() { return playerNames.length; }
    public String getIP() { return ip; }
    public String getPlayerName(int i) { return playerNames[i - 1]; }
	@Override
    public PlayerShip getPlayerShip() { return (PlayerShip) ships.get(playerID); }
    public ClientStream getMainServer() { return mainServer; }
	
	public final ArrayList<FixedTimer> timers = new ArrayList<FixedTimer>();
    
    public ClientGame(String name, String ip) throws ClientNameException, IOException {
        
        super();
        this.name = name;
        this.ip = ip;
        
        // try {
            connectToMainServer();
            registerName();
            waitForOtherPlayers();
            createBytePorts();
            createShips();
            turnOnBulletReceiver();
            turnOnTurretReceiver();
            turnOnPowerReceiver();
            turnOnChatLog();
            turnOnFadeLog();
			turnOnServerTime();
			// turnOnEndGameReceiver();
            Global.log("Successfully created client game.");
        // }
        // catch (IOException ex) {
            // System.err.println("Exception occured: " + ex.toString());
            // Global.onException();
        // }
        
    }
    
    void connectToMainServer() throws IOException {
		
        while (true) {
			if (isDisposed()) throw new IOException("Client game disposed");
            Global.log("Connecting to main server...");
            try {
                mainServer = new ClientStream(ip, Global.PORT);
                break;
            }
			catch (UnknownHostException ex) {
				throw ex;
			}
            catch (IOException ex) {
			}
        }
        
        Global.log("Connected to server [" + ip + " : " + Global.PORT + "]");
               
    }
    
    void registerName() throws IOException, ClientNameException {
        
        // send name of player to main server
        Global.log("Sending player information [" + name + "]...");
        mainServer.out.println(name);
		mainServer.out.flush();
        Global.log("Checking if name already exists...");
        
        // check if registered
        String s = mainServer.readLine();
        if (!s.equals("OK")) {
            Global.log("Error! Name already exists. ClientNameException thrown.");
            throw new ClientNameException("Client name [" + name + "] already exists in server");
        }
        
        Global.log("Successfully registered as [" + name + "]");
        
        
    }
    
    void waitForOtherPlayers() throws IOException {
        
        Global.log("Waiting for other players to register...");
        
        int n = Integer.parseInt(mainServer.readLine());
        playerNames = new String[n];
        
        for (int i = 0; i < n; ++i) {
            String info = mainServer.readLine();
            playerNames[i] = info;
        }
        
        // ping back to server for an OK
        mainServer.println();
		mainServer.flush();
        
        Global.log("List of Players:");
        for (int i = 0; i < n; ++i)
            Global.log("\t" + (i + 1) + ": " + playerNames[i]);
        
        // get player ID
        for (int i = 0; i < n; ++i) {
            if (name.equals(playerNames[i])) {
                playerID = i;
                break;
            }
        }
        
        Global.log("Player ID: " + (playerID + 1));
        
    }
    
    void createBytePorts() throws IOException {
        
        Global.log("Creating byte ports...");
        
        int n = numPlayers();
        playerByteStreams = new ClientByteStream[n];
        
        for (int i = 0; i < n; ++i) {
            
            ClientByteStream stream = null;
            
            while (true) {
			
				if (isDisposed()) throw new IOException("Client game disposed");
                Global.log("Connecting to port " + Global.playerPort(i) + "...");
                try {
                    stream = playerByteStreams[i] = new ClientByteStream(ip, Global.playerPort(i), Ship.bufferSize());
                    break;
                }
                catch (IOException ex) {
                }
            }
            
            Global.log("Connected!");
            Global.log("Sending client information...");
            
            // first byte is player ID
            
            stream.out.write((byte) playerID);
            stream.out.flush();
            Global.log("Done!");            
        }
        
        Global.log("Done creating byte ports!");
        
    }
    
    
    void createShips() {
        
        Global.log("Creating ships...");
        int n = numPlayers();
        
        for (int i = 0; i < n; ++i) {
            
            Global.log("Creating ship for Player " + (i + 1) + " [" + playerNames[i] + "]...");
            
            final int I = i;
            
            if (i == playerID) {
                final PlayerShip ship = new PlayerShip(name, nextColor(), this, new Controls());
                ship.getControls().enabled = false;
				timers.add(
				new FixedTimer(new FixedTask() {
					public boolean fixedRate() { return true; }
					public float FPS() { return Global.SendFPS; }
					public void run() {
                        try {
                            sendMessage(ship.getControlBytes());
                        }
						catch (IOException ex) {
							System.err.println("An exception occured via the ship of Player " + (I + 1) + " [" + playerNames[I] + "]: " + ex.toString());
							Global.onException();
                            stop();
						}
					}
				}));
				timers.add(
                new FixedTimer(new FixedTask() {
                    public boolean fixedRate() { return true; }
                    public float FPS() { return Global.ReceiveFPS; }
                    public void run() {
                        byte[] data = null;
                        try {
                            data = playerByteStreams[I].read();
                        }
                        catch (IOException ex) {
                            System.err.println("Error reading ship from server: " + ex);
                            Global.onException();
                            stop();
                            return;
                        }
                        if (data == null) return;
						ship.fromBytes(data);
                    }
                }));
                addShip(ship);
            }
            
            else {
                final Ship ship = new Ship(playerNames[i], nextColor());
				timers.add(
                new FixedTimer(new FixedTask() {
                    public boolean fixedRate() { return true; }
                    public float FPS() { return Global.ReceiveFPS; }
                    public void run() {
                        byte[] data = null;
                        try {
                            data = playerByteStreams[I].read();
                        }
                        catch (IOException ex) {
                            System.err.println("Error reading ship from server: " + ex);
                            Global.onException();
                            stop();
                            return;
                        }
                        if (data == null) return;
						ship.fromBytes(data);
                    }
                }));
                addShip(ship);
                
            }
            
            Global.log("Done!");
        }
        
        Global.log("Done creating ships!");
        
    }
    
    void turnOnPowerReceiver() throws IOException {
        Global.log("Turning on power receiver...");
        while (true) {
		
			if (isDisposed()) throw new IOException("Client game disposed");
            Global.log("Connecting to port " + (Global.powerPort()) + "...");
            try {
                powerStream = new ClientByteStream(ip, Global.powerPort(), Power.bufferSize());
                break;
            }
            catch (IOException ex) {}
        }
        Global.log("Connected!");
		timers.add(
        new FixedTimer(new FixedTask() {
            public boolean fixedRate() { return false; }
            public float FPS() { return Global.ReceiveFPS; }
            public void run() {
                byte[] data = null;
                try { data = powerStream.read(); }
                catch (IOException ex) {
                    Global.onException();
                    stop();
                    return;
                }
                if (data == null) return;
                addPower(Power.fromBytes(data));
            }
        }));
		Global.log("Turning on power remover...");
		while (true) {
			if (isDisposed()) throw new IOException("Client game disposed");
			Global.log("Connecting to port " + Global.powerRemoverPort() + "...");
			try {
				powerRemover = new ClientByteStream(ip, Global.powerRemoverPort(), 2);
				break;
			}
			catch (IOException ex) {}
		}
		Global.log("Connected!");
		timers.add(
		new FixedTimer(new FixedTask() {
			public boolean fixedRate() { return false; }
			public float FPS() { return Global.ReceiveFPS; }
			public void run() {
				byte[] data = null;
				try { data = powerRemover.read(); }
				catch (IOException ex) {
                    System.err.println("Cannot read info from power remover");
                    Global.onException();
                    stop();
                    return;
                }
				if (data == null) return;
				ByteBuffer bb = ByteBuffer.wrap(data);
				short id = bb.getShort();
				for (Power power : powers) {
					if (power.ID == id) {
						removePower(power);
						break;
					}
				}
			}
		}));
    }
	
	void turnOnBulletReceiver() throws IOException {
		Global.log("Turning on bullet receiver...");
        while (true) {
			if (isDisposed()) throw new IOException("Client game disposed");
            Global.log("Connecting to port " + (Global.bulletPort()) + "...");
            try {
                bulletStream = new ClientByteStream(ip, Global.bulletPort(), Bullet.bufferSize());
                break;
            }
            catch (IOException ex) {}
        }
        Global.log("Connected!");
		timers.add(
		new FixedTimer(new FixedTask() {
            public boolean fixedRate() { return true; }
            public float FPS() { return Global.ReceiveFPS * 20; }
            public void run() {
				byte[] data = null;
                try { data = bulletStream.read(); }
                catch (IOException ex) {
                    System.err.println("Bullet receiver error: " + ex);
                    Global.onException();
                    stop();
                    return;
                }
                if (data == null) return;
                Bullet toSpawn = Bullet.fromBytes(data);
                Ship find = cShip.get(toSpawn.getFill());
                if (find == null) {
                    for (Ship s : turrets) {
                        if (s.fill.equals(toSpawn.getFill())) {
                            find = s;
                            break;
                        }
                    }
                }
                if (find == null) return;
                find.getBulletSet().add(toSpawn);
				
            }
        }));
		
	}
    
    void turnOnTurretReceiver() throws IOException {
        Global.log("Turning on turret receiver...");
        while (true) {
			if (isDisposed()) throw new IOException("Client game disposed");
            Global.log("Connecting to port " + Global.turretPort() + "...");
            try {
                turretStream = new ClientByteStream(ip, Global.turretPort(), Ship.bufferSize());
                break;
            }
            catch (IOException ex) {}
        }
        Global.log("Connected!");
		timers.add(
		new FixedTimer(new FixedTask() {
			public boolean fixedRate() { return false; }
			public float FPS() { return Global.ReceiveFPS; }
			public void run() {
				byte[] data = null;
                String name = null;
				try { data = turretStream.read(); name = turretStream.readLine(); }
				catch (IOException ex) {
                    System.err.println("Cannot read info from turret stream");
                    Global.onException();
                    stop();
                    return;
                }
				if (data == null) return;
				Ship s = new Ship(name, Global.transparent);
				s.setDesign(new Design.Turret(s));
				s.fromBytes(data, false);
				addShip(s);
			}
		}));
    }
	
	void turnOnServerTime() throws IOException {
		Global.log("Turning on server time...");
        while (true) {
			if (isDisposed()) throw new IOException("Client game disposed");
            Global.log("Connecting to port " + Global.serverTimePort() + "...");
            try {
                serverTime = new ClientByteStream(ip, Global.serverTimePort(), 2);
                break;
            }
            catch (IOException ex) {}
        }
        Global.log("Connected!");
		timers.add(
			new FixedTimer(new FixedTask() {
				public boolean fixedRate() { return false; }
				public float FPS() { return Global.ReceiveFPS; }
				public void run() {
					byte[] data = null;
					try { data = serverTime.read(); }
					catch (IOException ex) {
						System.err.println("Error reading from server time: " + ex);
						Global.onException();
                        stop();
                        return;
					}
					if (data == null) return;
					ByteBuffer bb = ByteBuffer.wrap(data);
					short time = bb.getShort();
					Game.activeGame().getHud().setTime(time);
				}
			})
		);
	}
    
    
    void turnOnChatLog() throws IOException {
        Global.log("Connecting to chat log...");
		 while (true) {
			if (isDisposed()) throw new IOException("Client game disposed");
            Global.log("Connecting to port " + Global.chatLogPort() + "...");
            try {
                chatLog = new ClientByteStream(ip, Global.chatLogPort(), 1);
                break;
            }
            catch (IOException ex) {}
        }
		Global.log("Connected!");
		timers.add(
        new FixedTimer(new FixedTask() {
            public boolean fixedRate() { return false; }
            public float FPS() { return Global.ReceiveFPS; }
            public void run() {
                String s = null;
                try { s = chatLog.readLine(); }
                catch (IOException ex) {
                    System.err.println("Error reading from chat log: " + ex);
                    Global.onException();
                    stop();
                    return;
                }
                if (s == null) return;
                log(s);
            }
        }));
    }
    
    
    void turnOnFadeLog() throws IOException {
        Global.log("Connecting to fade log...");
        while (true) {
			if (isDisposed()) throw new IOException("Client game disposed");
            Global.log("Connecting to port " + Global.fadeLogPort() + "...");
            try {
                fadeLog = new ClientByteStream(ip, Global.fadeLogPort(), 12);
                break;
            }
            catch (IOException ex) {}
        }
		Global.log("Connected!");
        timers.add(
        new FixedTimer(new FixedTask() {
            public boolean fixedRate() { return false; }
            public float FPS() { return Global.ReceiveFPS; }
            public void run() {
                String s = null;
                byte[] data = null;
                try {
                    data = fadeLog.read();
                    if (data == null) return;
                    s = fadeLog.readLine();
                }
                catch (IOException ex) {
                    System.err.println("Error reading from fade log: " + ex);
                    Global.onException();
                    stop();
                    return;
                }
                if (s == null) return;
                ByteBuffer bb = ByteBuffer.wrap(data);
				float x = bb.getFloat();
				float y = bb.getFloat();
				Color color = Global.IntToColor(bb.getInt());
				
				// if fade color is same as ship color, play power-up sound
				
				if (color.equals(getPlayerShip().fill))
					Sounds.powerUp.play();
					
                fadeLog(s, x, y, color);
            }
        }));
    
    }
	
	void turnOnEndGameReceiver() {
		FixedTimer.schedule(new Runnable() {
			public void run() {
				try {
					String message = mainServer.readLine();
					if (message.startsWith("END:")) {
						endTheGame(message.substring(4));
					}
				}
				catch (IOException ex) {
					System.err.println("Error getting end of game message from server: " + ex);
					Global.onException();
				}
			}
		});
	}
    
    public void sendMessage(byte[] data) throws IOException {
        // check data
        playerByteStreams[playerID].write(data);
        playerByteStreams[playerID].flush();
    }
	
	@Override
	public void dispose() {
		closeNetworking();
		super.dispose();
	}
    
    public void closeNetworking() {
        for (FixedTimer timer : timers)
			timer.stop();
		timers.clear();
		if (Global.connectingSocket != null) {
			try { Global.connectingSocket.close(); }
			catch (IOException ex) {}
		}
		closeClient(mainServer);
		closeClient(bulletStream);
		closeClient(powerStream);
		closeClient(turretStream);
		closeClient(powerRemover);
		closeClient(chatLog);
		closeClient(fadeLog);
		closeClient(serverTime);
		if (playerByteStreams != null) {
			for (int i = 0; i < playerByteStreams.length; ++i) {
				closeClient(playerByteStreams[i]);
			}
		}
    }
	
	void closeClient(AbstractClient c) {
		try {
			if (c != null) {
				try {
					c.close();
				}
				catch (IOException ex) {
					System.err.println("Error upon closing of client: " + ex);
				}
			}
		}
		catch (Exception ex) {
			System.err.println("unknown exception: " + ex);
		}
	}
    
    
}
