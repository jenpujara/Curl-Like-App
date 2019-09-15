package com.lab.one;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class httpcImpl {

	public static final int HTTP_OK = 200;
	public static final int HTTP_MULT_CHOICE = 300;
	public static final int HTTP_MOVED_PERM = 301;
	public static final int HTTP_MOVED_TEMP = 302;
	public static final int HTTP_SEE_OTHER = 303;
	public static final int HTTP_NOT_MODIFIED = 304;
	public static final int HTTP_USE_PROXY = 305;
	ArrayList<String> headers = new ArrayList<>();
	String userInput;
	String requestCommand;
	String inlineData = null;
	String fileToRead;
	String fileData = new String();
	String fileToWrite;
	String url;
	String host;
	String path;
	String query;
	String protocol;
	String Location;

	int port;
	int statusCode;

	URI uri;
	Socket socket;
	PrintWriter request;
	BufferedWriter wr;

	boolean verbose = false;
	boolean headerOption = false;
	boolean inlineDataOption = false;
	boolean sendfileOption = false;
	boolean writeToFileOption = false;
	boolean isRedirect = false;
	String input;

	public httpcImpl(String input) {
		input = this.input;
	}

	public void parseInput() throws URISyntaxException {
		String[] splitInput = input.split("//s+");
		ArrayList<String> inputList = new ArrayList<>();
		for (int i = 0; i < splitInput.length; i++) {
			if (!splitInput[i].equals("") || splitInput.length != 0) {
				inputList.add(splitInput[i]);
			}
		}
		if (inputList.get(0).equals("httpc") && (inputList.get(1).equals("get") || inputList.get(1).equals("post"))) {
			for (int i = 0; i < inputList.size(); i++) {
				if (inputList.get(i).equals("-v")) {
					verbose = true;
				}
				if (inputList.get(i).equals("-h")) {
					headerOption = true;
					headers.add(inputList.get(++i));
				}
				if (inputList.get(i).equals("-d") || inputList.get(i).equals("--d")) {
					inlineDataOption = true;
					inlineData = inputList.get(++i);
				}
				if (inputList.get(i).equals("-f")) {
					sendfileOption = true;
					fileToRead = inputList.get(++i);
				}
				if (inputList.get(i).equals("-o")) {
					writeToFileOption = true;
					fileToWrite = inputList.get(++i);
				}
				if (inputList.get(i).startsWith("http://") || inputList.get(i).startsWith("https://")) {
					url = inputList.get(i);
				}
				if (url != null) {
					this.getUrlData(url);
					if (!(sendfileOption && inlineDataOption)) {
						if (requestCommand.equals("get")) {
							if (!(sendfileOption || inlineDataOption)) {
								try {
									this.getRequest();
								} catch (UnknownHostException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							} else {
								System.out.println("-f or -d are not allowed in GET Request");
							}
						} else if (requestCommand.equals("post")) {
							try {
								this.postRequest();
							} catch (UnknownHostException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					} else {
						System.out.println("Invalid Command: -f and -d both are not allowed.");
					}
				} else {
					System.out.println("Invalid URL");
				}
			}
		} else {
			System.out.println("Invalid Command");
		}

	}

	public void getUrlData(String url2) throws URISyntaxException {

		uri = new URI(url2);
		host = uri.getHost();
		path = uri.getRawPath();
		query = uri.getRawQuery();
		protocol = uri.getScheme();
		port = uri.getPort();

		if (path == null || path.length() == 0) {
			path = "";
		}
		if (query == null || query.length() == 0) {
			query = "";
		}
		if (query.length() > 0 || path.length() > 0) {
			path = path + "?" + query;
		}

		if (port == -1) {
			if (protocol.equals("http")) {
				port = 80;
			}
			if (protocol.equals("https")) {
				port = 443;
			}
		}
	}

	public void getRequest() throws UnknownHostException, IOException {

		socket = new Socket(host, port);
		request = new PrintWriter(socket.getOutputStream());

		if (path.length() == 0) {
			request.println("GET / HTTP/1.1");
		} else {
			request.println("GET " + path + " HTTP/1.1");
		}
		request.println("Host:" + host);

		// to send headers
		if (!headers.isEmpty()) {
			for (int i = 0; i < headers.size(); i++) {
				if (headerOption) {
					String[] headerKeyValue = headers.get(i).split(":");
					request.println(headerKeyValue[0] + ":" + headerKeyValue[1]);
				}
			}
		}
		request.println("\r\n");
		request.flush();
		this.printToConsole();
		request.close();
		this.checkForRedirection("getRedirect");
	}

	public void postRequest() throws UnknownHostException, IOException {

		socket = new Socket(host, port);
		wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

		if (path.length() == 0) {
			wr.write("POST / HTTP/1.1\r\n");
		} else {
			wr.write("POST " + path + " HTTP/1.1\r\n");
		}
		wr.write("Host:" + host + "\r\n");

		if (headerOption) {
			if (!headers.isEmpty()) {
				for (int i = 0; i < headers.size(); i++) {
					String[] headerKeyValue = headers.get(i).split(":");
					wr.write(headerKeyValue[0] + ":" + headerKeyValue[1] + "\r\n");
				}
			}
		}
		if (inlineDataOption) {
			// wr.write("Content-Type: application/json\r\n");
			wr.write("Content-Length:" + inlineData.length() + "\r\n");
		} else if (sendfileOption) {
			FileReader fr = new FileReader(fileToRead);
			BufferedReader brreader = new BufferedReader(fr);
			String sCurrentLine;
			while ((sCurrentLine = brreader.readLine()) != null) {
				fileData = fileData + sCurrentLine;
			}
			wr.write("Content-Length:" + fileData.length() + "\r\n");
			// wr.write(fileData);
		}

		wr.write("\r\n");
		if (inlineData != null) {
			inlineData = inlineData.replace("\'", "");
			wr.write(inlineData);
			wr.write("\r\n");
		}
		if (fileData != null) {
			wr.write(fileData);
			wr.write("\r\n");
		}
		wr.flush();
		this.printToConsole();
		wr.close();
		this.checkForRedirection("postRedirect");

	}
	public void printToConsole() throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String outputmsg;
		int count = 0;
		StringBuilder content = new StringBuilder();
		
		do {
			outputmsg = br.readLine();
			if (outputmsg.isEmpty()) {
				count++;
				if (count == 1) {
					content.append("|");
				}
			}
			content.append(outputmsg);
			content.append("^");

		} while ((outputmsg != null) && !(outputmsg.endsWith("}") || outputmsg.endsWith("</html>")
				|| outputmsg.endsWith("/get") || outputmsg.endsWith("post")));
		
		br.close();

		// System.out.println(content);
		String[] contentdevide = content.toString().split("\\|");
		String[] headers = contentdevide[0].split("\\^");
		String[] messagebody = contentdevide[1].split("\\^");

		// code for redirect
		statusCode = Integer.parseInt(headers[0].substring(9, 12));
		for (int k = 0; k < headers.length; k++) {

			if (headers[k].startsWith("Location:")) {
				Location = headers[k].substring(10);
			}
		}
		// end of code for redirect

		// For Verbose
		if (verbose) {
			// headers
			for (int i = 0; i < headers.length; i++) {
				System.out.println(headers[i]);

			}
			System.out.println("");
			for (int m = 0; m < messagebody.length; m++) {
				System.out.println(messagebody[m]);
			}
		}else {
			for (int m = 0; m < messagebody.length; m++) {
				System.out.println(messagebody[m]);
			}
		}

		if(writeToFileOption) {
			this.writeToFile(headers,messagebody);
		}
	}
	
	
	/**
	 * this method is used to write the RESPONSE from Server to the FILE.
	 * 
	 * @param headers headers part of REPONSE.
	 * @param messagebody body part of RESPONSE.
	 */
	public void writeToFile(String[] headers, String[] messagebody) {
		PrintWriter writer;
		if (fileToWrite != null) {
			try {
				writer = new PrintWriter(fileToWrite, "UTF-8");
				writer.println("Command: " + userInput + "\r\n");

				if (verbose) {
					for (int i = 0; i < headers.length; i++) {
						writer.println(headers[i]);
					}
				}
				writer.println("");
				for (int k = 0; k < messagebody.length; k++) {
					writer.println(messagebody[k]);
				}
				writer.close();
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * this method is used to check if the Server RESPONDS with 301 code? if yes, then it will process REDIRECTION.
	 * REDIRECT SEPCIFICATION:301 Moved Permanently
	 * 
	 * @param requestRedirect a string contains redirect request from "getRequest" or "postRequest".
	 */
	private void checkForRedirection(String requestRedirect) {
		if (statusCode != HTTP_OK) {
			if (statusCode == HTTP_MOVED_TEMP || statusCode == HTTP_MOVED_PERM
					|| statusCode == HTTP_SEE_OTHER)
				isRedirect = true;
		}

		if (isRedirect) {
			try {
				isRedirect = false;
				System.out.println("");
				Thread.sleep(1000);
				System.out.println("Status Code :"+statusCode);
				Thread.sleep(1000);
				System.out.print("Connecting to:"+Location);
				for(int k = 0; k<4 ; k++) {
					Thread.sleep(500);
					System.out.print(".");
				}
				System.out.println("");
				System.out.println("");

				this.getUrlData(Location);
				if(requestRedirect.equals("getRedirect")) {
					this.getRequest();
				}
				else if(requestRedirect.equals("postRedirect")) {
					this.postRequest();
				}
				System.out.println("Redirect Done...");

			} catch (URISyntaxException e) {

				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	

}
