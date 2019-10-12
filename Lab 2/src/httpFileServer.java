import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Server class.
 * @author 
 *
 */
class httpFileServerThread implements Runnable {

	boolean contentTypeFlag = false;
	boolean dispositionFlag = false;
	//boolean serverActiveFlag = true;
	//static boolean portFlag = false;
	//static boolean verboseFlag = false;
	//static boolean dirPathFlag = false;
	boolean httpcClientFlag = false;
	boolean httpFileClientFlag = false;
	
	int cntFlag = 0;
	int portNo;
	String dirPath;
	//String crlf = "\r\n";
	String cRequest; // client input
	String curlRequest;
	String msgBody;
	
	private Socket socket;
	private BufferedReader in = null; // input stream to get request from Client
	private PrintWriter out = null; // output stream send response to client
	private String inputRequest;
	httpFileModel httpFileModelObj;
	int clientInstance;

	public httpFileServerThread(Socket serverClient, int counter, String dirPath2) {
		this.socket = serverClient;
		setClientInstance(counter);
		setDirPath(dirPath2);			
	}
	
	public int getPortNo() {
		return portNo;
	}

	public void setPortNo(int portNo) {
		this.portNo = portNo;
	}

	public String getDirPath() {
		return dirPath;
	}

	public void setDirPath(String dirPath) {
		this.dirPath = dirPath;
	}

	public String getcRequest() {
		return cRequest;
	}

	public void setcRequest(String cRequest) {
		this.cRequest = cRequest;
	}

	public String getCurlRequest() {
		return curlRequest;
	}

	public void setCurlRequest(String curlRequest) {
		this.curlRequest = curlRequest;
	}

	public String getMsgBody() {
		return msgBody;
	}

	public void setMsgBody(String msgBody) {
		this.msgBody = msgBody;
	}

	public int getClientInstance() {
		return clientInstance;
	}

	public void setClientInstance(int clientInstance) {
		this.clientInstance = clientInstance;
	}
	
