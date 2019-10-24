import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class httpfsServerThread implements Runnable {

	boolean contentTypeFlag = false;

	public boolean isContentTypeFlag() {
		return contentTypeFlag;
	}

	public void setContentTypeFlag(boolean contentTypeFlag) {
		this.contentTypeFlag = contentTypeFlag;
	}

	public boolean isDispositionFlag() {
		return dispositionFlag;
	}

	public void setDispositionFlag(boolean dispositionFlag) {
		this.dispositionFlag = dispositionFlag;
	}

	boolean dispositionFlag = false;
	// boolean serverActiveFlag = true;
	// static boolean portFlag = false;
	// static boolean verboseFlag = false;
	// static boolean dirPathFlag = false;
	boolean httpcFlag = false;
	boolean httpfsFlag = false;

	int cntFlag = 0;
	int port;
	String pathDirectory;
	// String crlf = "\r\n";
	String clientRequest; // client input
	String curlRequest;
	String body;

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	private Socket socket;
	private PrintWriter writer = null; // output stream send response to client
	private String inputRequest;
	httpfsModel model;
	int clientInstance;

	public httpfsServerThread(Socket serverClient, String pathDirectory1, int counter) {
		this.socket = serverClient;
		setClientInstance(counter);
		setPathDirectory(pathDirectory1);
		System.out.println("PAth in Constructor " + getPathDirectory());

	}

	public void run() {
		try {
			System.out.println("PAth in rum " + getPathDirectory());
			model = new httpfsModel();
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// output stream
			// OutputStream output = socket.getOutputStream();
			writer = new PrintWriter(socket.getOutputStream());
			while ((inputRequest = br.readLine()) != null) {
				System.out.println("Input Request in Run Method :  " + inputRequest);

				if (inputRequest.endsWith(Constants.HTTP_VERSION)) {
					setHttpcFlag(true);
					setCurlRequest(inputRequest);
					System.out.println("Curl Request in Run Method : "  + getCurlRequest());
					// httpcClientFlag = true;
				} else if (inputRequest.matches("(GET|POST)/(.*)")) {
					// httpFileClientFlag = true;
					setHttpfsFlag(true);
					setClientRequest(inputRequest);
					System.out.println("Client Request in Run Method : "  + getClientRequest());
				}
				if (isHttpfsFlag()) {
					model.addhttpfsHeaders(inputRequest);
					contentTypeFlag = inputRequest.startsWith(Constants.CONTENT_TYPE) ? true : contentTypeFlag;
					dispositionFlag = inputRequest.startsWith(Constants.CONTENT_DISPOSITION) ? true : dispositionFlag;
					body = inputRequest.startsWith(Constants.INLINE_DATA_CODE2) ? inputRequest.substring(2) : body;
					/*
					 * if (inputRequest.startsWith(Constants.CONTENT_TYPE)) contentTypeFlag = true;
					 * if (inputRequest.startsWith(Constants.CONTENT_DISPOSITION)) { dispositionFlag
					 * = true; }
					 * 
					 * if (inputRequest.startsWith(Constants.INLINE_DATA_CODE2)) {
					 * setBody(inputRequest.substring(2)); }
					 */
				}
				if (isHttpfsFlag() && inputRequest.isEmpty()) {
					break;
				}

				if (isHttpcFlag()) {
					System.out.println(inputRequest);
					if (inputRequest.matches("(.*):(.*)")) {
						if (cntFlag == 0) {
							String[] splitInput = inputRequest.split(":");
							model.addFileHeaders(splitInput[0].trim(), splitInput[1].trim());
							System.out.println("MOdel Header " + model.getFileHeaders());
						}
					}

					if (cntFlag == 1) {
						// String data = inputRequest;
						model.setBody(inputRequest);
						break;
					}
					if (inputRequest.isEmpty())
						cntFlag++;
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

	private void inspectClientFlag() {
		if (isHttpcFlag()) {
			if (getCurlRequest().matches("(GET|POST) /(.*)")) {
				curlRequest();
			}
		}
		if (isHttpfsFlag()) {
			System.out.println("Client Request : " + getClientRequest());
			if (getClientRequest().startsWith(Constants.GET_METHOD)) {
				getRequest(/* getClientRequest().substring(4) */);
			} else if (getClientRequest().startsWith(Constants.POST_METHOD)) {
				System.out.println(getClientRequest().substring(5));
				// String fileName = getClientRequest().substring(5);
				postRequest(/* getClientRequest().substring(5), getBody() */);
			}
		}
	}

	/**
	 * handles httpc Client.
	 */
	public synchronized void curlRequest() {
		setCurlRequest(getCurlRequest().replace("GET /", "").replace("POST /", "").replace("HTTP/1.1", ""));
		model.setStatusCode(Constants.HTTP_200);
		model.setUri("http://127.0.0.1:" + getPort() + "/" + getCurlRequest());
		writer.println(model.getHeaderPart());
		System.out.println("Header Part " + model.getHeaderPart());
		checkCurlOption();
	}

	private void checkCurlOption() {
		if (getCurlRequest().startsWith("get?")) {
			System.out.println("CURL : Request Type GET");
			// args
			setCurlRequest(getCurlRequest().replace("get?", ""));
			if (getCurlRequest().matches("(.*)&(.*)")) {
				String[] temp = getCurlRequest().split("&");
				for (int i = 0; i < temp.length; i++) {
					String[] args = temp[i].split("="); // need to change
					model.setParams(args[0], args[1]); // need to change
				}
			} else {
				String[] args = getCurlRequest().split("="); // need to change
				model.setParams(args[0], args[1]);// need to change
			}
			System.out.println(model.getGETBodyPart());
			writer.println(model.getGETBodyPart());

		} else if (getCurlRequest().startsWith("post?")) {
			System.out.println("CURL : Request Type POST");
			setCurlRequest(getCurlRequest().replace("post?", ""));
			if (!getCurlRequest().isEmpty() && getCurlRequest().matches("(.*)=(.*)")) {
				if (getCurlRequest().matches("(.*)&(.*)")) {
					String[] temp = getCurlRequest().split("&");
					for (int i = 0; i < temp.length; i++) {
						String[] args = temp[i].split("="); // need to change
						model.setParams(args[0], args[1]);// need to change
					}
				} else {
					String[] args = getCurlRequest().split("=");// need to change
					model.setParams(args[0], args[1]);// need to change
				}
			}
			writer.println(model.getPOSTBodyPart());
		}
	}

	/**
	 * "GET /" returns a list of the current files in the data directory.<br>
	 * "GET /foo" returns the content of the file named foo in the data
	 * directory.<br>
	 * 
	 * @param fileNam name of file.
	 * 
	 */
	public synchronized void getRequest(/* String fileName */) {
		// String fileName = getClientRequest().substring(4);
		// File filePath;
		// String fileName = fileName1;
		/*
		 * if (contentTypeFlag) { fileName = fileName + model.getFileContentHeader(); //
		 * filePath = new File(retrieveFilePath(fileName)); } else { // filePath = new
		 * File(retrieveFilePath(fileName)); }
		 */
		String fileName = isContentTypeFlag() ? getClientRequest().substring(4) + model.getFileContentHeader()
				: getClientRequest().substring(4);
		File filePath = new File(retrieveFilePath(fileName));
		System.out.println("File Name : " + getClientRequest().substring(4));
		System.out.println("Path : " + getPathDirectory());

		if (!fileName.contains("/")) {
			if (filePath.exists()) {
				if (filePath.isDirectory()) {
					// File[] listOfFiles = filePath.listFiles();
					HashMap<String, ArrayList<String>> output = new HashMap<>();
					output.put("Directory >> ", new ArrayList<String>());
					output.put("File      >> ", new ArrayList<String>());
					for (File file : filePath.listFiles()) {
						if (file.isDirectory()) {
							ArrayList<String> temp = output.get("Directory >> ");
							temp.add(file.getName());
							output.replace("Directory >> ", temp);
							// writer.println("Directory >> " + file.getName());
							// System.out.println("Directory >> " + file.getName());
						} else if (file.isFile()) {
							ArrayList<String> temp = output.get("File      >> ");
							temp.add(file.getName());
							output.replace("File      >> ", temp);
							// writer.println("File >> " + file.getName());
							// System.out.println("File >> " + file.getName());
						}
					}
					for (Entry<String, ArrayList<String>> entry : output.entrySet()) {
						ArrayList<String> temp = entry.getValue();
						for (int i = 0; i < temp.size(); i++) {
							writer.println(entry.getKey() + temp.get(i));
							System.out.println(entry.getKey() + temp.get(i));
						}
					}
				} else if (filePath.isFile()) {
					System.out.println("path: " + getPathDirectory() + "/" + fileName);
					FileReader fileReader;
					PrintWriter fileWriter = null;
					File downloadPath = new File(getPathDirectory() + "/Download");
					String fileDownloadName = new String();
					// boolean dispositionDirectory;
					if (dispositionFlag) {
						fileDownloadName = model.getFileDispositionHeader();
						if (model.dispositionAttachmentFlag && !downloadPath.exists()) {
							/* dispositionDirectory = */new File(getPathDirectory() + "/Download").mkdir();
						}

					}

					try {
						if (model.dispositionAttachmentFlag) {
							/*
							 * if (model.dispositionFileFlag) fileWriter = new PrintWriter(downloadPath +
							 * "/" + fileDownloadName); else fileWriter = new PrintWriter(downloadPath + "/"
							 * + fileName);
							 */
							fileWriter = model.dispositionFileFlag
									? new PrintWriter(downloadPath + "/" + fileDownloadName)
									: new PrintWriter(downloadPath + "/" + fileName);
						}
						fileReader = new FileReader(filePath);
						BufferedReader br = new BufferedReader(fileReader);
						String currentLine;
						String fileData = null;
						while ((currentLine = br.readLine()) != null) {
							fileData = fileData + currentLine;
							if (dispositionFlag) {
								if (model.dispositionInlineFlag) {
									writer.println(currentLine);
								} else if (model.dispositionAttachmentFlag) {
									fileWriter.println(currentLine);
								}
							} else
								writer.println(currentLine);
						}

						if (model.dispositionAttachmentFlag)
							fileWriter.close();
						writer.println("Operation Success");
						br.close();
					} catch (FileNotFoundException e) {
						System.out.println(Constants.HTTP_404_ERROR);
						writer.println(Constants.HTTP_404_ERROR + Constants.FILE_NOT_FOUND);
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			} else {
				System.out.println(Constants.HTTP_404_ERROR);
				writer.println(Constants.HTTP_404_ERROR);
			}
		} else {
			System.out.println(Constants.ACCESS_DENIED);
			writer.println("Error: " + Constants.ACCESS_DENIED);
		}
	}

	private String retrieveFilePath(String fileName) {
		return getPathDirectory() + "/" + fileName;
	}

	/**
	 * "POST /bar" should create or overwrite the file named bar in the data
	 * directory<br>
	 * with the content of the body of the request.<br>
	 * options for the POST such as overwrite=true|false.
	 * 
	 * @param fileName name of file.
	 * @param content
	 */

	public synchronized void postRequest(/* String fileName, String content */) {
		// File filePath;
		PrintWriter printWriter;
		// String fileName = getClientRequest().substring(5);
		// String content = getBody();
		/*
		 * if (contentTypeFlag) filePath = new File(getPathDirectory() + "/" + fileName
		 * + model.getFileContentHeader()); else filePath = new File(getPathDirectory()
		 * + "/" + fileName);
		 */

		if (!getClientRequest().substring(5).contains("/")) {
			try {
				File filePath = contentTypeFlag
						? new File(getPathDirectory() + "/" + getClientRequest().substring(5)
								+ model.getFileContentHeader())
						: new File(getPathDirectory() + "/" + getClientRequest().substring(5));

				printWriter = new PrintWriter(filePath);
				printWriter.println(getBody()/* content */);
				writer.println("Operation Completed Succcessfully!");
				printWriter.close();
			} catch (FileNotFoundException e) {
				writer.print(Constants.HTTP_404_ERROR);
			}
		} else {
			System.out.println(Constants.ACCESS_DENIED);
			writer.println("Error: " + Constants.ACCESS_DENIED);
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getPathDirectory() {
		return pathDirectory;
	}

	public void setPathDirectory(String pathDirectory) {
		this.pathDirectory = pathDirectory;
	}

	public String getCurlRequest() {
		return curlRequest;
	}

	public void setCurlRequest(String curlRequest) {
		this.curlRequest = curlRequest;
	}

	public int getClientInstance() {
		return clientInstance;
	}

	public void setClientInstance(int clientInstance) {
		this.clientInstance = clientInstance;
	}

	public boolean isHttpcFlag() {
		return httpcFlag;
	}

	public void setHttpcFlag(boolean httpcFlag) {
		this.httpcFlag = httpcFlag;
	}

	public boolean isHttpfsFlag() {
		return httpfsFlag;
	}

	public void setHttpfsFlag(boolean httpfsFlag) {
		this.httpfsFlag = httpfsFlag;
	}

	public String getClientRequest() {
		return clientRequest;
	}

	public void setClientRequest(String clientRequest) {
		this.clientRequest = clientRequest;
	}
}
