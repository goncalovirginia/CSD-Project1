package csd.client.services;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
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
					restClientService.createContract(contractBase64);
					break;
				case "loadmoney":
					String contract = scanner.next();
					int value = scanner.nextInt();
					restClientService.loadMoney(contract, value);
					break;
				default:
					System.out.println("Invalid command.");
					break;
			}
		} while (!command.equals("quit"));

		SpringApplication.exit(applicationContext, () -> 0);
	}

}
