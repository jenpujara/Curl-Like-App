import static java.nio.channels.SelectionKey.OP_READ;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client class
 * 
 * @author Himen Sidhpura
 * @author Jenny Mistry
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
 * https://www.tutorialspoint.com/java_nio/java_nio_datagram_channel.htm
 * http://tutorials.jenkov.com/java-nio/datagram-channel.html
 * https://docs.oracle.com/javase/7/docs/api/java/net/InetAddress.html
 * https://github.com/Mananp96/Curl-like-app/tree/master/TCPClient-Server
 */

public class httpfsClient {

	static String body;
	static boolean bodyFlag;
	static boolean headerFlag;
	static ArrayList<String> headerList;
	static String query;
	static Socket socket;
	static URI uri;
	static String url;
	
	static DatagramChannel channel;
	private PrintWriter out;
	int port;
	static SocketAddress routerAddr = new InetSocketAddress("localhost", 3000);
	static int seqNum = 101;
	private static final Logger logger = LoggerFactory.getLogger(httpfsClient.class);
	static InetSocketAddress serverAddr;
	static String payload;
	private StringBuilder clientRequest;
	
	static Packet packet;
	static Map<Integer, String> packetDetails;
	
	public static SocketAddress getRouterAddr() {
		return routerAddr;
	}

	public static void setRouterAddr(SocketAddress routerAddr) {
		httpfsClient.routerAddr = routerAddr;
	}

	public static int getSeqNum() {
		return seqNum;
	}

	public static void setSeqNum(int seqNum) {
		httpfsClient.seqNum = seqNum;
	}

	public static InetSocketAddress getServerAddr() {
		return serverAddr;
	}

	public static void setServerAddr(InetSocketAddress serverAddr) {
		httpfsClient.serverAddr = serverAddr;
	}

	public static String getPayload() {
		return payload;
	}

	public static void setPayload(String payload) {
		httpfsClient.payload = payload;
	}

	public StringBuilder getClientRequest() {
		return clientRequest;
	}

	public void setClientRequest(StringBuilder clientRequest) {
		this.clientRequest = clientRequest;
	}

	public String getPacketDetails(int packetNumber) {
		return packetDetails.get(packetNumber);
	}

	public void setPacketDetails(Map<Integer, String> packetDetails) {
		this.packetDetails = packetDetails;
	}

	public static void setPacketACK(long sequenceNumber) {
		packetDetails.put((int) sequenceNumber, Constants.ACK_CODE);
	}

	public httpfsClient(InetSocketAddress serverAddr2, String query2,String content2, ArrayList<String> headerList) throws IOException {

		setServerAddr(serverAddr2);
		setBody(content2);
		setQuery(query2);
		this.headerList = headerList;
		channel = DatagramChannel.open();
		setPacketDetails(new HashMap<Integer, String>());
		handshake();
	}	

	/**
	 * used to do initial three-way handshake to server.
	 * @throws IOException
	 */
	public void handshake() throws IOException {
		sendToChannel("handshake",Constants.CONNECTION_TYPE);
		logger.info("Sending \"{}\" to router at {}", "handshake message", getRouterAddr());
		receive(channel,getRouterAddr());

		if(getPacketDetails(getSeqNum()).equals(Constants.ACK_CODE)) {
			setSeqNum(getSeqNum()+1); 
			sendToChannel(Constants.ACK_CODE,Constants.CONNECTION_TYPE);
			logger.info("Sending \"{}\" to router at {}", "Connection done", getRouterAddr());
			setSeqNum(getSeqNum()+1);
			sendRequest(getQuery());
		}else {

			logger.info("Sending \"{}\" again to router at {}", "handshake message", getRouterAddr());
			handshake();
		}
	}
	
	private void sendToChannel(String process, int packetType) throws IOException {
		packet = createPacket(process,packetType);
		channel.send(packet.toBuffer(), getRouterAddr());
		packetDetails.put(getSeqNum(), "");
	}

	private Packet createPacket(String process, int packetType) {
		return new Packet.Builder()
		.setType(packetType)
		.setSequenceNumber(getSeqNum())
		.setPortNumber(getServerAddr().getPort())
		.setPeerAddress(getServerAddr().getAddress())
		.setPayload(process.getBytes())
		.create();		
	}

	/**
	 * receive packet from server.
	 * @param channel DatagramChannel
	 * @param routerAddr Router Address
	 * @throws IOException IOException 
	 */
	public static void receive(DatagramChannel channel,SocketAddress routerAddr) throws IOException{
		// Try to receive a packet within timeout.
		channel.configureBlocking(false);
		Selector selector = Selector.open();
		channel.register(selector, OP_READ);
		logger.info("Waiting for the response");
		selector.select(10000);

		Set<SelectionKey> keys = selector.selectedKeys();
		if(keys.isEmpty()){
			logger.error("No response after timeout");
			return;
		}
		displayPayload(channel,keys);
	}  

