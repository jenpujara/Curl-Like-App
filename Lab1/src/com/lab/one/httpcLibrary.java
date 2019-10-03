package com.lab.one;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/*
 * http://www.java2s.com/Tutorials/Java/Socket/How_to_use_Java_Socket_class_to_create_a_HTTP_client.htm
 * http://www.java2s.com/Tutorial/Java/0320__Network/SendingaPOSTRequestUsingaSocket.htm
 * https://examples.javacodegeeks.com/core-java/net/socket/send-http-post-request-with-socket/
 * https://www.dreamincode.net/forums/topic/189336-socket-post-request/
 * https://www.geeksforgeeks.org/java-net-uri-class-java/
 * https://www.geeksforgeeks.org/uri-getrawquery-method-in-java-with-examples/
 * https://www.geeksforgeeks.org/uri-getrawpath-method-in-java-with-examples/
 * https://www.geeksforgeeks.org/uri-getpath-method-in-java-with-examples/
 * https://www.geeksforgeeks.org/uri-getrawuserinfo-method-in-java-with-examples/
 * https://www.geeksforgeeks.org/uri-getauthority-method-in-java-with-examples/
 * https://stackoverflow.com/questions/3487389/convert-string-to-uri
 */
public class httpcLibrary {

	ArrayList<String> headerList = new ArrayList<>();
	Socket socket;

	String inLineData;
	String readFile;
	String generateFile;
	String input;
	String url;
	String hostName;
	String protocolName;
	String query;
	String urlPath;

	String fileName;
	String referenceName;
	String newURL;

	int statusCode;
	int portNumber;

	boolean verboseFlag = false;
	boolean headerFlag = false;
	boolean inLineDataFlag = false;
	boolean readFileFlag = false;
	boolean generateFileFlag = false;
	boolean isRedirect = false;

	public static final int HTTP_OK = 200;
	public static final int HTTP_MULT_CHOICE = 300;
	public static final int HTTP_MOVED_PERM = 301;
	public static final int HTTP_MOVED_TEMP = 302;
	public static final int HTTP_SEE_OTHER = 303;
	public static final int HTTP_NOT_MODIFIED = 304;
	public static final int HTTP_USE_PROXY = 305;
	public static final String HTTPC = "httpc";
	public static final String GET_REDIRECT = "getRedirect";
	public static final String POST_REDIRECT = "postRedirect";
	public static final String HTTP = "http";
	public static final String HTTPS = "https";
	public static final String GET = "get";
	public static final String POST = "post";
	public static final String VERBOSE_CODE = "-v";
	public static final String HEADER_CODE = "-h";
	public static final String INLINE_DATA_CODE1 = "--d";
	public static final String INLINE_DATA_CODE2 = "-d";
	public static final String READFILE_CODE = "-f";
	public static final String CREATE_FILE_CODE = "-o";
	public static final String HELP = "help";
	public static final int HTTP_PORT = 80;
	public static final int HTTPS_PORT = 443;

	public httpcLibrary(String input1) {
		input = input1;
	}

	public void parseInput() throws IOException {
		System.out.println("Input Command : " + input);
		String[] splitInput = input.trim().split(" ");
		ArrayList<String> inputList = new ArrayList<>();
		for (int i = 0; i < splitInput.length; i++) {
			if (splitInput[i].trim().length() != 0) {
				inputList.add(splitInput[i]);
			}
		}
		if (inputList.get(0).equals(HTTPC) && (inputList.get(1).equals(HELP))) {
			printHelp(inputList.get(2));
		} else if (inputList.get(0).equals(HTTPC) && (inputList.get(1).equals(GET) || inputList.get(1).equals(POST))) {
			for (int i = 0; i < inputList.size(); i++) {
				switch (inputList.get(i)) {
				case VERBOSE_CODE:
					verboseFlag = true;
					break;

				case HEADER_CODE:
					headerFlag = true;
					headerList.add(inputList.get(++i));
					break;

				case INLINE_DATA_CODE1:
					inLineDataFlag = true;
					inLineData = inputList.get(++i) + inputList.get(++i);
					break;

				case INLINE_DATA_CODE2:
					inLineDataFlag = true;
					inLineData = inputList.get(++i) + inputList.get(++i);
					break;

				case READFILE_CODE:
					readFileFlag = true;
					readFile = inputList.get(++i);
					break;

				case CREATE_FILE_CODE:
					generateFileFlag = true;
					generateFile = inputList.get(++i);
					break;

				default:
					break;
				}
				// if (inputList.get(i).equals("-v")) {
				// verboseFlag = true;
				// }
				// if (inputList.get(i).equals("-h")) {
				// headerFlag = true;
				// headerList.add(inputList.get(++i));
				// }
				// if (inputList.get(i).equals("--d") || inputList.get(i).equals("-d")) {
				// inLineDataFlag = true;
				// inLineData = inputList.get(++i);
				// }
				// if (inputList.get(i).equals("-f")) {
				// readFileFlag = true;
				// readFile = inputList.get(++i);
				// }
				// if (inputList.get(i).equals("-o")) {
				// generateFileFlag = true;
				// generateFile = inputList.get(++i);
				// }
				if (inputList.get(i).startsWith("http://") || inputList.get(i).startsWith("https://")) {
					setUrl(inputList.get(i));
				}

			}
			if (getUrl() != null) {
				getUrlData();
				if (!(readFileFlag && inLineDataFlag)) {
					if (inputList.get(1).equals(POST)) {
						postRequest();
					} else if (inputList.get(1).equals(GET)) {
						getRequest();
					} else {
						System.out.println("No Post and Get Found in Input");
					}
				} else {
					System.out.println("Invalid Command: -f and -d both are not allowed.");
				}
			} else {
				System.out.println("Invalid URL");
			}
		} else {
			System.out.println("Invalid Command");
		}

	}

