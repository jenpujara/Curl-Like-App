package com.lab.one;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class httpc {

	ArrayList<String> headerList = new ArrayList<>();

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

	Socket socket;

	public static final int HTTP_OK = 200;
	public static final int HTTP_MULT_CHOICE = 300;
	public static final int HTTP_MOVED_PERM = 301;
	public static final int HTTP_MOVED_TEMP = 302;
	public static final int HTTP_SEE_OTHER = 303;
	public static final int HTTP_NOT_MODIFIED = 304;
	public static final int HTTP_USE_PROXY = 305;
	public static final String GET_REDIRECT = "getRedirect";
	public static final String POST_REDIRECT = "postRedirect";
	public static final String HTTP = "http";
	public static final String HTTPS = "https";
	public static final int HTTP_PORT = 80;
	public static final int HTTPS_PORT = 443;

	public static void main(String[] args) {
		while (true) {
			System.out.print(">> ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String input = "";
			try {
				input = br.readLine();
				if (input.equalsIgnoreCase("exit")) {
					break;
				}
				httpc httpcMain = new httpc(input);
				httpcMain.parseInput();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public httpc(String input1) {
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
		if (inputList.get(0).equals("httpc") && (inputList.get(1).equals("get") || inputList.get(1).equals("post"))) {
			for (int i = 0; i < inputList.size(); i++) {
				if (inputList.get(i).equals("-v")) {
					verboseFlag = true;
				}
				if (inputList.get(i).equals("-h")) {
					headerFlag = true;
					headerList.add(inputList.get(++i));
				}
				if (inputList.get(i).equals("--d") || inputList.get(i).equals("-d")) {
					inLineDataFlag = true;
					inLineData = inputList.get(++i);
				}
				if (inputList.get(i).equals("-f")) {
					readFileFlag = true;
					readFile = inputList.get(++i);
				}
				if (inputList.get(i).equals("-o")) {
					generateFileFlag = true;
					generateFile = inputList.get(++i);
				}
				if (inputList.get(i).startsWith("http://") || inputList.get(i).startsWith("https://")) {
					url = inputList.get(i);
				}
			}
			if (url != null) {
				getUrlData();
				if (!(readFileFlag && inLineDataFlag)) {
					if (inputList.get(1).equals("post")) {
						this.postRequest();
					} else if (inputList.get(1).equals("get")) {
						if (!(readFileFlag || inLineDataFlag)) {
							this.getRequest();
						} else {
							System.out.println("Invalid Command : In GET Request -f or -d are not allowed ");
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
		} else {
			System.out.println("Invalid Command");
		}

	}

	private void getUrlData() {
		try {
			URL urL = new URL(url);
			hostName = urL.getHost();
			protocolName = urL.getProtocol();
			portNumber = urL.getPort();
			query = urL.getQuery();
			urlPath = urL.getPath();
			fileName = urL.getFile();
			referenceName = urL.getRef();
			if (hostName == null || hostName.length() == 0) {
				hostName = "";
			}
			if (portNumber == -1) {
				if (protocolName.equals(HTTP)) {
					portNumber = HTTP_PORT;
				} else if (protocolName.equals(HTTPS)) {
					portNumber = HTTPS_PORT;
				}
			}
			if (query == null || query.length() == 0) {
				query = "";
			}
			if (query.length() > 0 || urlPath.length() > 0) {
				urlPath = urlPath + "?" + query;
			}
			if (fileName == null || fileName.length() == 0) {
				fileName = "";
			}
			if (referenceName == null || referenceName.length() == 0) {
				referenceName = "";
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public void postRequest() {

	}

	public void getRequest() throws UnknownHostException, IOException {
		socket = new Socket(hostName, portNumber);
		PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
		if (urlPath.length() == 0) {
			writer.println("GET / 	/1.1");
		} else {
			writer.println("GET " + urlPath + " HTTP/1.1");
		}
		writer.println("Host:" + hostName);
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
		printToConsole();
		writer.close();
		isRedirect(GET_REDIRECT);
	}

	public void printToConsole() throws IOException {

		InputStream inputStream = socket.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String outputmsg;
		boolean entryFlag = false;
		boolean divideFlag = true;
		StringBuilder receiveContent = new StringBuilder();
		do {
			outputmsg = reader.readLine();
			if (outputmsg.isEmpty()) {
				entryFlag = true;
				if (entryFlag && divideFlag) {
					divideFlag = false;
					receiveContent.append("Content Separated");
				}
			}
			receiveContent.append(outputmsg);
			receiveContent.append("Entry Separated");

		} while ((outputmsg != null) && !(outputmsg.endsWith("}") || outputmsg.endsWith("</html>")
				|| outputmsg.endsWith("/get") || outputmsg.endsWith("post")));

		reader.close();
		String[] splitReceiveContent = receiveContent.toString().split("Content Separated");
		String[] responseHeader = splitReceiveContent[0].split("Entry Separated");
		String[] responseBody = splitReceiveContent[1].split("Entry Separated");
		System.out.println("Header " + splitReceiveContent[0]);
		System.out.println("Body " + splitReceiveContent[1]);

		// code for redirect
		statusCode = Integer.parseInt(responseHeader[0].substring(9, 12));
		System.out.println("Status Code" + statusCode);
		for (int i = 0; i < responseHeader.length; i++) {
			if (responseHeader[i].startsWith("Location:")) {
				newURL = responseHeader[i].substring(10);
			}
		}
		// end of code for redirect

		// For Verbose
		isVerbose(verboseFlag, responseHeader);

		for (int i = 0; i < responseBody.length; i++) {
			System.out.println(responseBody[i]);
		}

		if (generateFileFlag) {
			generateFile(responseHeader, responseBody);
		}
	}

	/*
	 * This Function is used to check whether verbose is used in input command or
	 * Not. If verbose used, Prints the detail of the response such as protocol,
	 * status, and headers.
	 */
	public static void isVerbose(boolean tempFlag, String[] responseHeader) {
		if (tempFlag) {
			for (int i = 0; i < responseHeader.length; i++) {
				System.out.println(responseHeader[i]);
			}
			System.out.println("");
		}
	}

	public void isRedirect(String requestRedirect) {
		if (statusCode != HTTP_OK
				&& (statusCode == HTTP_MOVED_TEMP || statusCode == HTTP_MOVED_PERM || statusCode == HTTP_SEE_OTHER)) {
			isRedirect = true;
		}
		if (isRedirect) {
			try {
				isRedirect = false;
				System.out.println("");
				Thread.sleep(1000);
				System.out.println("Status Code :" + statusCode);
				Thread.sleep(1000);
				System.out.print("Connecting to:" + newURL);
				for (int k = 0; k < 4; k++) {
					Thread.sleep(500);
					System.out.print(".");
				}
				System.out.println("");
				url = newURL;
				getUrlData();
				if (requestRedirect.equals(GET_REDIRECT)) {
					getRequest();
				} else if (requestRedirect.equals(POST_REDIRECT)) {
					postRequest();
				}
				System.out.println("Valided : Redirection");
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				System.out.println("Server not found: " + e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void generateFile(String[] headers, String[] messagebody) {
		PrintWriter writer;
		if (generateFile != null) {
			try {
				writer = new PrintWriter(generateFile, "UTF-8");
				writer.println("Command: " + input + "\r\n");
				if (verboseFlag) {
					for (int i = 0; i < headers.length; i++) {
						writer.println(headers[i]);
					}
				}
				writer.println("");
				for (int i = 0; i < messagebody.length; i++) {
					writer.println(messagebody[i]);
				}
				writer.close();
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}

}
