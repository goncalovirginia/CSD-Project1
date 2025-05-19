package csd.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "queueExecutor")
	public Executor queueExecutor() {
		return Executors.newSingleThreadExecutor();
	}

}