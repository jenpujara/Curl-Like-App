import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Model Class
 * @author Himen Sidhpura
 * @author Jenny Mistry
 *
 */
public class httpfsModel {
	// int count = 0;
	boolean dispositionInlineFlag = false;
	boolean dispositionAttachmentFlag = false;
	boolean dispositionFileFlag = false;
	HashMap<String, String> headers;
	HashMap<String, String> parameters;
	ArrayList<String> filesList;
	String statusCode;
	String body = "";
	// String origin = "127.0.0.1";
	String uri;
	// String space = " ";
	ArrayList<String> fileServerHeader;
		
/**
 * default constructor
 */
	public httpfsModel() {
		Instant instant = Instant.now();
		fileServerHeader = new ArrayList<>();
		headers = new HashMap<>();
		parameters = new HashMap<>();
		filesList = new ArrayList<>();
		headers.put("Connection", "keep-alive");
		headers.put("Host", Constants.IP_ADDRESS);
		headers.put("Date", instant.toString());
	}

	/**
	 * method to add http file header
	 * @param header
	 */
	public void addhttpfsHeaders(String header) {
		fileServerHeader.add(header);
	}
	
	/**
	 * method to get the file header
	 * @return
	 */
	public String getFileContentHeader() {
		String extension = "";
		for (int i = 0; i < fileServerHeader.size(); i++) {
			if (fileServerHeader.get(i).startsWith(Constants.CONTENT_TYPE)) {
				String[] temp = fileServerHeader.get(i).split(":");
				extension = getExtension(temp[1]);
			}
		}
		return extension;
	}
	
	/**
	 * getter method to fetch the extension of the file
	 * @param str
	 * @return ext
	 */
	private String getExtension(String str) {
		String ext = "";
		if (str.equals("application/text"))
			ext = ".txt";
		else if (str.equals("application/json"))
			ext = ".json";
		return ext;
	}
	
	/**
	 * getter method to retrieve the header of disposition for the file
	 * @return fileName
	 */
	public String getFileDispositionHeader() {
		String fileName = "";
		for (int i = 0; i < fileServerHeader.size(); i++) {
			if (fileServerHeader.get(i).startsWith(Constants.CONTENT_DISPOSITION)) {
				String[] temp1 = fileServerHeader.get(i).split(";");
				String[] temp2 = temp1[0].split(":");
				if (temp2[1].equals("inline")) {
					dispositionInlineFlag = true;
				} else if (temp2[1].equals("attachment")) {
					dispositionAttachmentFlag = true;
					if (temp1.length == 2) {
						String temp3[] = temp1[1].split(":");
						fileName = temp3[1];
						dispositionFileFlag = true;
					}
				}
			}
		}
		return fileName;
	}
	/**
	 * method to fetch the header
	 * @return head
	 */
	public String getHeaders() {
		String head = "";
		for (Entry<String, String> entry : headers.entrySet()) {
			head += " " + entry.getKey() + ": " + entry.getValue() + "\r\n";
		}
		return head;

	}

	/**
	 * method to set the header
	 * @param headers
	 */
	public void setHeaders(HashMap<String, String> headers) {
		this.headers = headers;
	}
	
	/**
	 * this method is used to add the headers
	 * @param values
	 */
	public void addHeaders(String values/* String key, String value */) {
		String[] value = values.trim().split(":");
		headers.put(value[0].trim(), value[1].trim());
	}
	/*
	 * public void addFileHeaders(String key, String value) { headerMap.put(key,
	 * value); }
	 * 
	 * public String getFileHeaders() { String head = ""; for(Entry<String, String>
	 * entry : headerMap.entrySet()) { head +=
	 * " "+entry.getKey()+": "+entry.getValue()+"\r\n"; } return head; }
	 */
	
	/**
	 * setter method to set the status code
	 * @param status
	 */
	public void setStatusCode(String status) {
		this.statusCode = status;
	}
	
	/**
	 * getter method to get the status code
	 * @return statusCode
	 */
	public String getStatusCode() {
		return this.statusCode;
	}

	/**
	 * getter method to get the state of connection
	 * @return state
	 */
	public String getConnectionState() {
		if (statusCode.equals(Constants.HTTP_200))
			return "OK";
		else if (statusCode.equals(Constants.HTTP_400))
			return "Bad Request";
		else if (statusCode.equals(Constants.HTTP_404))
			return "Not Found";
		else
			return "ERROR HTTP";
	}
	
	/**
	 * getter method to get the parameters
	 * @return
	 */
	public String getParameters() {
		String head = "\r\n";
		for (Entry<String, String> entry : parameters.entrySet()) {
			head += " \"" + entry.getKey() + "\": \"" + entry.getValue() + "\",\r\n";
		}
		return head;
	}
	
	/**
	 * setter method to set the parameters
	 * @param values
	 */
	public void setParameters(String values/* String key, String value */) {
		String[] value = values.trim().split("=");
		parameters.put(value[0].trim(), value[1].trim());
	}

	/*
	 * public void setParams(String key, String value) { parameters.put(key, value);
	 * }
	 * 
	 * public String getParams() { String head = "\r\n"; for (Entry<String, String>
	 * entry : parameters.entrySet()) { head += " \"" + entry.getKey() + "\": \"" +
	 * entry.getValue() + "\",\r\n"; } return head; }
	 */

	// public String getOrigin() {
	// return origin;
	// }
	//
	/**
	 * setter method to set the Uri from the request
	 * @param uri
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	/**
	 * getter method to get the Uri from the request
	 * @return uri
	 */
	public String getUri() {
		return this.uri;
	}
	
	/**
	 * setter method to set the body
	 * @param body
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * getter method to get the body
	 * @param body
	 */
	public String getBody() {
		return this.body;
	}

	/**
	 * setter method to set the file name in the list of files
	 * @param fileName
	 */
	public void setFiles(String fileName) {
		filesList.add(fileName);
	}
	
	/**
	 * getter method to get the file name from the list of files
	 * @return listOfFiles
	 */
	public String getFiles() {
		String listOfFiles = "";
		for (String file : filesList) {
			listOfFiles += file + ",";
		}
		return listOfFiles;
	}
	
	/**
	 * method to get the header part from the request
	 * @return header
	 */
	public String getHeaderPart() {
		return "HTTP/1.0 " + getStatusCode() + " " + getConnectionState() + "\r\n" + getHeaders();
	}
	
	/**
	 * method used to fetch the body of the request of type GET
	 * @return GET body
	 */
	public String getGETBody() {
		return "{\r\n" + " \"args\":{" + getParameters() + "},\r\n" + " \"headers\":{\r\n" + getHeaders() + "},\r\n"
				+ " \"origin\": " + Constants.ORIGIN + ",\r\n" + " \"url\": " + getUri() + ",\r\n" + "}";
	}

	/**
	 * method used to fetch the body of the request of type POST
	 * @return POST body
	 */
	public String getPOSTBody() {
		return "{\r\n" + " " + "\"args\":{" + " " + getParameters() + "},\r\n" + " " + "\"data\":{" + " " + getBody()
				+ "},\r\n" + " " + "\"files\":{\r\n" + " " + this.getFiles() + "},\r\n" + " " + "\"headers\":{\r\n"
				+ getHeaders() + " },\r\n" + " " + "\"json\": { },\r\n" + " " + "\"origin\": " + Constants.ORIGIN
				+ ",\r\n" + " " + "\"url\": " + this.getUri() + ",\r\n" + "}";
	}
}