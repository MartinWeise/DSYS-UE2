package client;

import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class UdpHandler extends Thread {

	private DatagramSocket datagramSocket;
	private DatagramPacket p;
	private PrintStream userResponseStream;


	public UdpHandler(DatagramSocket datagramSocket, DatagramPacket p, PrintStream userResponseStream) {

		this.datagramSocket = datagramSocket;
		this.p = p;
		this.userResponseStream = userResponseStream;
	}


	public void run() {

		String data = new String(p.getData(), p.getOffset(), p.getLength());
		userResponseStream.println(data);

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
