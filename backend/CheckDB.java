import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/exam_auth_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String user = "root";
        String pass = "123123";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("=== EXAMS ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT exam_name, institution_name, status FROM exams")) {
                while (rs.next()) {
                    System.out.println(rs.getString("exam_name") + " | " + rs.getString("institution_name") + " | " + rs.getString("status"));
                }
            }

            System.out.println("\n=== REASSIGNING EXAMS TO SPPU ===");
            try (Statement stmt = conn.createStatement()) {
                int updated = stmt.executeUpdate("UPDATE new_university_exams SET institution_code = '123' WHERE institution_code = 'TEST2025B'");
                System.out.println("Updated " + updated + " exams. Reassigned back to SPPU!");
            }


            System.out.println("\n=== INSTITUTIONS ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name, institution_code, login_key, admin_email FROM institutions")) {
                while (rs.next()) {
                    System.out.println(rs.getString("name") + " | " + rs.getString("institution_code") + " | " + rs.getString("login_key") + " | " + rs.getString("admin_email"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
