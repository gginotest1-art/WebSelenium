package com.webselenium.helpers;

import java.security.SecureRandom;
import java.util.Locale;

public final class TestDataHelper {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%&*()-_";
    private static final String ALPHANUM = LOWER + DIGITS;
    private static final String ALL_CHARS = LOWER + UPPER + DIGITS + SYMBOLS;

    private TestDataHelper() { /* utility class */ }

    public static String randomUsername() {
        return randomUsername(7);
    }

    public static String randomUsername(int minLen) {
        int min = Math.max(minLen, 7);
        int extra = RANDOM.nextInt(4);
        int length = min + extra;

        StringBuilder sb = new StringBuilder(length);
        sb.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        for (int i = 1; i < length; i++) {
            sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

    public static String randomPassword() {
        return randomPassword(8);
    }

    public static String randomPassword(int minLen) {
        int min = Math.max(minLen, 7);
        int extra = RANDOM.nextInt(4);
        int length = min + extra;

        StringBuilder sb = new StringBuilder(length);
        // guarantee categories
        sb.append(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        sb.append(SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length())));

        while (sb.length() < length) {
            sb.append(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }

        return shuffle(sb.toString());
    }

    public static String randomPhone10() {
        return randomPhoneWithPrefixAndLength("0", 10);
    }

    public static String randomPhoneWithPrefixAndLength(String prefix, int totalLength) {
        if (totalLength <= 0) {
            throw new IllegalArgumentException("totalLength must be > 0");
        }
        StringBuilder sb = new StringBuilder();
        prefix = (prefix == null) ? "" : prefix;
        // If prefix longer than totalLength, truncate
        if (prefix.length() >= totalLength) {
            return prefix.substring(0, totalLength);
        }
        sb.append(prefix);
        int needed = totalLength - prefix.length();
        for (int i = 0; i < needed; i++) {
            sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }

    public static String randomEmail() {
        return randomEmailFor(randomUsername(8));
    }

    public static String randomEmailFor(String username) {
        username = (username == null || username.isBlank()) ? randomUsername() : username.toLowerCase(Locale.ROOT);
        String suffix = String.valueOf(System.currentTimeMillis() % 10000);
        return username + suffix + "@example.com";
    }

    public static UserData generateUser() {
        return generateUser(7, 8);
    }

    public static UserData generateUser(int usernameMinLen, int passwordMinLen) {
        String u = randomUsername(usernameMinLen);
        String p = randomPassword(passwordMinLen);
        String phone = randomPhone10();
        String email = randomEmailFor(u);
        return new UserData(u, p, phone, email);
    }

    public static final class UserData {
        public final String username;
        public final String password;
        public final String phone;
        public final String email;

        public UserData(String username, String password, String phone, String email) {
            this.username = username;
            this.password = password;
            this.phone = phone;
            this.email = email;
        }

        @Override
        public String toString() {
            return "UserData{" +
                    "username='" + username + '\'' +
                    ", password='" + password + '\'' +
                    ", phone='" + phone + '\'' +
                    ", email='" + email + '\'' +
                    '}';
        }
    }

    private static String shuffle(String input) {
        char[] arr = input.toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return new String(arr);
    }
}