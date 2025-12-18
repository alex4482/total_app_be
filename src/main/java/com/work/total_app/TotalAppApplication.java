package com.work.total_app;

import com.work.total_app.config.EmailerProperties;
import com.work.total_app.config.ReminderProperties;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.nio.file.Paths;

@Log4j2
@SpringBootApplication
@EnableConfigurationProperties({EmailerProperties.class, ReminderProperties.class})
@EnableScheduling
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
			// Only attempt to load external config if file exists and is readable
			if (externalLogConfigFile.exists() && externalLogConfigFile.canRead()) {
				// Manually initialize Log4j2 from external file
				Configurator.initialize(null, externalLogConfigPath);
				log.info("Using external log4j2.properties from {}", externalLogConfigPath);
			} else {
				// Fall back to default configuration from classpath
				// Log4j2 will automatically use log4j2.xml from resources
				log.info("Using default log4j2 configuration from classpath (resources)");
			}
		}
		catch (Exception e)
		{
			// If external config fails, Log4j2 will fall back to classpath config automatically
			// Only log a warning, don't propagate the error
			System.err.println("Warning: Could not load external log4j2 configuration: " + e.getMessage());
			System.err.println("Falling back to default configuration from classpath");
		}
	}
}