	public static void printHelp(String option) {
		if (option.equals(POST)) {
			System.out.println(
					"usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\nPost executes a HTTP POST request for a given URL with inline data or from file.\n -v Prints the detail of the response such as protocol, status, and headers.\n -h key:value Associates headers to HTTP Request with the format 'key:value'.\n -d string Associates an inline data to the body HTTP POST request. \n -f file Associates the content of a file to the body HTTP POST request.\nEither [-d] or [-f] can be used but not both.");

		} else if (option.equals(GET)) {
			System.out.println("usage: httpc get [-v] [-h key:value] URL\r\n"
					+ "Get executes a HTTP GET request for a given URL.\r\n"
					+ " -v Prints the detail of the response such as protocol, status,\r\n" + "and headers.\r\n"
					+ " -h key:value Associates headers to HTTP Request with the format\r\n" + "'key:value'.");
		}
	}

	private void getUrlData() {
		try {
			URI urL = new URI(getUrl());
			setHostName(urL.getHost());
			protocolName = urL.getScheme();
			setPortNumber(urL.getPort());
			query = urL.getRawQuery();
			setUrlPath(urL.getRawPath());
			if (getHostName() == null || getHostName().length() == 0) {
				setHostName("");
			}
			if (getPortNumber() == -1) {
				if (protocolName.equals(HTTP)) {
					setPortNumber(HTTP_PORT);
				} else if (protocolName.equals(HTTPS)) {
					setPortNumber(HTTPS_PORT);
				}
			}
			if (query == null || query.length() == 0) {
				query = "";
			}
			if (query.length() > 0 || getUrlPath().length() > 0) {
				setUrlPath(getUrlPath() + "?" + query);
			}
			if (fileName == null || fileName.length() == 0) {
				fileName = "";
			}
			if (referenceName == null || referenceName.length() == 0) {
				referenceName = "";
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public static String generateMethodURL(String method, String tempURL, String type) {
		if (method.equals("POST")) {
			if (tempURL.length() != 0) {
				return "POST " + tempURL + " HTTP/1.1\r\n";
				// writer.write("POST " + urlPath + " HTTP/1.1\r\n");
			} else {
				return "POST / HTTP/1.1\r\n";
				// writer.write("POST / HTTP/1.1\r\n");
			}
		} else if (method.equals("GET")) {
			if (tempURL.length() == 0) {
				// writer.println("GET / /1.1");
				return "GET / /1.1";
			} else {
				return "GET " + tempURL + " HTTP/1.1";
				// writer.println("GET " + urlPath + " HTTP/1.1");
			}
		}
		return "";
	}

	public void postRequest() throws IOException {

		socket = new Socket(getHostName(), getPortNumber());
		StringBuilder data = new StringBuilder();
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
		if (urlPath.length() != 0) {
			writer.write(generateMethodURL("POST", getUrlPath(), " HTTP/1.1\r\n"));
			/// writer.write("POST " + urlPath + " HTTP/1.1\r\n");
		} else {
			writer.write(generateMethodURL("POST", "", " HTTP/1.1\r\n"));
			// writer.write("POST / HTTP/1.1\r\n");
		}
		writer.write("Host:" + getHostName() + "\r\n");

		if (headerFlag && !headerList.isEmpty()) {
			for (int i = 0; i < headerList.size(); i++) {
				String[] headerKeyValue = headerList.get(i).split(":");
				writer.write(headerKeyValue[0] + ":" + headerKeyValue[1] + "\r\n");
			}
		}
		if (inLineDataFlag) {
			data.append(inLineData);
			// writer.write("Content-Length:" + inLineData.length() + "\r\n");
		} else if (readFileFlag) {
			BufferedReader reader = new BufferedReader(new FileReader(readFile));
			String line;
			while ((line = reader.readLine()) != null) {
				data.append(line);
			}
			reader.close();
		}
		writer.write("Content-Length:" + data.toString().trim().length() + "\r\n");
		writer.write("\r\n");
		if (inLineData != null) {
			inLineData = inLineData.replace("\'", "");
			writer.write(inLineData);
			writer.write("\r\n");
		}
		if (data.toString().trim().length() >= 1) {
			writer.write(data.toString());
			writer.write("\r\n");
		}
		writer.flush();
		displayOutput();
		writer.close();
		isRedirect(POST_REDIRECT);
	}

	public void getRequest() throws IOException {
		if (!(readFileFlag || inLineDataFlag)) {
			socket = new Socket(getHostName(), getPortNumber());
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
			if (getUrlPath().length() == 0) {
				// writer.println("GET / /1.1");
				writer.println(generateMethodURL("GET", "", ""));
			} else {
				writer.println(generateMethodURL("GET", getUrlPath(), ""));
				// writer.println("GET " + urlPath + " HTTP/1.1");
			}
			writer.println("Host:" + getHostName());
			if (!headerList.isEmpty()) {
				for (int i = 0; i < headerList.size(); i++) {
					if (headerFlag) {
						String[] headerKeyValue = headerList.get(i).split(":");
						writer.println(headerKeyValue[0] + ":" + headerKeyValue[1]);
					}
				}
			}
			writer.println("\r\n");
			writer.flush();
			displayOutput();
			writer.close();
			isRedirect(GET_REDIRECT);
		} else {
			System.out.println("Invalid Command : In GET Request -f or -d are not allowed ");
		}
	}

	public void displayOutput() throws IOException {

		InputStream inputStream = socket.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String output;
		boolean entryFlag = false;
		boolean divideFlag = true;
		StringBuilder receiveContent = new StringBuilder();
		do {
			output = reader.readLine();
			if (output.trim().isEmpty()) {
				entryFlag = true;
				if (entryFlag && divideFlag) {
					divideFlag = false;
					receiveContent.append("Content Separated");
				}
			}
			receiveContent.append(output);
			receiveContent.append("Entry Separated");
		} while ((output.trim() != null) && !(output.endsWith("</html>") || output.endsWith("}")
				|| output.endsWith("post") || output.endsWith("/get")));

		reader.close();
		String[] splitReceiveContent = receiveContent.toString().split("Content Separated");
		String[] responseHeader = splitReceiveContent[0].split("Entry Separated");
		String[] responseBody = splitReceiveContent[1].split("Entry Separated");
		/*
		 * System.out.println("Header " + splitReceiveContent[0]);
		 * System.out.println("Body " + splitReceiveContent[1]);
		 */
		setStatusCode(Integer.parseInt(responseHeader[0].substring(9, 12)));
		for (int i = 0; i < responseHeader.length; i++) {
			if (responseHeader[i].startsWith("Location:")) {
				setNewURL(responseHeader[i].substring(10));
			}
		}
		// For Verbose
		isVerbose(verboseFlag, responseHeader);
		printOutput(responseBody);
		isGenerateFile(generateFileFlag, responseHeader, responseBody);

	}

	public void isRedirect(String requestRedirect) {
		if (getStatusCode() != HTTP_OK && (getStatusCode() == HTTP_MOVED_TEMP || getStatusCode() == HTTP_MOVED_PERM
				|| getStatusCode() == HTTP_SEE_OTHER)) {
			isRedirect = true;
			try {
				isRedirect = false;
				System.out.println("");
				Thread.sleep(1000);
				System.out.println("Status Code :" + getStatusCode());
				Thread.sleep(1000);
				System.out.print("Connecting to:" + getNewURL());
				Thread.sleep(2000);
				System.out.println("Please Wait.......");
				/*
				 * for (int k = 0; k < 4; k++) { Thread.sleep(500); System.out.print("."); }
				 */
				setUrl(newURL);
				getUrlData();
				if (requestRedirect.equals(GET_REDIRECT)) {
					getRequest();
				} else if (requestRedirect.equals(POST_REDIRECT)) {
					postRequest();
				}
				System.out.println("Validated : Redirection");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public void isGenerateFile(boolean flag, String[] headers, String[] messagebody) {
		if (flag) {
			PrintWriter writer;
			if (generateFile != null) {
				try {
					writer = new PrintWriter(generateFile, "UTF-8");
					writer.println("Command: " + input + "\r\n");
					if (verboseFlag) {
						printOutputInFile(writer, headers);
						/*
						 * for (int i = 0; i < headers.length; i++) { writer.println(headers[i]); }
						 */
					}
					writer.println("");
					printOutputInFile(writer, messagebody);
					/*
					 * for (int i = 0; i < messagebody.length; i++) {
					 * writer.println(messagebody[i]); }
					 */
					writer.close();
				} catch (FileNotFoundException | UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * This Function is used to check whether verbose is used in input command or
	 * Not. If verbose used, Prints the detail of the response such as protocol,
	 * status, and headers.
	 */
	public static void isVerbose(boolean flag, String[] responseHeader) {
		if (flag) {
			printOutput(responseHeader);
		}
	}

	public static void printOutput(String[] message) {
		for (int i = 0; i < message.length; i++) {
			System.out.println(message[i]);
		}
		System.out.println("");
	}

	public static void printOutputInFile(PrintWriter writer, String[] message) {
		for (int i = 0; i < message.length; i++) {
			writer.println(message[i]);
		}
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getNewURL() {
		return newURL;
	}

	public void setNewURL(String newURL) {
		this.newURL = newURL;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public String getUrlPath() {
		return urlPath;
	}

	public void setUrlPath(String urlPath) {
		this.urlPath = urlPath;
	}
}