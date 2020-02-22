package com.test.nubank.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;

import org.json.JSONObject;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/*
 * Class Client
 * 
 * Client to perform operations to hazelcast backend
 * 
 */
public class Client {
	// client
	private HazelcastInstance hzClient;
	// sorted map
	private TreeMap<Long, String> sorted = new TreeMap<Long, String>(new Comparator<Long>() {
		@Override
		public int compare(Long o1, Long o2) {
			return o2.compareTo(o1);
		}
	});
	// a constant datetime format
	DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	public Client() {
		// Init according xml config
		ClientConfig clientConfig = new XmlClientConfigBuilder().build();
		hzClient = HazelcastClient.newHazelcastClient(clientConfig);
	}

	/*
	 * Getter for sorted map
	 */
	public TreeMap<Long, String> getSortedMap() {
		sorted.putAll(getHzClient().getMap("default"));
		return sorted;
	}
	/*
	 * Getter for original hazelcast map
	 */

	public IMap<Long, String> getMap() {
		return getHzClient().getMap("default");
	}

	/*
	 * Print map on console
	 */
	public void printMap() {
		getMap().entrySet().stream().forEach(System.out::println);
	}

	/*
	 * Collector to get last N elements
	 */
	public static <T> Collector<T, ?, List<T>> lastN(int n) {
		return Collector.<T, Deque<T>, List<T>>of(ArrayDeque::new, (acc, t) -> {
			if (acc.size() == n)
				acc.pollFirst();
			acc.add(t);
		}, (acc1, acc2) -> {
			while (acc2.size() < n && !acc1.isEmpty()) {
				acc2.addFirst(acc1.pollLast());
			}
			return acc2;
		}, ArrayList::new);
	}

	/*
	 * Method for set an operation.
	 * 
	 * identifies account and transaction ops and persists if its permitted
	 */
	@SuppressWarnings("deprecation")
	public String setOperation(String op) {
		JSONObject jsonObject = new JSONObject(op);
		for (String entry : JSONObject.getNames(jsonObject)) {
			if (entry.equalsIgnoreCase("account")) {
				jsonObject = validCreation(jsonObject);
			} else if (entry.equalsIgnoreCase("transaction")) {
				jsonObject = validTransaction(jsonObject);
			}
		}
		if (jsonObject.getJSONArray("violations").isEmpty()) {
			getMap().set(getHzClient().getIdGenerator("newid").newId(), jsonObject.toString());
			if (op.contains("transaction")) {
				getMap().set(getHzClient().getIdGenerator("newid").newId(), op);
			}
		}
		return jsonObject.toString();
	}

	/*
	 * Gets last state of account
	 */
	private String getAccount() {
		TreeMap<Long, String> sortMap = getSortedMap();
		for (Long k : sortMap.keySet()) {
			StringBuilder sb = new StringBuilder(sortMap.get(k));
			if (sb.toString().contains("account")) {
				return sb.toString();
			}
		}
		return null;
	}

	/*
	 * Validates the timing of the last moves against the one being authorized
	 */
	private Boolean validTiming(List<Object> lastMoves, JSONObject jsonOp) {
		Boolean status = true;
		AtomicInteger atomicInteger = new AtomicInteger(1);
		LocalDateTime dateOperation = LocalDateTime.parse(jsonOp.getJSONObject("transaction").get("time").toString(),
				format);
		lastMoves.forEach((lm) -> {
			JSONObject mv = new JSONObject(getMap().get(lm).toString());

			LocalDateTime dateMove = LocalDateTime.parse(mv.getJSONObject("transaction").get("time").toString(),
					format);
			long diff = ChronoUnit.MINUTES.between(dateMove, dateOperation);

			if (diff <= 3) {
				atomicInteger.getAndIncrement();
			}

		});

		if (atomicInteger.get() >= 3) {
			status = false;
		}

		return status;
	}

	/*
	 * Validates the similarity of the last moves against the one being authorized
	 */
	private Boolean validSimilarity(List<Object> lastMoves, JSONObject jsonOp) {
		Boolean status = true;
		AtomicInteger atomicInteger = new AtomicInteger(1);
		LocalDateTime dateOperation = LocalDateTime.parse(jsonOp.getJSONObject("transaction").get("time").toString(),
				format);

		lastMoves.forEach((lm) -> {
			JSONObject mv = new JSONObject(getMap().get(lm).toString());
			LocalDateTime dateMove = LocalDateTime.parse(mv.getJSONObject("transaction").get("time").toString(),
					format);
			long diff = ChronoUnit.MINUTES.between(dateMove, dateOperation);

			if (mv.getJSONObject("transaction").get("amount").equals(jsonOp.getJSONObject("transaction").get("amount"))
					&& diff <= 2) {
				atomicInteger.getAndIncrement();
			}

		});

		if (atomicInteger.get() >= 2) {
			status = false;
		}
		return status;
	}

	/*
	 * Validates the existence of the account to be authorized
	 */
	private JSONObject validCreation(JSONObject jsonObject) {
		ArrayList<String> err = new ArrayList<String>();
		if (getAccount() != null) {
			err.add("account-already-initialized");
		}
		return jsonObject.put("violations", err);
	}

	/*
	 * Validates the transaction to be authorized
	 */
	private JSONObject validTransaction(JSONObject jsonOp) {
		ArrayList<String> err = new ArrayList<String>();
		String account = getAccount();

		JSONObject jsonAcc = null;
		if (account != null) {
			TreeMap<Long, String> sortMap = getSortedMap();
			jsonAcc = new JSONObject(account);

			if (!Boolean.valueOf(jsonAcc.getJSONObject("account").get("activeCard").toString())) {
				err.add("c​ard-blocked");
			}
			if (Double.valueOf(jsonOp.getJSONObject("transaction").get("amount").toString()) > Double
					.valueOf(jsonAcc.getJSONObject("account").get("availableLimit").toString())) {
				err.add("insufficient-limit");
			}

			List<Object> lastMoves = sortMap.keySet().stream().filter(lm -> sortMap.get(lm).contains("transaction"))
					.collect(lastN(3));

			List<Object> lastMovesSim = sortMap.keySet().stream()
					.filter(lm -> (sortMap.get(lm).contains("transaction") && sortMap.get(lm)
							.contains(jsonOp.getJSONObject("transaction").get("merchant").toString())))
					.collect(lastN(2));

			if (!validTiming(lastMoves, jsonOp)) {
				err.add("high-frequency-small-interval");
			}
			if (!validSimilarity(lastMovesSim, jsonOp)) {
				err.add("doubled-transaction");
			}

			if (err.isEmpty()) {
				jsonAcc.getJSONObject("account").put("availableLimit",
						Double.valueOf(jsonAcc.getJSONObject("account").get("availableLimit").toString())
								- Double.valueOf(jsonOp.getJSONObject("transaction").get("amount").toString()));
			}
			jsonAcc.put("violations", err);
		}
		return jsonAcc;

	}
	/*
	 * Getter for the hazelcast client
	 */
	private HazelcastInstance getHzClient() {
		return hzClient;
	}
}
