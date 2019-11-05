import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Thread class implementing File Server
 * 
 * @author Himen Sidhpura
 * @author Jenny Mistry
 *
 */
public class httpfsServerThread implements Runnable {

	boolean contentTypeFlag = false;
	boolean dispositionFlag = false;
	boolean inLineFlag = false;
	boolean attachmentFlag = false;
	boolean fileFlag = false;
	boolean httpcFlag = false;
	boolean httpfsFlag = false;
	private Socket socket;
	private PrintWriter writer = null;
	private String inputRequest;
	int clientInstance;
	ArrayList<String> fileHeader;
	Map<String, String> headers;
	Map<String, String> parameters;
	ArrayList<String> filesList;
	String content;
	String statusCode;
	String uri;
	int port;
	String pathDirectory;
	String clientRequest;
	String curlRequest;
	String body;

	/**
	 * Parameterized constructor for the httpfsServerThread class
	 * 
	 * @param serverClient   The client from which the server receives and processes
	 *                       the request
	 * @param pathDirectory1 Directory path to which the user has access to
	 * @param counter        Counter to count the number of clients sending
	 *                       simultaneous requests to Server
	 */
	public httpfsServerThread(Socket serverClient, String pathDirectory1, int counter) {
		this.socket = serverClient;
		setClientInstance(counter);
		setPathDirectory(pathDirectory1);
		Instant instant = Instant.now();
		fileHeader = new ArrayList<>();
		filesList = new ArrayList<>();
		parameters = new HashMap<>();
		headers = new HashMap<>();
		headers.put("Connection", "keep-alive");
		headers.put("Host", "Localhost");
		headers.put("Date", instant.toString());
	}

