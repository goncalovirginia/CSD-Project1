package csd.client;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.client.RestClientSsl;
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
	public RestClient restClient(@Value("${ledger.address}") String ledgerAddress, RestClientSsl ssl) {
		return RestClient.builder().baseUrl(ledgerAddress).apply(ssl.fromBundle("client1")).build();
	}

}
