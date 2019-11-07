/*
 * https://www.w3.org/Protocols/rfc1341/4_Content-Type.html
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
 * https://stackoverflow.com/questions/5278975/http-response-header-content-disposition-for-attachments
 * https://stackoverflow.com/questions/16601428/how-to-set-content-disposition-and-filename-when-using-filesystemresource-to/22243867#22243867
 * https://www.javatpoint.com/java-regex
 * https://www.vogella.com/tutorials/JavaRegularExpressions/article.html
 * https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
 * https://stackoverflow.com/questions/12715321/java-networking-explain-inputstream-and-outputstream-in-socket
 * https://github.com/Mananp96/Curl-like-app/tree/master/TCPClient-Server
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Client class
 * 
 * @author Himen Sidhpura
 * @author Jenny Mistry
 *
 */

/*
https://www.w3.org/Protocols/rfc1341/4_Content-Type.html
https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
https://stackoverflow.com/questions/5278975/http-response-header-content-disposition-for-attachments
https://stackoverflow.com/questions/16601428/how-to-set-content-disposition-and-filename-when-using-filesystemresource-to/22243867#22243867
https://www.javatpoint.com/java-regex
https://www.vogella.com/tutorials/JavaRegularExpressions/article.html
https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
https://stackoverflow.com/questions/12715321/java-networking-explain-inputstream-and-outputstream-in-socket
https://stackoverflow.com/questions/10788125/a-simple-http-server-with-java-socket
https://stackoverflow.com/questions/2717294/create-a-simple-http-server-with-java
https://systembash.com/a-simple-java-tcp-server-and-tcp-client/
https://www.pegaxchange.com/2017/12/07/simple-tcp-ip-server-client-java/
https://www.geeksforgeeks.org/synchronized-in-java/
https://github.com/Mananp96/Curl-like-app/tree/master/TCPClient-Server
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
						//System.out.println("--->" + getBody());
					}
				}
			}

			try {
				uri = new URI(getUrl());
				socket = new Socket(uri.getHost(), uri.getPort());
				setQuery(uri.getPath().substring(1).trim());
				System.out.println("Server Connection Established for " + getQuery());
				sendRequest(getQuery());
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
	 */
	public static void receiveData(String query) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String out = "";
			boolean tempFlag = false;
			ArrayList<String> fileList = new ArrayList<>();
			ArrayList<String> directoryList = new ArrayList<>();
			while ((out = br.readLine()) != null) {
				if (!query.equals(Constants.GET_METHOD + "/")) {
					System.out.println(out);
				}

				else {
					tempFlag = true;
					String[] temp = out.split(">>");
					if (temp[0].trim().equals("Directory")) {
						directoryList.add(temp[1].trim());
					}
					if (temp[0].trim().equals("File")) {
						fileList.add(temp[1].trim());
					}
				}
			}

			if (tempFlag) {
				System.out.println("------------");
				System.out.println("DIRECTORIES: ");
				System.out.println("------------");
				for (String str : directoryList) {
					System.out.println(str);
				}

				System.out.println("-------");
				System.out.println("FILES: ");
				System.out.println("-------");
				for (String str : fileList) {
					System.out.println(str);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * this method is used to form the client request using the data sent to the
	 * server.
	 */
	public static void sendRequest(String query) {
		try {
			PrintWriter writer = new PrintWriter(socket.getOutputStream());
			writer.println(getQuery());

			if (isHeaderFlag())
				for (int i = 0; i < headerList.size(); i++) {
					writer.println(headerList.get(i));
				}

			if (isBodyFlag())
				writer.println("-d" + getBody());

			writer.println("\r\n");
			writer.flush();
			receiveData(query);
			writer.close();
			socket.close();
		} catch (IOException io) {
			io.printStackTrace();
		}

	}

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

	int port;

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