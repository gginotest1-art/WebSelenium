package com.webselenium.helpers;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesHelper {

    private static Properties properties = new Properties();

    public static void loadFile(String fileName) {
        try (InputStream inputStream = PropertiesHelper.class
                .getClassLoader()
                .getResourceAsStream(fileName)) {

            if (inputStream == null) {
                throw new RuntimeException("Cannot find file in resources: " + fileName);
            }

            properties.load(inputStream);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load properties file: " + fileName, e);
        }
    }

    public static String getValue(String key) {
        return properties.getProperty(key);
    }
}