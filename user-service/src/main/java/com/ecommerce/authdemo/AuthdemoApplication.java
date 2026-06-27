package com.ecommerce.authdemo;

import com.ecommerce.authdemo.config.MysqlConnectivityFallbackListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthdemoApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(AuthdemoApplication.class);
		application.addListeners(new MysqlConnectivityFallbackListener());
		application.run(args);
	}

}
