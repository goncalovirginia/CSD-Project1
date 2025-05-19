package csd.client.services;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@Service
public class CommandLineApplicationService implements CommandLineRunner {

	private final ApplicationContext applicationContext;
	private final HashService hashService;
	private final HMACService hmacService;
	private final RestClientService restClientService;

	public CommandLineApplicationService(ApplicationContext applicationContext, HashService hashService, HMACService hmacService, RestClientService restClientService) {
		this.applicationContext = applicationContext;
		this.hashService = hashService;
		this.hmacService = hmacService;
		this.restClientService = restClientService;
	}

	@Override
	public void run(String... args) throws Exception {
		Scanner scanner = new Scanner(System.in);

		System.out.print("Email: ");
		String email = scanner.next();
		System.out.println("Inputted email: " + email);

		byte[] userID = hashService.hash(email.getBytes(StandardCharsets.UTF_8));
		System.out.println("UserID: " + Base64.getEncoder().encodeToString(userID));

		System.out.println("Getting ledger server public key...");
		String ledgerPublicKey = restClientService.getLedgerPublicKey();
		System.out.println("Public key obtained: " + ledgerPublicKey);

		System.out.println("Use command 'help' to see all commands, and 'quit' to exit the application.");

		String command;
		do {
			System.out.print("> ");
			command = scanner.next();

			switch (command) {
				case "createcontract":
					ByteArrayOutputStream userIDAndTimestamp = new ByteArrayOutputStream();
					userIDAndTimestamp.write(userID);
					userIDAndTimestamp.write(Instant.now().toString().getBytes(StandardCharsets.UTF_8));
					byte[] newContract = hmacService.hash(userIDAndTimestamp.toByteArray());
					String contractBase64 = Base64.getEncoder().encodeToString(newContract);
					System.out.println("New Contract: " + contractBase64);
					String response = restClientService.createContract(contractBase64);
					System.out.println(response == null ? "Error: null response" : "Successfully created contract " + response);
					break;
				case "loadmoney":
					String contract = scanner.next();
					long value = scanner.nextLong();
					long response1 = restClientService.loadMoney(contract, value);
					System.out.println(response1 == Long.MIN_VALUE ? "Error: null response" : "Successfully loaded " + response1 + " into contract " + contract);
					break;
				case "sendtransaction":
					String originContract = scanner.next();
					String destinationContract = scanner.next();
					long value1 = scanner.nextLong();
					long response2 = restClientService.sendTransaction(originContract, destinationContract, value1);
					System.out.println(response2 == Long.MIN_VALUE ? "Error: null response" : "Successfully sent " + response2 + " from " + originContract + " to " + destinationContract);
					break;
				case "getbalance":
					String contract1 = scanner.next();
					long balance = restClientService.getBalance(contract1);
					System.out.println(balance == Long.MIN_VALUE ? "Error: null response" : "Current contract balance: " + balance);
					break;
				case "getextract":
					String contract2 = scanner.next();
					String extract = restClientService.getExtract(contract2);
					for (String line : extract.split("\n")) {
						String[] parts = line.split(":");
						System.out.println("Order: " + parts[0] + "    Operation: " + parts[1] + "    Origin Account: " + parts[2] + "    Destination Account: " + parts[3]);
					}
					break;
				case "gettotalvalue":
					List<String> contractList = Arrays.stream(scanner.nextLine().trim().split(" ")).toList();
					long totalValue = restClientService.getTotalValue(contractList);
					System.out.println(totalValue == Long.MIN_VALUE ? "Error: null response" : "Total value of requested contracts: " + totalValue);
					break;
				case "getgloballedgervalue":
					long globalLedgerValue = restClientService.getGlobalLedgerValue();
					System.out.println(globalLedgerValue == Long.MIN_VALUE ? "Error: null response" : "Global ledger value: " + globalLedgerValue);
					break;
				case "getledger":
					List<String> ledger = restClientService.getLedger();
					if (ledger == null)
						System.out.println("Error: null response");
					else {
						System.out.println("Current ledger:");
						for (String row : ledger) {
							String[] parts1 = row.split(":");
							System.out.println("Contract: " + parts1[0] + "    Balance: " + parts1[1] + "    Public Key: " + parts1[2]);
						}
					}
					break;
				case "help":
					System.out.println("createcontract");
					System.out.println("loadmoney <contract> <value>");
					System.out.println("sendtransaction <originContract> <destinationContract> <value>");
					System.out.println("getbalance <contract>");
					System.out.println("getextract <contract>");
					System.out.println("gettotalvalue <contract1> <contract2> ... <contractN>");
					System.out.println("getgloballedgervalue");
					System.out.println("getledger");
					System.out.println("test");
					System.out.println("quit");
					break;
				case "test":
					doTest(userID);
					break;
				case "quit":
					break;
				default:
					System.out.println("Invalid command.");
					break;
			}
		} while (!command.equals("quit"));

		SpringApplication.exit(applicationContext, () -> 0);
	}

