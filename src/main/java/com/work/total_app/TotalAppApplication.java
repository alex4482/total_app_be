package com.work.total_app;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Paths;

@Log4j2
@SpringBootApplication
public class TotalAppApplication {

	public static void main(String[] args) {

		setupLoggerConfig();

		SpringApplication.run(TotalAppApplication.class, args);
	}

	public static void setupLoggerConfig()
	{
		// Define external log4j2.properties path
		String externalLogConfigPath = Paths.get("config", "log4j2.properties").toAbsolutePath().toString();
		File externalLogConfigFile = new File(externalLogConfigPath);

		try {
			if (externalLogConfigFile.exists()) {
				// Manually initialize Log4j2 from external file
				Configurator.initialize(null, externalLogConfigPath);
				log.info("Using external log4j2.properties from {}", externalLogConfigPath);
			} else {
				log.info("Using default log4j2.properties from classpath (resources)");
			}
		}
		catch (Exception e)
		{
			log.error(e.toString());
		}
	}
}