	/**
	 * run method to implement Runnable Interface
	 */
	public void run() {
		try {
			InputStreamReader inputReader = new InputStreamReader(socket.getInputStream());
			BufferedReader br = new BufferedReader(inputReader);
			writer = new PrintWriter(socket.getOutputStream());
			int count = 0;
			while ((inputRequest = br.readLine()) != null) {
				if (inputRequest.endsWith(Constants.HTTP_VERSION)) {
					setHttpcFlag(true);
					setCurlRequest(inputRequest);
				} else if (inputRequest.matches("(GET|POST)/(.*)")) {
					setHttpfsFlag(true);
					setClientRequest(inputRequest);
				}
				if (isHttpfsFlag()) {
					if (inputRequest.isEmpty())
						break;
					fileHeader.add(inputRequest);
					contentTypeFlag = inputRequest.startsWith(Constants.CONTENT_TYPE) ? true : isContentTypeFlag();
					dispositionFlag = inputRequest.startsWith(Constants.CONTENT_DISPOSITION) ? true
							: isDispositionFlag();
					setBody(inputRequest.startsWith(Constants.INLINE_DATA_CODE2) ? inputRequest.substring(2)
							: getBody());
				}
				if (isHttpcFlag()) {
					if (inputRequest.matches(Constants.REG1)) {
						if (count == 0) {
							addHeaders(inputRequest);
						}
					}
					if (count == 1) {
						setContent(inputRequest);
						break;
					}
					if (inputRequest.isEmpty())
						count++;
				}
			}
			inspectClientFlag();
			writer.println("");
			writer.flush();
			br.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Method to check if the client is of type httpc or httpfs
	 */
	private void inspectClientFlag() {
		if (isHttpcFlag() && getCurlRequest().matches("(GET|POST) /(.*)")) {
			curlRequest();
		}
		if (isHttpfsFlag()) {
			System.out.println("Client Request : " + getClientRequest());
			if (getClientRequest().startsWith(Constants.GET_METHOD)) {
				getRequest();
			} else if (getClientRequest().startsWith(Constants.POST_METHOD)) {
				postRequest();
			}
		}
	}

	/**
	 * This method handles httpc Client sending CUrl requests to the Server
	 */
	public synchronized void curlRequest() {
		setCurlRequest(getCurlRequest().replace("GET /", ""));
		setCurlRequest(getCurlRequest().replace("POST /", ""));
		setCurlRequest(getCurlRequest().replace("HTTP/1.1", ""));
		setStatusCode(Constants.HTTP_200);
		setUri("http://" + Constants.IP_ADDRESS + ":" + getPort() + Constants.SLASH + getCurlRequest());
		writer.println("HTTP/1.0 " + getStatusCode() + " " + getConnectionState() + "\r\n" + getHeaders());
		checkCurlOption();
	}

	/**
	 * method that detects if the curl request sent by the client is of type GET or
	 * POST
	 */
	public void checkCurlOption() {
		if (getCurlRequest().startsWith("get?")) {
			System.out.println("CURL : Request Type GET");
			getCurlOption();
		} else if (getCurlRequest().startsWith("post?")) {
			System.out.println("CURL : Request Type POST");
			postCurlOption();
		}
	}

	/**
	 * method that processes Curl request of type POST
	 */
	public void postCurlOption() {
		setCurlRequest(getCurlRequest().replace("post?", ""));
		if (!getCurlRequest().isEmpty() && getCurlRequest().matches("(.*)=(.*)")) {
			if (getCurlRequest().matches(Constants.REG2)) {
				String[] temp = getCurlRequest().split("&");
				for (int i = 0; i < temp.length; i++) {
					setParameters(temp[i]);
				}
			} else {
				setParameters(getCurlRequest());
			}
		}
		printOutput(getPOSTBody());
	}

	/**
	 * method that processes Curl request of type GET
	 */
	public void getCurlOption() {
		setCurlRequest(getCurlRequest().replace("get?", ""));
		if (getCurlRequest().matches(Constants.REG2)) {
			String[] temp = getCurlRequest().split("&");
			for (int i = 0; i < temp.length; i++) {
				setParameters(temp[i]);
			}
		} else {
			setParameters(getCurlRequest());
		}
		printOutput(getGETBody());
	}

	/**
	 * method to handle the various types of GET request from the client. "GET /"
	 * returns a list of the current files in the data directory.<br>
	 * "GET /foo" returns the content of the file named foo in the data
	 * directory.<br>
	 */
	public synchronized void getRequest() {
		String fileName = isContentTypeFlag() ? getClientRequest().substring(4) + getFileContentHeader()
				: getClientRequest().substring(4);
		File filePath = new File(retrieveFilePath(fileName));
		if (isContentTypeFlag()) {
			try {
				if (filePath.exists()) {
					BufferedReader br = new BufferedReader(new FileReader(filePath));
					String line;
					writer.println("File Content");
					while ((line = br.readLine()) != null) {
						writer.println(line);
					}
					writer.println("Operation Status : Success");
					br.close();
				} else {
					printOutput(Constants.HTTP_404_ERROR);
				}

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (!fileName.contains(Constants.SLASH)) {
			if (filePath.exists()) {
				if (filePath.isDirectory()) {
					HashMap<String, ArrayList<String>> output = new HashMap<>();
					output.put(Constants.DIRECTORY, new ArrayList<String>());
					output.put(Constants.FILE, new ArrayList<String>());
					for (File file : filePath.listFiles()) {
						if (file.isDirectory()) {
							ArrayList<String> temp = output.get(Constants.DIRECTORY);
							temp.add(file.getName());
							output.replace(Constants.DIRECTORY, temp);
						} else if (file.isFile()) {
							ArrayList<String> temp = output.get(Constants.FILE);
							temp.add(file.getName());
							output.replace(Constants.FILE, temp);
						}
					}
					System.out.println("------------\nDIRECTORIES: \n------------");
					for (Entry<String, ArrayList<String>> entry : output.entrySet()) {
						ArrayList<String> temp = entry.getValue();
						for (int i = 0; i < temp.size(); i++) {
							if (entry.getKey().equals(Constants.DIRECTORY)) {
								printOutput(entry.getKey().trim() + temp.get(i).trim());
							}
						}
					}
					System.out.println("------------\n FILES : \n------------");
					for (Entry<String, ArrayList<String>> entry : output.entrySet()) {
						ArrayList<String> temp = entry.getValue();
						for (int i = 0; i < temp.size(); i++) {
							if (entry.getKey().equals(Constants.FILE)) {
								printOutput(entry.getKey().trim() + temp.get(i).trim());
							}
						}
					}
				} else if (filePath.isFile()) {
					PrintWriter fileWriter = null;
					File downloadPath = new File(getPathDirectory() + Constants.PATH_TO_DOWNLOAD);
					if (isDispositionFlag()) {
						getFileDispositionHeader();
						if ((isAttachmentFlag() && !downloadPath.exists())) {
							System.out.println("Download Folder Created : "
									+ new File(getPathDirectory() + Constants.PATH_TO_DOWNLOAD).mkdir());
						}
					}
					try {
						if (isAttachmentFlag()) {
							fileWriter = isFileFlag()
									? new PrintWriter(downloadPath + Constants.SLASH + getFileDispositionHeader())
									: new PrintWriter(downloadPath + Constants.SLASH + fileName);
						}
						BufferedReader br = new BufferedReader(new FileReader(filePath));
						String line;
						while ((line = br.readLine()) != null) {
							if (isDispositionFlag()) {
								if (isInLineFlag()) {
									writer.println(line);
								} else if (isAttachmentFlag()) {
									fileWriter.println(line);
								}
							} else {
								writer.println(line);
							}

						}
						writer.println("Operation Status : Success");
						if (isAttachmentFlag())
							fileWriter.close();
						br.close();
					} catch (FileNotFoundException e) {
						printOutput(Constants.HTTP_404_ERROR + Constants.FILE_NOT_FOUND);
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			} else {
				printOutput(Constants.HTTP_404_ERROR);
			}
		} else {
			printOutput("Error: " + Constants.ACCESS_DENIED);
		}
	}

	/**
	 * method to fetch the directory path of the file
	 * 
	 * @param fileName
	 * @return path
	 */
	private String retrieveFilePath(String fileName) {
		return getPathDirectory() + Constants.SLASH + fileName;
	}

	/**
	 * method to handle the various types of GET request from the client. "POST
	 * /foo" will generate or overwrite the file named foo in the data directory<br>
	 * with the content of the body of the request.<br>
	 * options for the POST such as overwrite=true|false.
	 * 
	 */
	public synchronized void postRequest() {
		if (!getClientRequest().substring(5).contains(Constants.SLASH)) {
			try {
				File filePath = isContentTypeFlag()
						? new File(getPathDirectory() + Constants.SLASH + getClientRequest().substring(5)
								+ getFileContentHeader())
						: new File(getPathDirectory() + Constants.SLASH + getClientRequest().substring(5));

				PrintWriter printWriter = new PrintWriter(filePath);
				printWriter.println(getBody());
				writer.println("Operation Status : Succcess");
				printWriter.close();
			} catch (FileNotFoundException e) {
				writer.print(Constants.HTTP_404_ERROR);
			}
		} else {
			printOutput("Error: " + Constants.ACCESS_DENIED);
		}
	}

	/**
	 * getter method to fetch request body
	 * 
	 * @return body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * setter method to set the request body
	 * 
	 * @param body
	 */
	public void setBody(String body) {
		this.body = body;
	}

	public boolean isInLineFlag() {
		return inLineFlag;
	}

	public void setInLineFlag(boolean inLineFlag) {
		this.inLineFlag = inLineFlag;
	}

	public boolean isAttachmentFlag() {
		return attachmentFlag;
	}

	public void setAttachmentFlag(boolean attachmentFlag) {
		this.attachmentFlag = attachmentFlag;
	}

	public boolean isFileFlag() {
		return fileFlag;
	}

	public void setFileFlag(boolean fileFlag) {
		this.fileFlag = fileFlag;
	}

	/**
	 * getter method to get the Port Number
	 * 
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * setter method to set the Port Number
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * getter method to get the Path to the Directory
	 * 
	 * @return pathDirectory
	 */
	public String getPathDirectory() {
		return pathDirectory;
	}

	/**
	 * setter method to set the Path to the Directory
	 * 
	 * @param pathDirectory
	 */
	public void setPathDirectory(String pathDirectory) {
		this.pathDirectory = pathDirectory;
	}

	/**
	 * getter method to get the Curl command for httpc requests
	 * 
	 * @return curlRequest
	 */
	public String getCurlRequest() {
		return curlRequest;
	}

	/**
	 * setter method to set the Curl command for httpc requests
	 * 
	 * @param curlRequest
	 */
	public void setCurlRequest(String curlRequest) {
		this.curlRequest = curlRequest;
	}

	/**
	 * getter method to get the number of clients sending concurrent requests to the
	 * server
	 * 
	 * @return clientInstance
	 */
	public int getClientInstance() {
		return clientInstance;
	}

	/**
	 * setter method to set the number of clients sending concurrent requests to the
	 * server
	 * 
	 * @param clientInstance
	 */
	public void setClientInstance(int clientInstance) {
		this.clientInstance = clientInstance;
	}

	/**
	 * getter method to check if the requests is of type httpc
	 * 
	 * @return httpcFlag
	 */
	public boolean isHttpcFlag() {
		return httpcFlag;
	}

	/**
	 * setter method to set the httpc mode
	 * 
	 * @param httpcFlag
	 */
	public void setHttpcFlag(boolean httpcFlag) {
		this.httpcFlag = httpcFlag;
	}

	/**
	 * getter method to check if the requests is of type httpfs
	 * 
	 * @return httpfsFlag
	 */
	public boolean isHttpfsFlag() {
		return httpfsFlag;
	}

	/**
	 * setter method to set the httpfs mode
	 * 
	 * @param httpfsFlag
	 */
	public void setHttpfsFlag(boolean httpfsFlag) {
		this.httpfsFlag = httpfsFlag;
	}

	/**
	 * getter method to fetch the client request
	 * 
	 * @return clientRequest
	 */
	public String getClientRequest() {
		return clientRequest;
	}

	/**
	 * setter method to set the client request
	 * 
	 * @param clientRequest
	 */
	public void setClientRequest(String clientRequest) {
		this.clientRequest = clientRequest;
	}

	/**
	 * method to check if Content-Type mode is used or not
	 * 
	 * @return contentTypeFlag
	 */
	public boolean isContentTypeFlag() {
		return contentTypeFlag;
	}

	/**
	 * setter method to set the Content-Type flag
	 * 
	 * @param contentTypeFlag
	 */
	public void setContentTypeFlag(boolean contentTypeFlag) {
		this.contentTypeFlag = contentTypeFlag;
	}

	/**
	 * This method is used to check if Disposition is used or not
	 * 
	 * @return dispositionFlag
	 */
	public boolean isDispositionFlag() {
		return dispositionFlag;
	}

	/**
	 * setter method to set the Disposition mode to true or false
	 * 
	 * @param dispositionFlag
	 */
	public void setDispositionFlag(boolean dispositionFlag) {
		this.dispositionFlag = dispositionFlag;
	}

	public String getFileDispositionHeader() {
		String name = "";
		for (int i = 0; i < fileHeader.size(); i++) {
			if (fileHeader.get(i).trim().startsWith(Constants.CONTENT_DISPOSITION)) {
				String[] temp = fileHeader.get(i).trim().split(";");
				String[] temp2 = temp[0].trim().split(":");
				if (temp2[1].trim().equals("attachment")) {
					setAttachmentFlag(true);
					if (temp.length == 2) {
						String[] temp3 = temp[1].trim().split(":");
						name = temp3[1].trim();
						setFileFlag(true);
					}
				}
				if (temp2[1].trim().equals("inline")) {
					setInLineFlag(true);
				}
			}
		}
		return name.trim();
	}

	public String getFileContentHeader() {
		String ext = "";
		for (int i = 0; i < fileHeader.size(); i++) {
			if (fileHeader.get(i).trim().startsWith(Constants.CONTENT_TYPE)) {
				String[] temp = fileHeader.get(i).trim().split(":");
				ext = getExtension(temp[1].trim());
			}
		}
		return ext.trim();
	}

	public String getExtension(String ext) {
		String temp = "";
		switch (ext.trim()) {
		case "application/text":
			temp = ".txt";
			break;
		case "application/json":
			temp = ".json";
			break;
		default:
			temp = "";
			break;
		}
		return temp;
	}

	public String getHeaders() {
		StringBuilder head = new StringBuilder();
		for (Entry<String, String> entry : headers.entrySet()) {
			head.append(" " + entry.getKey() + ": " + entry.getValue() + "\r\n");
		}
		return head.toString().trim();
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public void addHeaders(String values) {
		String[] value = values.trim().split(":");
		headers.put(value[0].trim(), value[1].trim());
	}

	public String getGETBody() {
		return "{\r\n" + " \"args\":{" + getParameters() + "},\r\n" + " \"headers\":{\r\n" + getHeaders() + "},\r\n"
				+ " \"origin\": " + Constants.ORIGIN + ",\r\n" + " \"url\": " + getUri() + ",\r\n" + "}";
	}

	public String getPOSTBody() {
		return "{\r\n" + " " + "\"args\":{" + " " + this.getParameters() + "},\r\n" + " " + "\"data\":{" + " "
				+ this.getContent() + "},\r\n" + " " + "\"files\":{\r\n" + " " + getFiles() + "},\r\n" + " "
				+ "\"headers\":{\r\n" + this.getHeaders() + " },\r\n" + " " + "\"json\": { },\r\n" + " "
				+ "\"origin\": " + Constants.ORIGIN + ",\r\n" + " " + "\"url\": " + getUri() + ",\r\n" + "}";
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setFiles(String fileName) {
		filesList.add(fileName);
	}

	/*
	 * getter method to get the file name from the list of files
	 */
	public String getFiles() {
		StringBuilder temp = new StringBuilder();
		for (int i = 0; i < filesList.size(); i++) {
			temp.append(filesList.get(i) + ",");
		}
		return temp.toString();
	}

	public void setStatusCode(String status) {
		this.statusCode = status;
	}

	/**
	 * getter method to get the status code
	 * 
	 * @return statusCode
	 */
	public String getStatusCode() {
		return this.statusCode;
	}

	public String getConnectionState() {
		String temp = getStatusCode();
		String sCode = "";
		switch (temp) {
		case Constants.HTTP_200:
			sCode = "OK";
			break;
		case Constants.HTTP_400:
			sCode = "Bad Request";
			break;
		case Constants.HTTP_404:
			sCode = "Not Found";
			break;
		default:
			sCode = "ERROR HTTP";
			break;
		}
		return sCode;
	}


	public String getParameters() {
		StringBuilder temp = new StringBuilder();
		temp.append("\r\n");
		for (Entry<String, String> entry : parameters.entrySet()) {
			temp.append(" \"" + entry.getKey() + "\": \"" + entry.getValue() + "\",\r\n");
		}
		return temp.toString();
	}

	/**
	 * setter method to set the parameters
	 * 
	 * @param values
	 */
	public void setParameters(String values/* String key, String value */) {
		String[] value = values.trim().split("=");
		parameters.put(value[0].trim(), value[1].trim());
	}

	/**
	 * setter method to set the Uri from the request
	 * 
	 * @param uri
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * getter method to get the Uri from the request
	 * 
	 * @return uri
	 */
	public String getUri() {
		return this.uri;
	}

	public void printOutput(String msg) {
		writer.println(msg);
		System.out.println(msg);
	}
}