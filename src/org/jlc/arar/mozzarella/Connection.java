package org.jlc.arar.mozzarella;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import fr.berger.enhancedlist.Couple;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class Connection {
	
	public static final int MAX_DATA_BYTE_LENGTH = 1024;
	
	@Nullable
	private DatagramSocket datagramSocket;
	@NotNull
	private InetAddress address;
	private int port;
	
	@Nullable
	private DatagramPacket lastReceivedPacket;
	
	/* CONSTRUCTOR */
	
	public Connection(@NotNull InetAddress address, int port) {
		setDatagramSocket(null);
		setAddress(address);
		setPort(port);
		setLastReceivedPacket(null);
	}
	public Connection(@NotNull Connection connection) {
		if (connection == null)
			throw new NullPointerException();
		
		setDatagramSocket(connection.getDatagramSocket());
		setAddress(connection.getAddress());
		setPort(connection.getPort());
		setLastReceivedPacket(connection.getLastReceivedPacket());
	}
	
	/* CONNECTION METHODS */
	
	public synchronized boolean send(@NotNull String data) {
		boolean result;
		
		try {
			if (getDatagramSocket() == null)
				setDatagramSocket(new DatagramSocket());
			DatagramPacket dp = new DatagramPacket(data.getBytes(), data.length(), getAddress(), getPort());
			getDatagramSocket().send(dp);
			result = true;
		} catch (IOException e) {
			e.printStackTrace();
			result = false;
		}
		
		return result;
	}
	public synchronized boolean send(@NotNull InetAddress address, int port, @NotNull String data) {
		setAddress(address);
		setPort(port);
		return send(data);
	}
	public synchronized boolean send(@NotNull InetAddress address, @NotNull String data) {
		setAddress(address);
		return send(data);
	}
	public synchronized boolean send(int port, @NotNull String data) {
		setPort(port);
		return send(data);
	}
	
	@Nullable
	public synchronized Couple<String, DatagramPacket> receive() {
		byte[] data = new byte[MAX_DATA_BYTE_LENGTH];
		
		DatagramPacket dp = null;
		
		try {
			if (getDatagramSocket() == null)
				setDatagramSocket(new DatagramSocket(getPort()));
			dp = new DatagramPacket(data, data.length);
			getDatagramSocket().receive(dp);
			setLastReceivedPacket(dp);
			data = dp.getData();
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
		
		// Convert "data" to string
		String message = new String(data, StandardCharsets.UTF_8);
		message = message.replaceAll(new String(new char[] {'\0'}), "");
		
		return new Couple<>(message, dp);
	}
	public synchronized Couple<String, DatagramPacket> receive(@NotNull InetAddress address, int port) {
		setAddress(address);
		setPort(port);
		return receive();
	}
	public synchronized Couple<String, DatagramPacket> receive(@NotNull InetAddress address) {
		setAddress(address);
		return receive();
	}
	public synchronized Couple<String, DatagramPacket> receive(int port) {
		setPort(port);
		return receive();
	}
	
	public synchronized boolean answer(@NotNull String data) throws OperationNotSupportedException {
		if (getLastReceivedPacket() == null)
			throw new OperationNotSupportedException("Cannot answer because there is no body to speak to... #alone");
		
		boolean result;
		
		try {
			DatagramSocket ds = new DatagramSocket(/*getLastReceivedPacket().getPort()*/);
			DatagramPacket dp = new DatagramPacket(data.getBytes(), data.length(), getLastReceivedPacket().getAddress(), getLastReceivedPacket().getPort());
			ds.send(dp);
			result = true;
		} catch (IOException e) {
			e.printStackTrace();
			result = false;
		}
		
		return result;
	}
	public synchronized boolean answer(@NotNull DatagramPacket lastReceivedPacket, @NotNull String data) throws OperationNotSupportedException {
		setLastReceivedPacket(lastReceivedPacket);
		return answer(data);
	}
	
	/* GETTERS & SETTERS */
	
	public synchronized DatagramSocket getDatagramSocket() {
		return datagramSocket;
	}
	
	public synchronized void setDatagramSocket(DatagramSocket datagramSocket) {
		this.datagramSocket = datagramSocket;
	}
	
	public synchronized InetAddress getAddress() {
		return address;
	}
	
	public synchronized void setAddress(InetAddress address) {
		this.address = address;
	}
	
	public synchronized int getPort() {
		return port;
	}
	
	public synchronized void setPort(int port) {
		this.port = port;
	}
	
	public synchronized DatagramPacket getLastReceivedPacket() {
		return lastReceivedPacket;
	}
	
	public synchronized void setLastReceivedPacket(DatagramPacket lastReceivedPacket) {
		this.lastReceivedPacket = lastReceivedPacket;
	}
}
