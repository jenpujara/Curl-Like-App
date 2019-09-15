package com.lab.one;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

public class httpcMain {
	public static void main(String[] args) {
		while (true) {
			System.out.print(">> ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String input = "";
			try {
				input = br.readLine();
				if(input.equalsIgnoreCase("exit")) {
					break;
				}
				httpcImpl httpcImpl = new httpcImpl(input);
				httpcImpl.parseInput();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}
}
