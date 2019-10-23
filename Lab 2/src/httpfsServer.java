import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server class.
 * @author 
 *
 */

/**
 * Server class.
 *
 */
public class httpfsServer {

	static boolean isPort;

	public static boolean isPort() {
		return isPort;
	}

	public static void setPort(boolean isPort) {
		httpfsServer.isPort = isPort;
	}

	public static int getPort() {
		return port;
	}

	public static void setPort(int port) {
		httpfsServer.port = port;
	}

	public static String getPathDirectory() {
		return pathDirectory;
	}

	public static void setPathDirectory(String pathDirectory) {
		httpfsServer.pathDirectory = pathDirectory;
	}

	public static boolean isDebugging() {
		return isDebugging;
	}

	public static void setDebugging(boolean isDebugging) {
		httpfsServer.isDebugging = isDebugging;
	}

	static int port;
	static String pathDirectory = "";
	static boolean isDebugging;

	/**
	 * main method used to create a Server with port Number 5555.
	 * 
	 * @param args args.
	 * @throws IOException Input-Output Exception.
	 */
	public static void main(String args[]) throws IOException {
		setDebugging(false);
		setPort(8080);
		setPort(false);
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input = br.readLine().trim();
		String[] splitStrings = input.split(" ");

		for (int i = 0; i < splitStrings.length; i++) {
			if (splitStrings[i].equals(Constants.PORT_CODE)) {
				setPort(true);
				setPort(Integer.parseInt(splitStrings[++i]));
			}
			if (splitStrings[i].equals(Constants.PATH_DIRECTORY)) {
				setPathDirectory(splitStrings[++i]);
			} else {
				setPathDirectory("C:/Users/Himen/Desktop");
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
}
