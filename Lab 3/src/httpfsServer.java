import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * /** Server class.
 * 
 * @author Himen Sidhpura
 * @auhtor Jenny Mistry
 *
 */

/*
 * https://www.w3.org/Protocols/rfc1341/4_Content-Type.html
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
 * https://stackoverflow.com/questions/5278975/http-response-header-content-
 * disposition-for-attachments
 * https://stackoverflow.com/questions/16601428/how-to-set-content-disposition-
 * and-filename-when-using-filesystemresource-to/22243867#22243867
 * https://www.javatpoint.com/java-regex
 * https://www.vogella.com/tutorials/JavaRegularExpressions/article.html
 * https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.
 * html https://stackoverflow.com/questions/12715321/java-networking-explain-
 * inputstream-and-outputstream-in-socket
 * https://stackoverflow.com/questions/10788125/a-simple-http-server-with-java-
 * socket
 * https://stackoverflow.com/questions/2717294/create-a-simple-http-server-with-
 * java https://systembash.com/a-simple-java-tcp-server-and-tcp-client/
 * https://www.pegaxchange.com/2017/12/07/simple-tcp-ip-server-client-java/
 * https://www.geeksforgeeks.org/synchronized-in-java/
 * https://github.com/Mananp96/Curl-like-app/tree/master/TCPClient-Server
 */

public class httpfsServer {

	static boolean isDebugging;

	static boolean isPort;

	static int port;

	Packet packet;
	
	static String directoryPath = "";
	
	private static final Logger logger = LoggerFactory.getLogger(httpfsServer.class);

	/**
	 * getter method for getting Directory Path
	 * 
	 * @return pathDirectory
	 */
	public static String getPathDirectory() {
		return directoryPath;
	}

	/**
	 * getter method for getting Port Number
	 * 
	 * @return port
	 */
	public static int getPort() {
		return port;
	}

	/**
	 * method for checking if debugging mode is on or not
	 * 
	 * @return isDebugging
	 */
	public static boolean isDebugging() {
		return isDebugging;
	}

	/**
	 * method to check if port is active or not
	 * 
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
	public static void main(String[] args) throws IOException {
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

//		
//		boolean flag = true;
//		while (true) {
//			counter++;
//			Socket client = socket.accept();
//			if (isDebugging())
//				System.out.println(">>Connection Established with Client " + counter);
//			httpfsServerThread serverThread = new httpfsServerThread(client, getPathDirectory(), counter);
//			Thread thread = new Thread(serverThread);
//			thread.start();
//			if (!flag)
//				break;
//		}
//		socket.close();		

		DatagramChannel channel = DatagramChannel.open();
		channel.bind(new InetSocketAddress(port));
		
//		ServerSocket socket = new ServerSocket(getPort());
//		int counter = 0;
		System.out.println("Server Started");
		if (isDebugging()) {
//			System.out.println("Server Listening  At " + getPort() + " Port");
			logger.info("EchoServer is listening at {}", channel.getLocalAddress());
//			System.out.println("EchoServer is listening at {}" + channel.getLocalAddress());

		}
		ByteBuffer buf = ByteBuffer.allocate(Constants.MAX_LEN).order(ByteOrder.BIG_ENDIAN);
		
		
		httpfsServer server = new httpfsServer();
		server.listenAndServe(channel,buf);		
	}

	public void listenAndServe(DatagramChannel channel, ByteBuffer buf) throws IOException  {
		int counter = 0;
		while (true) {
			buf.clear();
			SocketAddress router = channel.receive(buf);
			System.out.println();

			// Parse a pacsoutket from the received raw data.
			buf.flip();
			packet = Packet.fromBuffer(buf);
			buf.flip();
			String payload = new String(packet.getPayload(), UTF_8);
			logger.info("Packet: {}", packet);
//			System.out.println("Packet: {}" + packet);
			logger.info("Payload: {}", payload);
//			System.out.println("Payload: {}" + payload);
			logger.info("Router: {}", router);
//			System.out.println("Router: {}" + router);

			// Send the response to the router not the client.
			// The peer address of the packet is the address of the client already.
			// We can use toBuilder to copy properties of the current packet.
			// This demonstrate how to create a new packet from an existing packet.
			validateRequest(buf,payload,router,channel,counter);
		}
	}

	

	private void validateRequest(ByteBuffer buf, String payload, SocketAddress router, DatagramChannel channel, int counter) throws IOException {
		long packetNumber = packet.getSequenceNumber();
		String ACK = "send packet from " + (++packetNumber);
		if (packet.getType() == Constants.CONNECTION_TYPE && !payload.equals(Constants.ACK_CODE)) {
			Packet resp = packet.toBuilder().setPayload(ACK.getBytes()).create();
			channel.send(resp.toBuffer(), router);
			System.out.println(">> Client with " + packet.getPeerPort() + " connection established");
			System.out.println(packet.getType());
			System.out.println("Payload :" +payload);

		} else if (packet.getType() == Constants.DATA_TYPE && !payload.equals(Constants.ACK_CODE)) {

			httpfsServerThread serverThread = new httpfsServerThread(channel, packet, counter, directoryPath, buf, router);
			Thread t = new Thread(serverThread);
			t.start();

		} else if (packet.getType() == Constants.DATA_TYPE && payload.equals(Constants.ACK_CODE)) {
			System.out.println("DONE");
		}
		buf.clear();
	}

	/**
	 * setter method to set the debugging mode
	 * 
	 * @param isDebugging
	 */
	public static void setDebugging(boolean isDebugging) {
		httpfsServer.isDebugging = isDebugging;
	}

	/**
	 * setter method for setting Directory Path
	 * 
	 * @param pathDirectory path for Directory
	 */
	public static void setPathDirectory(String pathDirectory) {
		httpfsServer.directoryPath = pathDirectory;
	}

	/**
	 * setter method for setting Mode of port
	 * 
	 * @param isPort
	 */
	public static void setPort(boolean isPort) {
		httpfsServer.isPort = isPort;
	}

	/**
	 * setter method for setting Port Number
	 * 
	 * @param port
	 */
	public static void setPort(int port) {
		httpfsServer.port = port;
	}
}
