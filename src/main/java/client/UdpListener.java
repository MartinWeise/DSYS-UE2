package client;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;

import client.UdpHandler;

public class UdpListener extends Thread {

	private DatagramSocket datagramSocket;
	private PrintStream userResponseStream;	
	private boolean end;
	private UdpHandler handler;
	private ExecutorService threadPool;

	
	public UdpListener(DatagramSocket datagramSocket, PrintStream userResponseStream, ExecutorService threadPool) {
		
		this.datagramSocket = datagramSocket;
		this.userResponseStream = userResponseStream;
		this.threadPool = threadPool;
		this.end = false;		
	}


	public void run() {

		byte[] b;
		DatagramPacket p;

		try {

			while (!end) {

				b = new byte[1024];
				p = new DatagramPacket(b, b.length);

				// Wait for incoming packets from server
				datagramSocket.receive(p);
				
				//Process each incoming packet in a separate thread
				handler = new UdpHandler(datagramSocket, p, userResponseStream);
				threadPool.submit(handler);
				
			}

		} catch (IOException e) {
			System.err.println("client udp listener: " + e.getMessage());
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
