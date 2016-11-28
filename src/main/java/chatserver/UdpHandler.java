package chatserver;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

public class UdpHandler extends Thread {

	private DatagramSocket datagramSocket;
	private DatagramPacket packet;
	private PrintStream userResponseStream;
	private TcpListener u;
	private byte[] buffer;


	public UdpHandler(DatagramSocket datagramSocket, DatagramPacket packet, PrintStream userResponseStream, byte[] buffer, TcpListener u) {

		this.datagramSocket = datagramSocket;
		this.packet = packet;
		this.userResponseStream = userResponseStream;
		this.buffer = buffer;
		this.u = u;
	}
	
	
	public void run() {
		
		String request = new String(packet.getData(), packet.getOffset(), packet.getLength());
		userResponseStream.println("Received packet from client: " + request);

		String[] parts = request.split("\\s");
		String response = "Command not available!";


		if (request.startsWith("!list")) {
			if (parts.length > 1) {
				response = "Command doesn't require any arguments: !list";

			} else {
				response = "Online users:\n";
				Map<String, UserData> m = u.getUsers();
				int i = 0;
				for(Map.Entry<String, UserData> e : m.entrySet()){	
					if(e.getValue().getOnline()) {
						response += e.getKey() + "\n";
						i++;
					}
				}
				if(i == 0) {
					response = "No users online.";
				}

			}
		}

		InetAddress address = packet.getAddress();
		int port = packet.getPort();
		buffer = response.getBytes();

		packet = new DatagramPacket(buffer, buffer.length, address, port);

		try {
			datagramSocket.send(packet);
		} catch (IOException e) {
			System.err.println("udp handler: " + e.getMessage());
		}
		
	}
	
	
	public void close() {
		if (datagramSocket != null) {
			datagramSocket.close();
		}
		if(userResponseStream != null) {
			userResponseStream.close();
		}
		
	}
	

}
