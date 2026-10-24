import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection.java
 * -------------------------------------------------------------------------
 * Manages JDBC connections to MySQL for the Kongu Engineering College (R2024)
 * Student Academic & Continuous Evaluation System.
 *
 * Design Principles:
 *  - Reads credentials from external "db.properties" or environment variables
 *    (DB_URL, DB_USER, DB_PASSWORD) to prevent exposing secrets in source code.
 *  - Automatically registers the MySQL Connector/J driver.
 *  - Employs fail-safe fallback defaults and informative error diagnostics.
 *  - Suitable for use with Java 7+ try-with-resources.
 * -------------------------------------------------------------------------
 */
public class DBConnection {

    private static final String DEFAULT_PROPERTIES_FILE = "db.properties";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/kec_eval_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean initialized = false;

    // Static block to initialize credentials and register driver
    static {
        loadConfiguration();
    }

    /**
     * Loads database configuration with priority:
     * 1. Environment variables (DB_URL, DB_USER, DB_PASSWORD)
     * 2. Local "db.properties" file
     * 3. Fallback defaults
     */
    public static synchronized void loadConfiguration() {
        Properties props = new Properties();
        File propFile = new File(DEFAULT_PROPERTIES_FILE);

        if (propFile.exists()) {
            try (InputStream input = new FileInputStream(propFile)) {
                props.load(input);
            } catch (IOException e) {
                System.err.println("[WARN] Failed to read db.properties: " + e.getMessage());
            }
        }

        // Environment variables override properties file if set
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        dbUrl = (envUrl != null && !envUrl.trim().isEmpty())
                ? envUrl.trim()
                : props.getProperty("db.url", DEFAULT_URL).trim();

        dbUser = (envUser != null && !envUser.trim().isEmpty())
                ? envUser.trim()
                : props.getProperty("db.user", DEFAULT_USER).trim();

        dbPassword = (envPass != null)
                ? envPass
                : props.getProperty("db.password", DEFAULT_PASSWORD);

        // Explicitly load MySQL Connector/J driver class
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initialized = true;
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] MySQL Connector/J driver not found in classpath!");
            System.err.println("        Please ensure 'mysql-connector-j-9.x.x.jar' is in the classpath.");
            initialized = false;
        }
    }

    /**
     * Obtains a new database connection.
     * Callers should manage the lifecycle using try-with-resources:
     * <pre>
     *   try (Connection conn = DBConnection.getConnection()) {
     *       // Execute SQL operations
     *   }
     * </pre>
     *
     * @return active java.sql.Connection
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfiguration();
        }
        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Connection failed to: " + dbUrl);
            System.err.println("           User: " + dbUser);
            System.err.println("           Message: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Tests if the database connection can be established successfully.
     *
     * @return true if connection succeeded, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Allows dynamic override of credentials at runtime (e.g. from user prompt).
     */
    public static synchronized void setCredentials(String url, String user, String password) {
        if (url != null && !url.trim().isEmpty()) dbUrl = url.trim();
        if (user != null && !user.trim().isEmpty()) dbUser = user.trim();
        if (password != null) dbPassword = password;
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }

    /**
     * Standalone test harness for quick diagnostic verification.
     */
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" KEC (R2024) JDBC Connection Diagnostic Utility");
        System.out.println("==================================================");
        System.out.println("Target URL : " + dbUrl);
        System.out.println("User       : " + dbUser);
        System.out.println("Password   : " + (dbPassword.isEmpty() ? "(empty)" : "********"));
        System.out.print("Testing connection... ");

        if (testConnection()) {
            System.out.println("[SUCCESS] Connected to MySQL database successfully!");
        } else {
            System.out.println("[FAILED] Unable to connect.");
            System.out.println("Troubleshooting tips:");
            System.out.println(" 1. Ensure MySQL service is running.");
            System.out.println(" 2. Verify credentials in db.properties.");
            System.out.println(" 3. Run kec_schema.sql to create database & tables.");
        }
    }
}
