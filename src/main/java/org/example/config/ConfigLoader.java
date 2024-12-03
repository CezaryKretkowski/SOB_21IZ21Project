package org.example.config;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;

public class ConfigLoader {

    public static ServerConfig loadConfig(String fileName) {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new RuntimeException("Config file not found: " + fileName);
            }
            return yaml.loadAs(inputStream, ServerConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config file: " + fileName, e);
        }
    }
}