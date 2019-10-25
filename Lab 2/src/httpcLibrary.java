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

/**
 * httpcLibrary class contains various methods to develop a library for cURL commands.
 *
 * @author Himen Sidhpura
 * @author Jenny Mistry 
 * @version 1.0.0
 */

/**
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

	String inLineData = "";
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

	/**
	 * Getter for httpcLibrary
	 * 
	 * @param input1
	 */
	public httpcLibrary(String input1) {
		input = input1;
	}

	/**
	 * This method is used to parse the provided input.
	 * 
	 * @throws IOException
	 */

	public void parseInput() throws IOException {
		String[] splitInput = input.trim().split(" ");
		ArrayList<String> inputList = new ArrayList<>();
		for (int i = 0; i < splitInput.length; i++) {
			if (splitInput[i].trim().length() != 0) {
				inputList.add(splitInput[i]);
			}
		}
		if (inputList.get(0).equals(Constants.HTTPC) && (inputList.get(1).equals(Constants.HELP))) {
			if (inputList.size() == 2)
				printHelp("NOTHING");
			else
				printHelp(inputList.get(2));
		} else if (inputList.get(0).equals(Constants.HTTPC)
				&& (inputList.get(1).equals(Constants.GET) || inputList.get(1).equals(Constants.POST))) {
			for (int i = 0; i < inputList.size(); i++) {
				switch (inputList.get(i)) {
				case Constants.VERBOSE_CODE:
					setVerboseFlag(true);
					break;

				case Constants.HEADER_CODE:
					setHeaderFlag(true);
					headerList.add(inputList.get(++i));
					break;

				case Constants.INLINE_DATA_CODE1:
					setInLineDataFlag(true);
					for (int j = ++i; j < inputList.size(); j++) {
						if (inputList.get(j).substring(inputList.get(j).length() - 1).equals("}")) {
							inLineData += inputList.get(j);
							break;
						} else {
							inLineData += inputList.get(j);
						}
					}
					break;

				case Constants.INLINE_DATA_CODE2:
					setInLineDataFlag(true);
					for (int j = ++i; j < inputList.size(); j++) {
						if (inputList.get(j).substring(inputList.get(j).length() - 1).equals("}")) {
							inLineData += inputList.get(j);
							break;
						} else {
							inLineData += inputList.get(j);
						}
					}
					break;

				case Constants.READFILE_CODE:
					setReadFileFlag(true);
					readFile = inputList.get(++i);
					break;

				case Constants.CREATE_FILE_CODE:
					setGenerateFileFlag(true);
					generateFile = inputList.get(++i);
					break;

				default:
					break;
				}
				if (inputList.get(i).startsWith("http://") || inputList.get(i).startsWith("https://")) {
					setUrl(inputList.get(i));
				}

			}
			// inputList.get(1).trim() this will give whether request is GET or POST
			processRequest(inputList.get(1).trim());
		} else {
			System.out.println("Invalid Command");
		}

	}

	public void processRequest(String requestType) {
		if (getUrl() != null) {
			getUrlData();
			if (!(isReadFileFlag() && isInLineDataFlag())) {
				if (requestType.equals(Constants.POST)) {
					try {
						postRequest();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (requestType.equals(Constants.GET)) {
					try {
						getRequest();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("No Post and Get Found in Input");
				}
			} else {
				System.out.println("Invalid Command: -f and -d both are not allowed.");
			}
		} else {
			System.out.println("Invalid URL");
		}
	}

	/**
	 * printHelp method prints the details for GET and POST when help option is
	 * typed in.
	 * 
	 * @param option
	 *            represents either GET or POST method.
	 */
	public static void printHelp(String option) {
		if (option.equals(Constants.POST)) {
			System.out.println(
					"usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\nPost executes a HTTP POST request for a given URL with inline data or from file.\n -v Prints the detail of the response such as protocol, status, and headers.\n -h key:value Associates headers to HTTP Request with the format 'key:value'.\n -d string Associates an inline data to the body HTTP POST request. \n -f file Associates the content of a file to the body HTTP POST request.\nEither [-d] or [-f] can be used but not both.");

		} else if (option.equals(Constants.GET)) {
			System.out.println("usage: httpc get [-v] [-h key:value] URL\r\n"
					+ "Get executes a HTTP GET request for a given URL.\r\n"
					+ " -v Prints the detail of the response such as protocol, status,\r\n" + "and headers.\r\n"
					+ " -h key:value Associates headers to HTTP Request with the format\r\n" + "'key:value'.");
		} else if (option.equals("NOTHING")) {
			System.out.println(
					"httpc is a curl-like application but supports HTTP protocol only.\r\n"
					+"Usage:\r\n\t httpc command [arguments] \r\nThe commands are: \r\n\tget\texecutes a HTTP GET request and prints the response.\r\n\tpost\texecutes a HTTP POST request and prints the response. \r\n\thelp\tprints this screen. \r\nUse \"httpc help [command]\" for more information about a command.");
		}
	}

	/**
	 * getUrlData fetches the information required for the Url.
	 */
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
				if (protocolName.equals(Constants.HTTP)) {
					setPortNumber(Constants.HTTP_PORT);
				} else if (protocolName.equals(Constants.HTTPS)) {
					setPortNumber(Constants.HTTPS_PORT);
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
			System.out.println(e.getMessage());
		}
	}

	/**
	 * postRequest provides the implementation of HTTP POST method.
	 * 
	 * @throws IOException
	 */
	public void postRequest() throws IOException {

		socket = new Socket(getHostName(), getPortNumber());
		StringBuilder data = new StringBuilder();
		/*
		 * BufferedWriter writer = new BufferedWriter(new
		 * OutputStreamWriter(socket.getOutputStream())); if (urlPath.length() == 0) {
		 * writer.write("POST / HTTP/1.1\r\n"); } else { writer.write("POST " + urlPath
		 * + " HTTP/1.1\r\n"); }
		 */
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
		if (urlPath.length() != 0) {
			writer.write(generateMethodURL("POST", getUrlPath(), " HTTP/1.1\r\n"));
		} else {
			writer.write(generateMethodURL("POST", "", " HTTP/1.1\r\n"));
		}
		writer.write("Host:" + getHostName() + "\r\n");
		if (isHeaderFlag() && !headerList.isEmpty()) {
			for (int i = 0; i < headerList.size(); i++) {
				String[] headerKeyValue = headerList.get(i).split(":");
				writer.write(headerKeyValue[0] + ":" + headerKeyValue[1] + "\r\n");
			}
		}
		if (isInLineDataFlag()) {
			writer.write("Content-Length:" + inLineData.length() + "\r\n");
		} else if (isReadFileFlag()) {
			BufferedReader reader = new BufferedReader(new FileReader(readFile));
			String line;
			while ((line = reader.readLine()) != null) {
				data.append(line);
			}
			writer.write("Content-Length:" + data.toString().length() + "\r\n");
			System.out.println("Data " + data.toString());
			reader.close();
		}
		writer.write("\r\n");
		if (inLineData != null) {
			inLineData = inLineData.replace("\'", "");
			writer.write(inLineData);
			writer.write("\r\n");
		}
		if (data.toString() != null) {
			writer.write(data.toString());
			writer.write("\r\n");
		}
		writer.flush();
		displayOutput();
		writer.close();
		isRedirect(Constants.POST_REDIRECT);
	}

	/**
	 * getRequest provides the implementation of HTTP GET method.
	 * 
	 * @throws IOException
	 */
	public void getRequest() throws IOException {
		if (!(isReadFileFlag() || isInLineDataFlag())) {
			socket = new Socket(getHostName(), getPortNumber());
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
			if (getUrlPath().length() == 0) {
				writer.println(generateMethodURL("GET", "", ""));
			} else {
				writer.println(generateMethodURL("GET", getUrlPath(), ""));
			}
			writer.println("Host:" + getHostName());
			if (!headerList.isEmpty()) {
				for (int i = 0; i < headerList.size(); i++) {
					if (isHeaderFlag()) {
						String[] headerKeyValue = headerList.get(i).split(":");
						writer.println(headerKeyValue[0] + ":" + headerKeyValue[1]);
					}
				}
			}
			writer.println("\r\n");
			writer.flush();
			displayOutput();
			writer.close();
			isRedirect(Constants.GET_REDIRECT);
		} else {
			System.out.println("Invalid Command : In GET Request -f or -d are not allowed ");
		}
	}

	/**
	 * This method uses Sockets to take the input and writes the result on the
	 * console.
	 * 
	 * @throws IOException
	 */
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
		setStatusCode(Integer.parseInt(responseHeader[0].substring(9, 12)));
		for (int i = 0; i < responseHeader.length; i++) {
			if (responseHeader[i].startsWith("Location:")) {
				setNewURL(responseHeader[i].substring(10));
			}
		}
		isVerbose(isVerboseFlag(), responseHeader);
		printOutput(responseBody);
		isGenerateFile(isGenerateFileFlag(), responseHeader, responseBody);

	}

	/**
	 * isRedirect method checks the status code in the url and based on code,
	 * redirects to the corresponding page
	 * 
	 * @param requestRedirect
	 */
	public void isRedirect(String requestRedirect) {
		if (getStatusCode() != Constants.HTTP_OK && (getStatusCode() == Constants.HTTP_MOVED_TEMP
				|| getStatusCode() == Constants.HTTP_MOVED_PERM || getStatusCode() == Constants.HTTP_SEE_OTHER)) {
			setReadFileFlag(true);
			try {
				setRedirect(false);
				sleepThread(1000);
				System.out.println("\nStatus Code :" + getStatusCode());
				sleepThread(1000);
				System.out.print("Redirecting to:" + getNewURL() + "\n Please Wait............");
				sleepThread(2000);
				setUrl(newURL);
				getUrlData();
				if (requestRedirect.equals(Constants.GET_REDIRECT))
					getRequest();
				else if (requestRedirect.equals(Constants.POST_REDIRECT))
					postRequest();
				System.out.println("Validated : Redirection");
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}

	}

	public static void sleepThread(int seconds) {
		try {
			Thread.sleep(seconds);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * This method forms the format for the based on the method type.
	 * 
	 * @param method
	 *            can be either GET or POST.
	 * @param tempURL
	 *            determines the entire URLpath
	 * @param type
	 *            describes the type of version for HTTP Protocol
	 * @return String
	 */
	public static String generateMethodURL(String method, String tempURL, String type) {
		if (method.equals("POST")) {
			if (tempURL.length() != 0) {
				return "POST " + tempURL + " HTTP/1.1\r\n";
			} else {
				return "POST / HTTP/1.1\r\n";
			}
		} else if (method.equals("GET")) {
			if (tempURL.length() == 0) {
				return "GET / /1.1";
			} else {
				return "GET " + tempURL + " HTTP/1.1";
			}
		}
		return "";
	}

	/**
	 * isGenerateFile method creates a file with the header and body passed in the
	 * parameters if CREATE_FILE_CODE is present in the Url.
	 * 
	 * @param flag
	 *            checks if generateFileFlag is true or false.
	 * @param headers
	 *            provides the header values to be used for file.
	 * @param messagebody
	 *            provides the main content for the file.
	 */
	public void isGenerateFile(boolean flag, String[] headers, String[] messagebody) {
		if (flag) {
			PrintWriter writer;
			if (generateFile != null) {
				try {
					writer = new PrintWriter(generateFile, "UTF-8");
					writer.println("Command: " + input + "\r\n");
					if (isVerboseFlag())
						printOutputInFile(writer, headers);

					writer.println("");
					printOutputInFile(writer, messagebody);
					writer.close();
				} catch (FileNotFoundException | UnsupportedEncodingException e) {
					System.out.println(e.getMessage());
				}
			}
		}
	}

	/**
	 * This Function is used to check whether verbose is used in input command or
	 * Not. If verbose used, Prints the detail of the response such as protocol,
	 * status, and headers.
	 */
	public static void isVerbose(boolean flag, String[] responseHeader) {
		if (flag) {
			printOutput(responseHeader);
		}
	}

	/**
	 * This method prints the message passed as a parameter
	 * 
	 * @param message
	 *            to be printed
	 */
	public static void printOutput(String[] message) {
		for (int i = 0; i < message.length; i++) {
			System.out.println(message[i]);
		}
		System.out.println("");
	}

	/**
	 * This method is used to print the data in file for CREATE_FILE_CODE option.
	 * 
	 * @param writer
	 * @param message
	 */
	public static void printOutputInFile(PrintWriter writer, String[] message) {
		for (int i = 0; i < message.length; i++) {
			writer.println(message[i]);
		}
	}

	/**
	 * Getter for fetching url.
	 * 
	 * @return url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Setter for setting the url.
	 * 
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Getter for fetching the new url.
	 * 
	 * @return newURL
	 */
	public String getNewURL() {
		return newURL;
	}

	/**
	 * Setter for setting the new url.
	 * 
	 * @param newURL
	 */
	public void setNewURL(String newURL) {
		this.newURL = newURL;
	}

	/**
	 * Getter for fetching the status code.
	 * 
	 * @return statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Setter for setting the status code.
	 * 
	 * @param statusCode
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Getter for fetching the host name of the url.
	 * 
	 * @return hostName
	 */
	public String getHostName() {
		return hostName;
	}

	/**
	 * Setter for setting the host name of the url.
	 * 
	 * @param hostName
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * Getter for fetching the port number of the url.
	 * 
	 * @return portNumber
	 */
	public int getPortNumber() {
		return portNumber;
	}

	/**
	 * Setter for setting the port number of the url.
	 * 
	 * @param portNumber
	 */
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	/**
	 * Getter for fetching the path of the url.
	 * 
	 * @return urlPath
	 */
	public String getUrlPath() {
		return urlPath;
	}

	/**
	 * Setter for setting the path of the url.
	 * 
	 * @param urlPath
	 */
	public void setUrlPath(String urlPath) {
		this.urlPath = urlPath;
	}

	public boolean isVerboseFlag() {
		return verboseFlag;
	}

	public void setVerboseFlag(boolean verboseFlag) {
		this.verboseFlag = verboseFlag;
	}

	public boolean isHeaderFlag() {
		return headerFlag;
	}

	public void setHeaderFlag(boolean headerFlag) {
		this.headerFlag = headerFlag;
	}

	public boolean isInLineDataFlag() {
		return inLineDataFlag;
	}

	public void setInLineDataFlag(boolean inLineDataFlag) {
		this.inLineDataFlag = inLineDataFlag;
	}

	public boolean isReadFileFlag() {
		return readFileFlag;
	}

	public void setReadFileFlag(boolean readFileFlag) {
		this.readFileFlag = readFileFlag;
	}

	public boolean isGenerateFileFlag() {
		return generateFileFlag;
	}

	public void setGenerateFileFlag(boolean generateFileFlag) {
		this.generateFileFlag = generateFileFlag;
	}

	public boolean isRedirect() {
		return isRedirect;
	}

	public void setRedirect(boolean isRedirect) {
		this.isRedirect = isRedirect;
	}

}