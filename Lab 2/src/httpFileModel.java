import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class httpFileModel {	
	//int count = 0;
	boolean dispositionInlineFlag = false;
	boolean dispositionAttachmentFlag = false;
	boolean dispositionFileFlag = false;
	HashMap<String, String> headerMap;
	HashMap<String, String> paramMap;
	ArrayList<String> filesList;
	String statusCode;
	String body = "";
//	String origin = "127.0.0.1"; 
	String uri;
//	String space = " ";
	ArrayList<String> fileServerHeader;
	
	public httpFileModel(){
		fileServerHeader = new ArrayList<>();
		headerMap = new HashMap<>();
		paramMap = new HashMap<>();
		filesList = new ArrayList<>();
		headerMap.put("Connection", "keep-alive");
		headerMap.put("Host", "Localhost");
		Instant instant = Instant.now();
		headerMap.put("Date", instant.toString());
	}
	
	public void addhttpfsHeaders(String header) {
		fileServerHeader.add(header);
	}
	
	public String getFileContentHeader() {	
		String extension = new String();
		for(int i = 0; i<fileServerHeader.size(); i++) {
			if(fileServerHeader.get(i).startsWith(Constants.CONTENT_TYPE)) {
				String[] temp = fileServerHeader.get(i).split(":");
				extension=getExtension(temp[1]);				
			}
		}
		return extension;
	}
	
	
	
	private String getExtension(String str) {
		String ext="";
		if(str.equals("application/text"))
			ext = ".txt";
		else if(str.equals("application/json"))
			ext = ".json";
		return ext;
	}

	public String getFileDispositionHeader() {
		String fileName = "";
		for(int i = 0; i<fileServerHeader.size(); i++) {
			if(fileServerHeader.get(i).startsWith(Constants.CONTENT_DISPOSITION)) {
				String[] temp1 = fileServerHeader.get(i).split(";");
				String[] temp2 = temp1[0].split(":");
				if(temp2[1].equals("inline")) {
					dispositionInlineFlag = true;
				}else if(temp2[1].equals("attachment")) {
					dispositionAttachmentFlag = true;
					if(temp1.length == 2) {
						String temp3[] = temp1[1].split(":");
						fileName = temp3[1];
						dispositionFileFlag = true;
					}
				}				
			}
		}
		return fileName;
	}
	
	public void addFileHeaders(String key, String value) {
		headerMap.put(key, value);
	}
	
	public String getFileHeaders() {
		String head = "";
		for(Entry<String, String> entry : headerMap.entrySet()) {
			head += " "+entry.getKey()+": "+entry.getValue()+"\r\n";
		}
		return head;
	}
	
	public void setStatusCode(String status) {
		this.statusCode = status;
	}
	
	public String getStatusCode() {
		return this.statusCode;
	}
	
	public String getConnectionState() {
		String state="";
		if(statusCode.equals(Constants.STATUS_CODE_1)) 
			state = "OK";
		else if(statusCode.equals(Constants.STATUS_CODE_2))
			state = "Bad Request";
		else if(statusCode.equals(Constants.STATUS_CODE_3))
			state = "Not Found";
		else 
			state = "ERROR HTTP";
		
		return state;
	}
	
	public void setParams(String key, String value) {
		paramMap.put(key, value);
	}
	
	public String getParams() {
		String head = "\r\n";
		for(Entry<String, String> entry : paramMap.entrySet()) {
			head += " \""+entry.getKey()+"\": \""+entry.getValue()+"\",\r\n";
		}
		return head;
	}
	
//	public String getOrigin() {
//		return origin;
//	}
//	
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String getUri() {
		return this.uri;
	}
	
	public void setBody(String body) {
		this.body = body;
	}
	
	public String getBody() {
		return this.body;
	}
	
	public void setFiles(String fileName) {
		filesList.add(fileName);
	}
	
	public String getFiles() {
		String listOfFiles = "";
		for(String file : filesList) {
			listOfFiles += file+",";
		}
		return listOfFiles;
	}
	
	public String getHeaderPart() {
		return "HTTP/1.0 " + getStatusCode() + " " + getConnectionState() +"\r\n"+ getFileHeaders();
	}
	
	public String getGETBodyPart() {
		return 
				"{\r\n"+
				" \"args\":{"+
				getParams()+"},\r\n"+
				" \"headers\":{\r\n"+
				getFileHeaders()+"},\r\n"+
				" \"origin\": "+Constants.ORIGIN+",\r\n"+
				" \"url\": "+getUri()+",\r\n"+
				"}";
	}
	
	public String getPOSTBodyPart() {
		return 
				"{\r\n"+" "+
				"\"args\":{"+" "+
				getParams()+"},\r\n"+" "+
				"\"data\":{"+" "+
				getBody()+"},\r\n"+" "+
				"\"files\":{\r\n"+" "+
				this.getFiles()+"},\r\n"+" "+
				"\"headers\":{\r\n"+
				getFileHeaders()+" },\r\n"+" "+
				"\"json\": { },\r\n"+" "+
				"\"origin\": "+Constants.ORIGIN+",\r\n"+" "+
				"\"url\": "+this.getUri()+",\r\n"+
				"}";
	}
}