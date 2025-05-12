package csd.client.services;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

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
					System.out.println(response == null ? "Error: null response" : "Contract " + response + " successfully created.");
					break;
				case "loadmoney":
					String contract = scanner.next();
					long value = scanner.nextLong();
					restClientService.loadMoney(contract, value);
					break;
				case "sendtransaction":
					String originContract = scanner.next();
					String destinationContract = scanner.next();
					long value1 = scanner.nextLong();
					restClientService.sendTransaction(originContract, destinationContract, value1);
					break;
				case "getbalance":
					String contract1 = scanner.next();
					long balance = restClientService.getBalance(contract1);
					System.out.println(balance);
					break;
				case "getextract":
					String contract2 = scanner.next();
					String extract = restClientService.getExtract(contract2);
					System.out.println(extract);
					break;
				case "gettotalvalue":
					List<String> contractList = Arrays.stream(scanner.nextLine().trim().split(" ")).toList();
					long totalValue = restClientService.getTotalValue(contractList);
					System.out.println(totalValue);
					break;
				case "getgloballedgervalue":
					long globalLedgerValue = restClientService.getGlobalLedgerValue();
					System.out.println(globalLedgerValue);
					break;
				case "getledger":
					List<String> ledger = restClientService.getLedger();
					for (String row : ledger)
						System.out.println(row);
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
					System.out.println("quit");
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

}
