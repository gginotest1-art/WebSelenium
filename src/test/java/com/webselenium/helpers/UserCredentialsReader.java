package com.webselenium.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class UserCredentialsReader {

    private static Properties userProps;
    private static final String USER_FILE = "users.properties";

    static {
        loadUserCredentials();
    }

    private static void loadUserCredentials() {
        try {
            userProps = new Properties();

            // ✅ FIX: Dùng ClassLoader thay vì FileInputStream
            InputStream inputStream = UserCredentialsReader.class.getClassLoader()
                    .getResourceAsStream(USER_FILE);

            if (inputStream == null) {
                throw new RuntimeException("User credentials file not found in classpath: " + USER_FILE);
            }

            userProps.load(inputStream);
            inputStream.close();

            System.out.println("✓ User credentials loaded: " + USER_FILE);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load user credentials", e);
        }
    }

    /**
     * Get user data by role/type
     */
    public static UserData getUser(String userType) {
        String username = userProps.getProperty(userType + ".username");
        String password = userProps.getProperty(userType + ".password");
        String email = userProps.getProperty(userType + ".email");
        String role = userProps.getProperty(userType + ".role");

        if (username == null) {
            throw new RuntimeException("User type not found: " + userType);
        }

        return new UserData(userType, username, password, email, role);
    }

    public static UserData getAdminUser() {
        return getUser("admin");
    }

    public static UserData getRegularUser(String userType) {
        return getUser(userType);
    }

    public static UserData getVipUser() {
        return getUser("vip");
    }

    public static UserData getTestUser() {
        return getUser("test");
    }

    public static UserData getInactiveUser() {
        return getUser("inactive");
    }

    public static UserData get(String userType) {
        return getUser(userType);
    }

    /**
     * Inner class để lưu user data
     */
    public static class UserData {
        public String type;
        public String username;
        public String password;
        public String email;
        public String role;

        public UserData(String type, String username, String password, String email, String role) {
            this.type = type;
            this.username = username;
            this.password = password;
            this.email = email;
            this.role = role;
        }

        @Override
        public String toString() {
            return "UserData{" +
                    "type='" + type + '\'' +
                    ", username='" + username + '\'' +
                    ", role='" + role + '\'' +
                    '}';
        }
    }
}