package chatserver;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;

public class UdpListener extends Thread {

	private DatagramSocket datagramSocket;
	private PrintStream userResponseStream;
	private TcpListener u;
	private ExecutorService threadPool;
	
	private boolean end;
	private UdpHandler handler;

	
	public UdpListener(DatagramSocket datagramSocket, PrintStream userResponseStream, TcpListener u, ExecutorService threadPool) {
		
		this.datagramSocket = datagramSocket;
		this.userResponseStream = userResponseStream;
		this.u = u;
		this.threadPool = threadPool;
		this.end = false;
		
	}


	public void run() {

		byte[] buffer;
		DatagramPacket packet;

		try {

			while (!end) {

				buffer = new byte[1024];
				packet = new DatagramPacket(buffer, buffer.length);

				//Wait for incoming packets from client
				datagramSocket.receive(packet);
				
				//Process each incoming packet in a separate thread
				handler = new UdpHandler(datagramSocket, packet, userResponseStream, buffer, u);
				threadPool.submit(handler);
			}

		} catch (IOException e) {
			System.err.println("udp listener: " + e.getMessage());
		}

	}


	public void close() {

		end = true;
		handler.close();
		
		if (datagramSocket != null) {
			datagramSocket.close();
		}
		if(userResponseStream != null) {
			userResponseStream.close();
		}
		
		threadPool.shutdown();
	}


}
