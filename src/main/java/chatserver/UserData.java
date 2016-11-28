package chatserver;

public class UserData {

	private String userName;
	private String userPW;

	private boolean online;
	private String lastReceivedMessage;
	private String senderName;
	private String privateAdress;


	public UserData(String userName, String userPW) {
		this.userName = userName;
		this.userPW = userPW;

		this.online = false;
		this.lastReceivedMessage = "";
		this.privateAdress = "";
	}

	public String getUserName() {
		return this.userName;
	}

	public String getUserPW() {
		return this.userPW;
	}

	public synchronized boolean getOnline() {
		return this.online;
	}

	public synchronized void setOnlineStatus(boolean online) {
		this.online = online;
	}

	public synchronized String getLastReceivedMessage() {
		return this.lastReceivedMessage;
	}

	public synchronized void setLastReceivedMessage(String message) {
		this.lastReceivedMessage = message;
	}

	public synchronized String getSenderName() {
		return this.senderName;
	}

	public synchronized void setSenderName(String name) {
		this.senderName = name;
	}

	public synchronized String getPrivateAdress() {
		return this.privateAdress;
	}

	public synchronized void setPrivateAdress(String adress) {
		this.privateAdress = adress;
	}

	public String getIP() {
		String[] parts = privateAdress.split(":");
		return parts[0];
	}

	public int getPort() {
		String[] parts = privateAdress.split(":");
		return Integer.parseInt(parts[1]);
	}

}


