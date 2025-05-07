package csd.client;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.security.Security;

@SpringBootApplication
public class ClientApplication {

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		SpringApplication.run(ClientApplication.class, args);
	}

	@Bean
	public RestClient restClient() {
		return RestClient.builder().build();
	}

}