	public void run(){
		try
		{	
			httpFileModelObj = new httpFileModel();
			
			//get the data from client    
			InputStream input = socket.getInputStream();
			in = new BufferedReader(new InputStreamReader(input));
			//output stream
			OutputStream output= socket.getOutputStream();
			out = new PrintWriter(output);
			while((inputRequest = in.readLine())!=null) {
				if(inputRequest.endsWith(Constants.HTTP_VERSION)) {
					setCurlRequest(inputRequest);
					httpcClientFlag = true;
				}
				else if(inputRequest.matches("(GET|POST)/(.*)")) {
					httpFileClientFlag = true;
					setcRequest(inputRequest);
				}		
				
				if(httpFileClientFlag) {
					httpFileModelObj.addhttpfsHeaders(inputRequest);
					if(inputRequest.startsWith(Constants.CONTENT_TYPE))
						contentTypeFlag = true;
					if(inputRequest.startsWith(Constants.CONTENT_DISPOSITION)) {
						dispositionFlag = true;
					}
					if(inputRequest.startsWith(Constants.INLINE_DATA_CODE2)) {
						setMsgBody(inputRequest.substring(2));
					}
				}
				if(httpFileClientFlag && inputRequest.isEmpty()) {
					break;
				}
			
				if(httpcClientFlag) {
					System.out.println(inputRequest);
					if(inputRequest.matches("(.*):(.*)")&&cntFlag==0){
						String[] headers = inputRequest.split(":");
						httpFileModelObj.addFileHeaders(headers[0], headers[1]);
					}
					
					if(cntFlag==1) {
						String data = inputRequest;
						httpFileModelObj.setBody(data);
						break;
					}
					if(inputRequest.isEmpty())
						cntFlag++;
				}	
			}
			inspectClientFlag();			
			out.println("");
			out.flush();
			in.close();
			socket.close();
			

		}catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void inspectClientFlag() {
		if(httpcClientFlag) {
			if(getCurlRequest().matches("(GET|POST) /(.*)")) {
				curlRequest();
			}
		}
		
		if(httpFileClientFlag) {
			System.out.println("Command Requested by Client: "+getcRequest());

			if(getcRequest().startsWith(Constants.GET_METHOD)) {
				get_Server_Request(getcRequest().substring(4));
			}else if(getcRequest().startsWith(Constants.POST_METHOD)) {
				System.out.println(getcRequest().substring(5));
				String fileName = getcRequest().substring(5);
				post_Server_Request(fileName, getMsgBody());
			}
		}
	}

	/**
	 * handles httpc Client.
	 */
	public synchronized void curlRequest() {
		setCurlRequest(getCurlRequest().replace("GET /", "").replace("POST /", "").replace("HTTP/1.1", ""));
		httpFileModelObj.setStatusCode(Constants.STATUS_CODE_1);
		httpFileModelObj.setUri("http://localhost:"+getPortNo()+"/"+getCurlRequest());
		out.println(httpFileModelObj.getHeaderPart());
		checkCurlOption();
	}
	
	
	private void checkCurlOption() {
		if(getCurlRequest().startsWith("get?")) {
			System.out.println("curl GET Request...");
			//args
			setCurlRequest(getCurlRequest().replace("get?", ""));
			if(getCurlRequest().matches("(.*)&(.*)")) {
				String[] temp = getCurlRequest().split("&");
				for(int i = 0;i<temp.length;i++) {
					String[] args = temp[i].split("=");
					httpFileModelObj.setParams(args[0], args[1]);
				}
			}else {
				String[] args = getCurlRequest().split("=");
				httpFileModelObj.setParams(args[0], args[1]);
			}
			System.out.println(httpFileModelObj.getGETBodyPart());
			out.println(httpFileModelObj.getGETBodyPart());
			
		}else if(getCurlRequest().startsWith("post?")) {
			System.out.println("curl POST Request...");
			setCurlRequest(getCurlRequest().replace("post?", ""));
			if(!getCurlRequest().isEmpty() && getCurlRequest().matches("(.*)=(.*)")) {
				if(getCurlRequest().matches("(.*)&(.*)")) {
					String[] temp = getCurlRequest().split("&");
					for(int i = 0;i<temp.length;i++) {
						String[] args = temp[i].split("=");
						httpFileModelObj.setParams(args[0], args[1]);
					}
				}else {
					String[] args = getCurlRequest().split("=");
					httpFileModelObj.setParams(args[0], args[1]);
				}
			}
			out.println(httpFileModelObj.getPOSTBodyPart());
		}
	}

	/**
	 * "GET /" returns a list of the current files in the data directory.<br>
	 * "GET /foo" returns the content of the file named foo in the data directory.<br>
	 * 
	 * @param fileNam name of file.
	 * 
	 */
	public synchronized void get_Server_Request(String fileName) {
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException e1) {
//			// TODO Auto-generated catch block 
//			e1.printStackTrace();
//		}
		File filePath;
		//String fileName = fileName1;
		if(contentTypeFlag) {
			fileName = fileName+httpFileModelObj.getFileContentHeader();
			filePath = new File(retrieveFilePath(fileName));
		}else {
			filePath = new File(retrieveFilePath(fileName));
		}
		
		if(!fileName.contains("/")) {
			
		
		if(filePath.exists()) {
			if(filePath.isDirectory()) {	
				File[] listOfFiles = filePath.listFiles();
				for(File file : listOfFiles) {
					if(file.isFile()) {
						System.out.println("File      >> "+file.getName());
						out.println("File      >> "+file.getName());
					}else if(file.isDirectory()) {
						System.out.println("Directory >> "+file.getName());
						out.println("Directory >> "+file.getName());
					}
				}
			}else if(filePath.isFile()) {
				System.out.println("path: "+getDirPath()+"/"+fileName);
				FileReader fileReader;
				PrintWriter fileWriter = null;
				File downloadPath = new File(getDirPath()+"/Download");
				String fileDownloadName = new String();
				boolean dispositionDirectory;
				if(dispositionFlag) {
					fileDownloadName = httpFileModelObj.getFileDispositionHeader();
					if(httpFileModelObj.dispositionAttachmentFlag) {
						if(!downloadPath.exists())
							dispositionDirectory = new File(getDirPath()+"/Download").mkdir();
					}
				}
				
				try {
					
					if(httpFileModelObj.dispositionAttachmentFlag) {
						if(httpFileModelObj.dispositionFileFlag) 
							fileWriter = new PrintWriter(downloadPath+"/"+fileDownloadName);
						else
							fileWriter = new PrintWriter(downloadPath+"/"+fileName);
					}	
					fileReader = new FileReader(filePath);
					BufferedReader bufferedReader = new BufferedReader(fileReader);
					String currentLine;
					String fileData = null;
					while ((currentLine = bufferedReader.readLine()) != null) {
						fileData = fileData + currentLine;
						if(dispositionFlag) {
							
							if(httpFileModelObj.dispositionInlineFlag) {
								out.println(currentLine);
							}else if(httpFileModelObj.dispositionAttachmentFlag) {
								fileWriter.println(currentLine);
							}
						}else 
							out.println(currentLine);
					}
					if(httpFileModelObj.dispositionAttachmentFlag)
						fileWriter.close();
					out.println("Operation Success");
				} catch (FileNotFoundException e) {
					System.out.println(Constants.NOT_FOUND_ERROR);
					out.println(Constants.NOT_FOUND_ERROR + ": File Not Found");
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		} else {
			System.out.println(Constants.NOT_FOUND_ERROR);
			out.println(Constants.NOT_FOUND_ERROR);
		}
		}else {
			System.out.println(Constants.ACCESS_DENIAL);
			out.println("Error: " + Constants.ACCESS_DENIAL);
		}
	}
	
	

	private String retrieveFilePath(String fileName) {
		return getDirPath()+"/"+fileName;
	}

	/**
	 * "POST /bar" should create or overwrite the file named bar in the data directory<br>
	 * with the content of the body of the request.<br>
	 * options for the POST such as overwrite=true|false.
	 * 
	 * @param fileName name of file. 
	 * @param content 
	 */
	public synchronized void post_Server_Request(String fileName, String content) {
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}
		File filePath;
		PrintWriter postWriter;
		if(contentTypeFlag) 
			filePath = new File(getDirPath()+"/"+fileName+httpFileModelObj.getFileContentHeader());
		else
			filePath = new File(getDirPath()+"/"+fileName);
		
		if(!fileName.contains("/")) {
			try {
				postWriter = new PrintWriter(filePath);
				postWriter.println(content);
				out.println("Operation Completed Succcessfully!");
				postWriter.close();
			} catch (FileNotFoundException e) {
				out.print(Constants.NOT_FOUND_ERROR);
			}
		}else {
			System.out.println(Constants.ACCESS_DENIAL);
			out.println("Error: " + Constants.ACCESS_DENIAL);
		}
	}

}

/**
 * Server class.	
 *
 */
public class httpFileServer{	
	
	static boolean portFlag = false;
	static int portNo;
	static String dirPath;
	private static boolean verboseFlag = false;;
	/**
	 * main method used to create a Server with port Number 5555.
	 * @param args args.
	 * @throws IOException Input-Output Exception.
	 */
	public static void main(String args[]) throws IOException {
		InputStreamReader inputReader = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(inputReader);
		String serverInput = br.readLine();
		String[] commands = serverInput.split(" ");
		for(int i = 0; i<commands.length; i++) {
			if(commands[i].equals(Constants.SERVER_PORT_CODE)){
				portFlag = true;
				portNo = Integer.parseInt(commands[++i]);
			}
			if(commands[i].equals(Constants.DIR_PATH_CODE)) {
				dirPath = commands[++i];
			}else {
				dirPath = "D:/Jenny/Concordia/COMP 6461(Computer Networks)/Server_Data";
			}	
			if(commands[i].equals(Constants.PRINT_DEBUG_MSG)) {
				verboseFlag  = true;
			}
		}
		if(!portFlag) {
			portNo = 8080;
		}
		ServerSocket serverSocket = new ServerSocket(portNo);
		int counter = 0;
		if(verboseFlag) {
			System.out.println("Server Started...");
			System.out.println("Server listens to port: " + portNo);
		}
		while(true) {
			counter++;
			Socket serverClient = serverSocket.accept();
			if(verboseFlag)
				System.out.println(">> Client "+counter+" : Connection Established");
			httpFileServerThread hst = new httpFileServerThread(serverClient,counter,dirPath);
			Thread t= new Thread(hst);
			t.start();
		}
	}
}