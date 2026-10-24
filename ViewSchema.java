import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ViewSchema.java
 * -------------------------------------------------------------------------
 * Displays the complete database schema structure for all 4 tables in
 * kec_eval_db directly in the terminal:
 *  1. students
 *  2. courses
 *  3. enrollments
 *  4. assessment_marks
 *
 * Shows: Column Name, Data Type, Size, Nullability, Primary/Foreign Keys,
 * and Table Constraints.
 * -------------------------------------------------------------------------
 */
public class ViewSchema {

    public static void main(String[] args) {
        System.out.println("==========================================================================================");
        System.out.println("            KONGU ENGINEERING COLLEGE (R2024) - DATABASE SCHEMA VIEWER                    ");
        System.out.println("==========================================================================================");

        String[] tables = {"students", "courses", "enrollments", "assessment_marks"};

        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("Database Engine : " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            System.out.println("Database Catalog: " + conn.getCatalog());
            System.out.println("Driver          : " + meta.getDriverName() + " (" + meta.getDriverVersion() + ")");

            for (int i = 0; i < tables.length; i++) {
                String tableName = tables[i];
                System.out.println("\n------------------------------------------------------------------------------------------");
                System.out.printf(" [%d] TABLE SCHEMA: %s\n", (i + 1), tableName.toUpperCase());
                System.out.println("------------------------------------------------------------------------------------------");

                // Execute DESCRIBE on table
                String descSql = "DESCRIBE " + tableName;
                try (PreparedStatement pstmt = conn.prepareStatement(descSql);
                     ResultSet rs = pstmt.executeQuery()) {

                    System.out.println("+----------------------+----------------------+--------+-----+-------------------+----------------+");
                    System.out.printf("| %-20s | %-20s | %-6s | %-3s | %-17s | %-14s |\n",
                            "FIELD / COLUMN", "DATA TYPE", "NULL", "KEY", "DEFAULT", "EXTRA");
                    System.out.println("+----------------------+----------------------+--------+-----+-------------------+----------------+");

                    while (rs.next()) {
                        String field = rs.getString("Field");
                        String type = rs.getString("Type");
                        String isNull = rs.getString("Null");
                        String key = rs.getString("Key");
                        String defVal = rs.getString("Default");
                        String extra = rs.getString("Extra");

                        System.out.printf("| %-20s | %-20s | %-6s | %-3s | %-17s | %-14s |\n",
                                field,
                                type,
                                isNull,
                                key,
                                (defVal != null ? defVal : "NULL"),
                                extra);
                    }
                    System.out.println("+----------------------+----------------------+--------+-----+-------------------+----------------+");
                }

                // Show Foreign Keys if any
                showForeignKeys(conn, tableName);
            }

            System.out.println("\n==========================================================================================");
            System.out.println(" Schema inspection completed successfully for all 4 tables.");
            System.out.println("==========================================================================================");

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to inspect schema: " + e.getMessage());
        }
    }

    private static void showForeignKeys(Connection conn, String tableName) {
        String fkSql =
            "SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
            "FROM information_schema.KEY_COLUMN_USAGE " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND REFERENCED_TABLE_NAME IS NOT NULL";

        try (PreparedStatement pstmt = conn.prepareStatement(fkSql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean hasFk = false;
                while (rs.next()) {
                    if (!hasFk) {
                        System.out.println(" Foreign Key Relationships:");
                        hasFk = true;
                    }
                    System.out.printf("  * %s (%s) --> REFERENCES %s(%s)\n",
                            rs.getString("CONSTRAINT_NAME"),
                            rs.getString("COLUMN_NAME"),
                            rs.getString("REFERENCED_TABLE_NAME"),
                            rs.getString("REFERENCED_COLUMN_NAME"));
                }
            }
        } catch (SQLException e) {
            // Suppress minor metadata query errors
        }
    }
}
