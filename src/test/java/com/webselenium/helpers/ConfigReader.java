package com.webselenium.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigReader {

    private static final Properties prop = new Properties();

    static {
        loadConfig();
    }

    private static void loadConfig() {
        String envProp = System.getProperty("env", "staging").trim();
        String env = envProp.isEmpty() ? "staging" : envProp;

        // Candidate filenames to try (in order)
        List<String> candidates = new ArrayList<>();
        candidates.add(env + ".properties");

        // common alias handling (stg <-> staging)
        if ("staging".equalsIgnoreCase(env)) {
            candidates.add("stg.properties");
        } else if ("stg".equalsIgnoreCase(env)) {
            candidates.add("staging.properties");
        }

        // also try generic config files as fallback
        candidates.add("config.properties");
        candidates.add("application.properties");

        // attempt to load first available file from classpath
        InputStream is = null;
        List<String> tried = new ArrayList<>();
        for (String candidate : candidates) {
            tried.add(candidate);
            is = ConfigReader.class.getClassLoader().getResourceAsStream(candidate);
            if (is != null) {
                try {
                    prop.load(is);
                    System.out.println("✓ Config loaded from classpath: " + candidate + " (env=" + env + ")");
                    try { is.close(); } catch (Exception ignored) {}
                    return;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load config file: " + candidate, e);
                }
            }
        }

        // If nothing found, throw clear error listing attempted names
        StringBuilder sb = new StringBuilder();
        sb.append("Config file not found in classpath. Tried: ");
        sb.append(String.join(", ", tried));
        sb.append(". Please add one of these files to src/test/resources and re-run tests. ");
        sb.append("Example: staging.properties or stg.properties");
        throw new RuntimeException(sb.toString());
    }

    public static String get(String key) {
        String value = prop.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Property key not found: " + key);
        }
        return value;
    }

    public static String get(String key, String defaultValue) {
        return prop.getProperty(key, defaultValue);
    }

    public static int getInt(String key) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer value for key: " + key, e);
        }
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    // convenience methods
    public static String getBaseUrl() { return get("base.url"); }
    public static String getEnvironment() { return get("environment"); }
    public static int getImplicitWait() { return getInt("implicit.wait"); }
    public static int getExplicitWait() { return getInt("explicit.wait"); }
    public static int getPageLoadTimeout() { return getInt("page.load.timeout"); }
    public static String getBrowser() { return get("browser", "chrome"); }
    public static boolean isHeadless() { return getBoolean("headless"); }
    public static int getBrowserWidth() { return getInt("browser.width"); }
    public static int getBrowserHeight() { return getInt("browser.height"); }

    public static void printAllConfig() {
        System.out.println("\n========== CURRENT CONFIG ==========");
        prop.forEach((k, v) -> System.out.println(k + " = " + v));
        System.out.println("=====================================\n");
    }
}