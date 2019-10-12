import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Client class
 * @author Himen Sidhpura
 * @author Jenny Mistry
 *
 */
public class httpFileClient {
	
	static boolean headerFlag = false;
	static boolean bodyFlag = false;
	static String body;
	private Socket socket;
	private PrintWriter pwr;
	static String uri;
	int portNo;
	static String queryString;
	static ArrayList<String> headerList = new ArrayList<>();
	
	public static String getBody() {
		return body;
	}

	public static void setBody(String body) {
		httpFileClient.body = body;
	}

	public static String getUri() {
		return uri;
	}

	public static void setUri(String uri) {
		httpFileClient.uri = uri;
	}

	public int getPortNo() {
		return portNo;
	}

	public void setPortNo(int portNo) {
		this.portNo = portNo;
	}

	public static String getQueryString() {
		return queryString;
	}

	public static void setQueryString(String queryString) {
		httpFileClient.queryString = queryString;
	}
	
	public httpFileClient(String host, int port, String queryStr,String body, ArrayList<String> headers) {
		
		try 
		{	
			setBody(body);
			setQueryString(queryStr);
			headerList = headers;
			socket = new Socket(host, port);
			System.out.println("Server Connected...");
			
		} catch (IOException e) {
			System.out.println("");
			System.out.println(Constants.NOT_FOUND_ERROR + " : Host Not Found");
		}
	}	

	public void sendRequest(){
		try {
		pwr= new PrintWriter(socket.getOutputStream());
		pwr.println(getQueryString());
		
		if(headerFlag) {
			for(int i = 0 ; i<headerList.size();i++) {
				pwr.println(headerList.get(i));
			}
		}
		if(bodyFlag) {
			pwr.println(Constants.DIR_PATH_CODE + getBody());
		}
		
		pwr.println("\r\n");
		pwr.flush();
		//displayResult();
		try {
			InputStream input = socket.getInputStream();
			InputStreamReader inreader = new InputStreamReader(input);
			BufferedReader br = new BufferedReader(inreader);
			String output;
			while((output = br.readLine()) != null) {
				System.out.println(output);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		pwr.close();
		socket.close();
	}
		catch(IOException io) {
			io.printStackTrace();
		}
		
	}

//	public void displayResult() {
//		try {
//		    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//			String output;
//			while((output = br.readLine()) != null) {
//			System.out.println(output);
//		}
//	
//} 	catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//	}
	
	
	public static void main(String args[]) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String httpFileClient;
		try {
			httpFileClient = br.readLine();
			String[] commandClient = httpFileClient.split(" ");
			if(commandClient[0].equals("httpfs")) {
				for(int i =0; i<commandClient.length; i++) {					
					if(commandClient[i].equals(Constants.HEADER_CODE)) {
						headerFlag = true;
						headerList.add(commandClient[++i]);
					}
					if(commandClient[i].startsWith("http://")){
						uri = commandClient[i];
					}
					if(commandClient[i].startsWith(Constants.INLINE_DATA_CODE2)) {
						bodyFlag = true;
						body = commandClient[++i];
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		URI url;
		try {
			url = new URI(uri);
			String hostName = url.getHost();
			int portNo = url.getPort();
			queryString = url.getPath();
			System.out.println(queryString.substring(1));
			httpFileClient client2 = new httpFileClient(hostName,portNo,queryString.substring(1),body, headerList);
			client2.sendRequest();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		}	
	
}