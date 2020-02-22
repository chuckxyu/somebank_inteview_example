package com.test.nubank;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.test.nubank.client.Client;

/*
 * Class Authorizer
 * 
 * It's a class which main method uses a a scanner to read stdin lines
 * and a Client instance to validate operations and persis them on and In-Memory DB
 */
public class Authorizer {

	public Authorizer() {
	}

	public static void main(String[] args) {
		// Client creation and the clearence of the memory
		Client clHz = new Client();
		Scanner scanner = new Scanner(System.in);
		String input = null;

		clHz.getMap().evictAll();

		do {
			try {
				// read the input line
				input = scanner.nextLine();
				System.out.println(clHz.setOperation(input));
			} catch (JSONException jsonE) {
				System.out.println("*ERROR: Not a json");
			} catch (NullPointerException npE) {
				System.out.println("*ERROR: Null Pointer");
			}
		} while (scanner.hasNext());// to keep reading until crtl + c
		scanner.close();

	}

	/*
	 * Main Test
	 * 
	 * Used to emulate the creation of an account with N availableLimit, 
	 * and N valid transactions with amount = 1
	 * 
	 * True if map size = (N * 2)+ 1
	 * 
	 * That is the number of transactions and state changes plus the creation of the account
	 */
	@Test
	public void testMain() throws IOException {
		int nTx = 4000;
		Client clHz = new Client();
		clHz.getMap().evictAll();
		String acc = "{ \"account\": { \"activeCard\": true, \"availableLimit\": " + nTx + " } }";
		JSONObject rootTx = new JSONObject(
				"{ \"transaction\": { \"merchant\": \"Burger King\", \"amount\": 1, \"time\": \"2019-02-13T10:00:00.000Z\" } }");
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		LocalDateTime dateOperation = LocalDateTime.parse(rootTx.getJSONObject("transaction").get("time").toString(),
				format);

		System.out.println(clHz.setOperation(acc));
		for (int i = 0; i < nTx; i++) {
			dateOperation = dateOperation.plusMinutes(3);
			rootTx.getJSONObject("transaction").put("time", dateOperation.format(format));
			System.out.println(clHz.setOperation(rootTx.toString()));
		}
		assertTrue(clHz.getMap().size() == (nTx * 2) + 1);

	}
}
