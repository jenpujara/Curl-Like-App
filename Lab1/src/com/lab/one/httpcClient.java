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
/* References
 * https://tools.ietf.org/html/rfc7231#section-7.1.2
 */
public class httpcClient {

	
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
				httpcLibrary library = new httpcLibrary(input);
				library.parseInput();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
}
