import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 /**
 * Server class.
 * @author Himen Sidhpura
 * @auhtor Jenny Mistry 
 *
 */
public class httpfsServer {

	static boolean isDebugging;

	static boolean isPort;

	static String pathDirectory = "";

	static int port;

	/**
	 * getter method for getting Directory Path
	 * @return pathDirectory
	 */
	public static String getPathDirectory() {
		return pathDirectory;
	}
	
	/**
	 * getter method for getting Port Number
	 * @return port
	 */
	public static int getPort() {
		return port;
	}
	
	/**
	 * method for checking if debugging mode is on or not
	 * @return isDebugging
	 */
	public static boolean isDebugging() {
		return isDebugging;
	}

	/**
	 * method to check if port is active or not
	 * @return isPort
	 */
	public static boolean isPort() {
		return isPort;
	}

	/**
	 * main method used to create a Server with port Number 8080.
	 * 
	 * @param args args.
	 * @throws IOException Input-Output Exception.
	 */	
	public static void main(String args[]) throws IOException {
		setDebugging(false);
		setPort(8080);
		setPort(false);
		InputStreamReader inReader = new InputStreamReader(System.in); 
		BufferedReader br = new BufferedReader(inReader);
		String[] splitStrings = br.readLine().trim().split(" ");

		for (int i = 0; i < splitStrings.length; i++) {
			if (splitStrings[i].equals(Constants.PORT_CODE)) {
				setPort(true);
				setPort(Integer.parseInt(splitStrings[++i]));
			}
			if (splitStrings[i].equals(Constants.PATH_DIRECTORY)) {
				setPathDirectory(splitStrings[++i]);
			} else {
				setPathDirectory(Constants.PATH_TO_DIRECTORY);
			}
			if (splitStrings[i].equals(Constants.DEBUGGING)) {
				setDebugging(true);
			}
		}

		ServerSocket socket = new ServerSocket(getPort());
		int counter = 0;
		System.out.println("Server Started");
		if (isDebugging()) {
			System.out.println("Server Listening  At " + getPort() + " Port");
		}
		while (true) {
			counter++;
			Socket client = socket.accept();
			if (isDebugging())
				System.out.println(">>Connection Established with Client " + counter);
			System.out.println("Path in HTTFSServer  : " + getPathDirectory());
			httpfsServerThread serverThread = new httpfsServerThread(client, getPathDirectory(), counter);
			Thread thread = new Thread(serverThread);
			thread.start();
		}
	}
	
	/**
	 * setter method to set the debugging mode
	 * @param isDebugging
	 */
	public static void setDebugging(boolean isDebugging) {
		httpfsServer.isDebugging = isDebugging;
	}

	/**
	 * setter method for setting Directory Path
	 * @param pathDirectory path for Directory
	 */
	public static void setPathDirectory(String pathDirectory) {
		httpfsServer.pathDirectory = pathDirectory;
	}

	/**
	 * setter method for setting Mode of port
	 * @param isPort
	 */
	public static void setPort(boolean isPort) {
		httpfsServer.isPort = isPort;
	}
	
	/**
	 * setter method for setting Port Number
	 * @param port
	 */
	public static void setPort(int port) {
		httpfsServer.port = port;
	}
}
