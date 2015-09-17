import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.swing.*;

public abstract class AbstractClient {
    
    public Socket socket;
    
    public AbstractClient(Socket s) {
        socket = s;
    }
	
	protected AbstractClient ()  {
	}
    
    public abstract void write(byte[] data) throws IOException;
    public abstract void print(String s) throws IOException;
    public abstract void read(byte[] data) throws IOException;
    public abstract String readLine() throws IOException;
    public abstract void close() throws IOException;
    
    public void flush() {}
    public void print(Object s) throws IOException { print(s.toString()); }
    public void println(Object s) throws IOException { print(s); print("\n"); flush(); }
    public void println() throws IOException { print("\n"); }
    
}

class ClientStream extends AbstractClient {
    
    public BufferedReader in;
    public PrintStream out;
    
    public ClientStream(String ip, int port) throws IOException {
		super();
        socket = new Socket();
		Global.connectingSocket = socket; // allow closing of client while connecting
		socket.connect(new InetSocketAddress(ip, port));
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintStream(((socket.getOutputStream())));
    }
    
    public ClientStream(Socket s) throws IOException {
        super(s);
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new PrintStream(((s.getOutputStream())));
    }
    
    @Override
    public void flush() {
        out.flush();
    }
    
    @Override
    public void read(byte[] data) throws IOException {
        for (int i = 0; i < data.length; ++i) {
			int r = in.read();
			if (r == -1) return;
            data[i] = (byte) r;
		}
    }
    
    @Override
    public String readLine() throws IOException {
        return in.readLine();
    }
        
    @Override
    public void print(String s) throws IOException {
        out.print(s);
    }
    
    @Override
    public void write(byte[] data) throws IOException {
        out.write(data, 0, data.length);
		out.flush();
    }
	
	@Override
	public void close() throws IOException {
		StringBuilder error = new StringBuilder();
		try { socket.close(); }
		catch (IOException ex) { error.append(ex).append('\n'); }
		catch (NullPointerException ex) {}
		try { in.close(); }
		catch (IOException ex) { error.append(ex).append('\n'); }
		catch (NullPointerException ex) {}
		try { out.close(); }
		// catch (IOException ex) { error.append(ex).append('\n'); }
		catch (NullPointerException ex) {}
		if (error.length() > 0)
			throw new IOException (error.toString().trim());
	}
    
}


class ClientByteStream extends AbstractClient {
    
	public BufferedInputStream in;
	public BufferedOutputStream out;
	
	public byte[] buffer;
	public int bufferSize;
	
	
	public ClientByteStream(String ip, int port, int bufferSize) throws IOException {
		socket = new Socket();
		Global.connectingSocket = socket; // allow closing of client while connecting
		
		socket.connect(new InetSocketAddress(ip, port));
		
		in = new BufferedInputStream(socket.getInputStream());
		out = new BufferedOutputStream(socket.getOutputStream());
		
		this.bufferSize = bufferSize;
		this.buffer = new byte[bufferSize];
	}
	
	public ClientByteStream(Socket s, int bufferSize) throws IOException {
		super(s);
        
		in = new BufferedInputStream(s.getInputStream());
		out = new BufferedOutputStream(s.getOutputStream());
        
		this.bufferSize = bufferSize;
		this.buffer = new byte[bufferSize];
		// reader.start();
	}
	    
	boolean isReading = false;
	boolean ok = true;
	
    @Override
	public void read(byte[] buffer) throws IOException {
		if (isReading) return;
		isReading = true;
		for (int i = 0; i < bufferSize; ++i) {
			int r = in.read();
			if (r == -1) {
				ok = false;
				return;
			}
			buffer[i] = (byte) r;
		}
		isReading = false;
	}
	
	public byte[] read() throws IOException {
		if (isReading) return null;
		read(buffer);
		if (!ok) return null;
		return buffer;
	}
    
    @Override
    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = (char) in.read();
            if (c == '\n') break;
            sb.append(c);
        }
        return sb.toString();
    }
   
    @Override
	public void write(byte[] data) throws IOException {
        out.write(data, 0, data.length);
        out.flush();
	}
    
    @Override
    public void print(String s) throws IOException {
        write(s.getBytes());
    }
	
	
	@Override
	public void close() throws IOException {
		StringBuilder error = new StringBuilder();
		try { socket.close(); }
		catch (IOException ex) { error.append(ex).append('\n'); }
		catch (NullPointerException ex) {}
		try { in.close(); }
		catch (IOException ex) { error.append(ex).append('\n'); }
		catch (NullPointerException ex) {}
		try { out.close(); }
		// catch (IOException ex) { error.append(ex).append('\n'); }
		catch (NullPointerException ex) {}
		if (error.length() > 0)
			throw new IOException (error.toString().trim());
	}
	
}


class ClientByteArrayStream extends AbstractClient {
    
	public BufferedInputStream in;
	public BufferedOutputStream out;
		
	public ClientByteArrayStream(String ip, int port) throws IOException {
		super();
		socket = new Socket();
		Global.connectingSocket = socket; // allow closing of client while connecting
		
		socket.connect(new InetSocketAddress(ip, port));
		
		in = new BufferedInputStream(socket.getInputStream());
		out = new BufferedOutputStream(socket.getOutputStream());
	}
	
	public ClientByteArrayStream(Socket s) throws IOException {
		super(s);
        
		in = new BufferedInputStream(s.getInputStream());
		out = new BufferedOutputStream(s.getOutputStream());
    }
	    
	boolean isReading = false;	
	
    @Override
	public void read(byte[] buffer) throws IOException {
		for (int i = 0; i < buffer.length; ++i)
            buffer[i] = (byte) in.read();
	}
	
	public byte[][] read() throws IOException {
		if (isReading) return null;
        isReading = true;
        int n = in.read();
        if (n == 0) {
            isReading = false;
            return null;
        }
        byte[][] data = new byte[n][];
        for (int i = 0; i < n; ++i) {
            int m = in.read();
            data[i] = new byte[m];
            read(data[i]);
        }
        isReading = false;
        return data;
	}
    
    @Override
    public String readLine() throws IOException {
        return "";
    }
   
    @Override
	public void write(byte[] data) throws IOException {
        out.write(data, 0, data.length);
        out.flush();
	}
    
    @Override
    public void print(String s) throws IOException {
        write(s.getBytes());
    }
	
	
	@Override
	public void close() throws IOException {
		StringBuilder error = new StringBuilder();
		try { socket.close(); }
		catch (IOException ex) { error.append(ex).append('\n'); }
		catch (NullPointerException ex) {}
		try { in.close(); }
		catch (IOException ex) { error.append(ex).append('\n'); }
		catch (NullPointerException ex) {}
		try { out.close(); }
		// catch (IOException ex) { error.append(ex).append('\n'); }
		catch (NullPointerException ex) {}
		if (error.length() > 0)
			throw new IOException (error.toString().trim());
	}
	
	
}