	private static void displayPayload(DatagramChannel channel,Set<SelectionKey> keys) throws IOException {
		while(true) {
			ByteBuffer buf = ByteBuffer.allocate(Constants.MAX_LEN);
			SocketAddress router = channel.receive(buf);
			buf.flip();
			if(buf.limit() == 0)
				break;
			Packet response = Packet.fromBuffer(buf);
			logger.info("Packet: {}", response);
			logger.info("Router: {}", router);
			setPayload(new String(response.getPayload(), StandardCharsets.UTF_8));
			logger.info("Payload: {}", getPayload());
			setPacketACK(response.getSequenceNumber());
			keys.clear();
		}
	}
	

	/**
	 * Send data to server
	 * @throws IOException IOException
	 */
	public void sendRequest(String query) throws IOException {
		setClientRequest(new StringBuilder());
		setClientRequest(getClientRequest().append(getQuery()+"\n"));
		if(headerFlag) {
			for(int i = 0 ; i<headerList.size();i++) {
				setClientRequest(getClientRequest().append(headerList.get(i)+"\n"));
			}
		}
		if(bodyFlag) {
			setClientRequest(getClientRequest().append(Constants.PATH_DIRECTORY + getBody() + "\n"));
		}
		setClientRequest(getClientRequest().append("\r\n"));
		sendToChannel(getClientRequest().toString().trim(), Constants.DATA_TYPE);
		logger.info("Sending \"{}\" to router at {}", getClientRequest(), getRouterAddr());
		receive(channel, getRouterAddr());
		if(getPacketDetails(getSeqNum()).equals(Constants.ACK_CODE)) {
			setSeqNum(getSeqNum()+1);
			sendToChannel(Constants.ACK_CODE, Constants.DATA_TYPE);
		}else {
			this.sendRequest(getQuery());
		}

	}


	/**
	 * getter method for fetching the body of the request.
	 * 
	 * @return body
	 */
	public static String getBody() {
		return body;
	}

	/**
	 * getter method for query retrieval
	 * 
	 * @return query
	 */
	public static String getQuery() {
		return query;
	}

	/**
	 * getter method for getting the url from the request
	 * 
	 * @return url
	 */
	public static String getUrl() {
		return url;
	}

	/**
	 * method to check if the content is present or not.
	 * 
	 * @return bodyFlag
	 */
	public static boolean isBodyFlag() {
		return bodyFlag;
	}

	/**
	 * method to check if the header is present or not.
	 * 
	 * @return headerFlag
	 */
	public static boolean isHeaderFlag() {
		return headerFlag;
	}

	/**
	 * Main method implementing the HTTP File Manager Client.
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	public static void main(String args[]) {
		InputStreamReader inputReader = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(inputReader);
		String input;
		headerList = new ArrayList<>();
		setHeaderFlag(false);
		setBodyFlag(false);
		try {
			input = br.readLine();
			String[] splitInput = input.split(" ");
			if (splitInput[0].equals("httpfs")) {
				for (int i = 0; i < splitInput.length; i++) {
					if (splitInput[i].equals(Constants.HEADER_CODE)) {
						setHeaderFlag(true);
						headerList.add(splitInput[++i]);
					}
					if (splitInput[i].startsWith("http://") || splitInput[i].startsWith("https://")) {
						setUrl(splitInput[i]);

					}
					if (splitInput[i].startsWith(Constants.INLINE_DATA_CODE1)
							|| splitInput[i].startsWith(Constants.INLINE_DATA_CODE2)) {
						setBodyFlag(true);
						String temp = input.substring(input.indexOf(Constants.INLINE_DATA_CODE2)).trim();
						String temp1 = temp.substring(1);
						setBody(temp1.substring(temp1.indexOf("\"")));
					}
				}
			}

			try {
				URI uri = new URI(url);
				String host = uri.getHost();
				int port = uri.getPort();
				setQuery(uri.getPath().substring(1));
				InetSocketAddress serverAddress = new InetSocketAddress(host, port);
				httpfsClient httpfsclient = new httpfsClient(serverAddress,getQuery(),getBody(), headerList);
				System.out.println(packetDetails);
			} catch (IOException e) {
				System.out.println(Constants.HTTP_404_ERROR + " : Host Not Found");
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * method to display the data received from client request.
	 * @param payload 
	 */

	/**
	 * setter method to set the body of the request
	 * 
	 * @param body
	 */
	public static void setBody(String body) {
		httpfsClient.body = body;
	}

	/**
	 * setter method to set the body flag to true or false
	 * 
	 * @param bodyFlag
	 */
	public static void setBodyFlag(boolean bodyFlag) {
		httpfsClient.bodyFlag = bodyFlag;
	}

	/**
	 * setter method to set the header flag to true or false
	 * 
	 * @param headerFlag
	 */
	public static void setHeaderFlag(boolean headerFlag) {
		httpfsClient.headerFlag = headerFlag;
	}

	/**
	 * setter method to set the query string in the request.
	 * 
	 * @param query
	 */
	public static void setQuery(String query) {
		httpfsClient.query = query;
	}

	/**
	 * setter method to set the url of the client request.
	 * 
	 * @param url
	 */
	public static void setUrl(String url) {
		httpfsClient.url = url;
	}


	/**
	 * getter method to retrieve the port number on which the server is listening
	 * 
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * setter method to set the port number on which the server listens
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

}