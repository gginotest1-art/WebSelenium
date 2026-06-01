package com.webselenium.dataconstants;

import com.webselenium.helpers.ConfigReader;
import com.webselenium.helpers.UserCredentialsReader;

public final class DataConstants {

    private DataConstants() {
        // utility class
    }

    // ===== LOGIN BUTTONS & MESSAGES (static constants) =====
    public static final String LOGIN_BUTTON_TEXT = "Đăng nhập";
    public static final String REGISTER_BUTTON_TEXT = "Đăng ký";
    public static final String LOGOUT_BUTTON_TEXT = "Đăng xuất";
    public static final String LOGIN_SUCCESS_MSG = "Đăng nhập thành công";
    public static final String LOGIN_FAILED_MSG = "Tên đăng nhập hoặc mật khẩu không đúng";
    public static final String SESSION_EXPIRED_MSG = "Phiên làm việc của bạn đã hết hạn";

    // ===== CONFIG-DRIVEN VALUES - LAZY ACCESSORS =====
    // Use methods instead of static fields to avoid classinit failures
    public static int getImplicitWait() {
        try {
            return ConfigReader.getInt("implicit.wait");
        } catch (RuntimeException e) {
            return 10;
        }
    }

    public static int getExplicitWait() {
        try {
            return ConfigReader.getInt("explicit.wait");
        } catch (RuntimeException e) {
            return 15;
        }
    }

    public static int getPageLoadTimeout() {
        try {
            return ConfigReader.getInt("page.load.timeout");
        } catch (RuntimeException e) {
            return 30;
        }
    }

    public static String getBaseUrl() {
        try {
            return ConfigReader.get("base.url");
        } catch (RuntimeException e) {
            return "http://localhost";
        }
    }

    public static String getEnvironment() {
        try {
            return ConfigReader.get("environment");
        } catch (RuntimeException e) {
            return System.getProperty("env", "staging");
        }
    }

    public static String getBrowser() {
        try {
            return ConfigReader.get("browser", "chrome");
        } catch (RuntimeException e) {
            return "chrome";
        }
    }

    public static boolean isHeadless() {
        try {
            return ConfigReader.getBoolean("headless");
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static int getBrowserWidth() {
        try {
            return ConfigReader.getInt("browser.width");
        } catch (RuntimeException e) {
            return 1920;
        }
    }

    public static int getBrowserHeight() {
        try {
            return ConfigReader.getInt("browser.height");
        } catch (RuntimeException e) {
            return 1080;
        }
    }

    // ===== USER DATA ACCESSORS =====
    public static UserCredentialsReader.UserData getAdminUser() {
        return UserCredentialsReader.getAdminUser();
    }

    public static UserCredentialsReader.UserData getMarket4User() {
        return UserCredentialsReader.getRegularUser("market4");
    }

    public static UserCredentialsReader.UserData getVipUser() {
        return UserCredentialsReader.getVipUser();
    }

    public static UserCredentialsReader.UserData getTestUser() {
        return UserCredentialsReader.getTestUser();
    }

    public static UserCredentialsReader.UserData getInactiveUser() {
        return UserCredentialsReader.getInactiveUser();
    }

    public static String getUsername(String userType) {
        return UserCredentialsReader.get(userType).username;
    }

    public static String getPassword(String userType) {
        return UserCredentialsReader.get(userType).password;
    }
}