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
public class httpfsClient {

	static String body;
	static boolean bodyFlag;
	static boolean headerFlag;
	static ArrayList<String> headerList;
	static String query;
	static Socket socket;
	static URI uri;
	static String url;

	public static String getBody() {
		return body;
	}

	public static String getQuery() {
		return query;
	}

	public static String getUrl() {
		return url;
	}

	public static boolean isBodyFlag() {
		return bodyFlag;
	}

	public static boolean isHeaderFlag() {
		return headerFlag;
	}

	public static void main(String args[]) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
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
						setBody(splitInput[++i]);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			System.out.println(" ---> " + getUrl());
			uri = new URI(getUrl());
			System.out.println(uri.getHost());
			socket = new Socket(uri.getHost(), uri.getPort());
			System.out.println(getQuery());
			System.out.println(uri.getPath().substring(1).trim());
			setQuery(uri.getPath().substring(1));
			System.out.println("Server Connection Establlished");
			sendRequest();
		} catch (IOException e) {
			System.out.println(Constants.HTTP_404_ERROR + " : Host Not Found");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

	public static void receiveData() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String out = "";
			while ((out = br.readLine()) != null) {
				System.out.println(out);
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

	public static void sendRequest() {
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
			receiveData();
			writer.close();
			socket.close();
		} catch (IOException io) {
			io.printStackTrace();
		}

	}

	public static void setBody(String body) {
		httpfsClient.body = body;
	}

	public static void setBodyFlag(boolean bodyFlag) {
		httpfsClient.bodyFlag = bodyFlag;
	}

	public static void setHeaderFlag(boolean headerFlag) {
		httpfsClient.headerFlag = headerFlag;
	}

	public static void setQuery(String query) {
		httpfsClient.query = query;
	}

	public static void setUrl(String url) {
		httpfsClient.url = url;
	}

	int port;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}