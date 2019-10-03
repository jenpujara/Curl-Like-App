package com.lab.one;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
