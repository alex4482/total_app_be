package com.work.total_app.helpers;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

@Getter
@Log4j2
public class PropertiesHelper {
    private static Properties properties;

    public static void reloadProperties()
    {
        // Define external log4j2.properties path
        String externalCustomConfigPath = Paths.get("config", "custom.properties").toAbsolutePath().toString();
        File externalLogConfigFile = new File(externalCustomConfigPath);

        try {
            InputStream input;
            if (externalLogConfigFile.exists()) {
                log.info("Using external custom.properties from {}", externalCustomConfigPath);
                input = new FileInputStream(externalCustomConfigPath);
            }
            else {
                log.info("Using default custom.properties from resources. ");
                input = PropertiesHelper.class.getClassLoader().getResourceAsStream("custom.properties");
            }
            properties = new Properties();
            properties.load(input);
        }
        catch (Exception ex) {
            log.error("Failed to read and load new properties\n", ex);
        }
    }

    public static String getProp(String key)
    {
        return properties.getProperty(key);
    }
}
