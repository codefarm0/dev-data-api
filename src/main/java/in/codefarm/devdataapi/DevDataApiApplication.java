package in.codefarm.devdataapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import in.codefarm.devdataapi.config.DevDataApiProperties;

@SpringBootApplication
@EnableConfigurationProperties(DevDataApiProperties.class)
public class DevDataApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevDataApiApplication.class, args);
	}

}