	private void doTest(byte[] userID) throws Exception {
		List<Map.Entry<String, Long>> contracts = new ArrayList<>();
		List<Long> latencies = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			ByteArrayOutputStream userIDAndTimestamp = new ByteArrayOutputStream();
			userIDAndTimestamp.write(userID);
			userIDAndTimestamp.write(Instant.now().toString().getBytes(StandardCharsets.UTF_8));
			byte[] newContract = hmacService.hash(userIDAndTimestamp.toByteArray());
			String contractBase64 = Base64.getEncoder().encodeToString(newContract);
			Long start = System.currentTimeMillis();
			String responseContract = restClientService.createContract(contractBase64);
			Long end = System.currentTimeMillis();
			latencies.add(end - start);
			contracts.add(new AbstractMap.SimpleEntry<>(responseContract, 0L));
			System.out.println("Created contract " + responseContract);

			Thread.sleep(100L);

			start = System.currentTimeMillis();
			long response1 = restClientService.loadMoney(responseContract, 1000L);
			end = System.currentTimeMillis();
			latencies.add(end - start);
			System.out.println(response1 == Long.MIN_VALUE ? "Error: null response" : "Successfully loaded " + response1 + " into contract " + responseContract);
			contracts.get(i).setValue(response1);
		}

		int threadCount = 1000;
		CountDownLatch latch = new CountDownLatch(threadCount);
		Long startAll = System.currentTimeMillis();
		for (int i = 0; i < threadCount; i++) {
			new Thread(() -> {
				try {
					Thread.sleep((long) (Math.random() * 5000));

					double random2 = Math.random() * 2;

					if (random2 < 1.0) {
						Map.Entry<String, Long> contract = contracts.get((int) (Math.random() * contracts.size()));
						long value = (long) (Math.random() * 1000L + 10L);
						System.out.println("Loading money onto contract: " + contract + " with value: " + value);
						Long start = System.currentTimeMillis();
						long response1 = restClientService.loadMoney(contract.getKey(), value);
						Long end = System.currentTimeMillis();
						latencies.add(end - start);
						System.out.println(response1 == Long.MIN_VALUE ? "Error: null response" : "Successfully loaded " + response1 + " into contract " + contract);
						if (response1 != Long.MIN_VALUE) contract.setValue(response1);
					} else {
						Map.Entry<String, Long> originContract = contracts.get((int) (Math.random() * contracts.size()));
						Map.Entry<String, Long> destinationContract = contracts.get((int) (Math.random() * contracts.size()));
						long value1;
						do {
							value1 = (long) (Math.random() * 10 + 1L);
						} while (value1 > originContract.getValue());
						System.out.println("Sending transaction from: " + originContract + " to: " + destinationContract + " with value: " + value1);
						Long start = System.currentTimeMillis();
						long response2 = restClientService.sendTransaction(originContract.getKey(), destinationContract.getKey(), value1);
						Long end = System.currentTimeMillis();
						latencies.add(end - start);
						System.out.println(response2 == Long.MIN_VALUE ? "Error: null response" : "Successfully sent " + response2 + " from " + originContract + " to " + destinationContract);
						if (response2 != Long.MIN_VALUE) {
							originContract.setValue(originContract.getValue() - value1);
							destinationContract.setValue(destinationContract.getValue() + value1);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				latch.countDown();
			}).start();
		}

		latch.await();
		Long endAll = System.currentTimeMillis();
		System.out.println("Finished all requests.");
		System.out.println("Average latency: " + latencies.stream().mapToLong(Long::longValue).average().orElse(0.0));
		System.out.println("Average throughput: " + (threadCount * 1000L) / (endAll - startAll));
	}

}
