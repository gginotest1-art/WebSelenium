package com.webselenium.helpers;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UserCredentialsReader {

    private static final String USER_FILE = "users.yaml";
    private static final Map<String, UserData> USERS;

    static {
        USERS = Collections.unmodifiableMap(loadUsers());
        System.out.println("✓ User credentials loaded: " + USER_FILE + " (" + USERS.size() + " users)");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, UserData> loadUsers() {
        try (InputStream is = UserCredentialsReader.class.getClassLoader().getResourceAsStream(USER_FILE)) {
            if (is == null) {
                throw new RuntimeException("User credentials file not found in classpath: " + USER_FILE);
            }
            Map<String, Object> root = new Yaml().load(is);
            if (root == null || !(root.get("users") instanceof Map)) {
                throw new RuntimeException(USER_FILE + " missing top-level 'users:' map");
            }
            Map<String, Object> raw = (Map<String, Object>) root.get("users");
            Map<String, UserData> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                if (!(e.getValue() instanceof Map)) continue;
                out.put(e.getKey(), UserData.fromMap(e.getKey(), (Map<String, Object>) e.getValue()));
            }
            return out;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + USER_FILE, e);
        }
    }

    // ===== LOOKUP =====

    public static UserData get(String type) {
        UserData u = USERS.get(type);
        if (u == null) throw new RuntimeException("User type not found: " + type);
        return u;
    }

    public static UserData getUser(String type) { return get(type); }

    public static boolean has(String type) { return USERS.containsKey(type); }

    // ===== BACKWARDS-COMPAT NAMED ACCESSORS =====
    // Existing tests reference these; new code should prefer get("xxx") or filter().

    public static UserData getAdminUser()    { return get("admin"); }
    public static UserData getVipUser()      { return get("vip"); }
    public static UserData getTestUser()     { return get("test"); }
    public static UserData getInactiveUser() { return get("inactive"); }
    public static UserData getRegularUser(String type) { return get(type); }

    // ===== BULK / FILTER QUERIES =====

    /** All users in declaration order (including inactive). */
    public static List<UserData> all() {
        return new ArrayList<>(USERS.values());
    }

    /** All users where active != false. */
    public static List<UserData> allActive() {
        return filter(u -> u.active);
    }

    /** Users matching the given role (case-insensitive). */
    public static List<UserData> byRole(String role) {
        if (role == null) return Collections.emptyList();
        return filter(u -> role.equalsIgnoreCase(u.role));
    }

    /** Users carrying every tag in the given list. */
    public static List<UserData> withTags(String... tags) {
        if (tags == null || tags.length == 0) return all();
        return filter(u -> {
            for (String t : tags) if (!u.tags.contains(t)) return false;
            return true;
        });
    }

    public static List<UserData> filter(Predicate<UserData> p) {
        return USERS.values().stream().filter(p).collect(Collectors.toList());
    }

    // ===== USER DATA =====

    public static final class UserData {
        public final String type;
        public final String username;
        public final String password;
        public final String email;
        public final String role;
        public final long balance;
        public final boolean active;
        public final List<String> tags;

        public UserData(String type, String username, String password, String email,
                        String role, long balance, boolean active, List<String> tags) {
            this.type = type;
            this.username = username;
            this.password = password;
            this.email = email;
            this.role = role;
            this.balance = balance;
            this.active = active;
            this.tags = tags == null ? Collections.emptyList() : Collections.unmodifiableList(tags);
        }

        @SuppressWarnings("unchecked")
        static UserData fromMap(String type, Map<String, Object> m) {
            String username = str(m.get("username"));
            String password = str(m.get("password"));
            if (username == null || password == null) {
                throw new RuntimeException("User '" + type + "' missing username/password");
            }
            long balance = 0L;
            Object b = m.get("balance");
            if (b instanceof Number) balance = ((Number) b).longValue();
            boolean active = !Boolean.FALSE.equals(m.get("active"));
            List<String> tags = m.get("tags") instanceof List
                    ? ((List<Object>) m.get("tags")).stream().map(Object::toString).collect(Collectors.toList())
                    : Collections.emptyList();
            return new UserData(type, username, password,
                    str(m.get("email")), str(m.get("role")),
                    balance, active, tags);
        }

        private static String str(Object o) { return o == null ? null : o.toString(); }

        /** Returned by TestNG when used as a @DataProvider param — keep it short. */
        @Override
        public String toString() { return type; }
    }
}