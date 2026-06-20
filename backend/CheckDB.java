import java.sql.*;
public class CheckDB {
    public static void main(String[] args) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/exam_auth_db","root","123123");
        ResultSet rs = c.createStatement().executeQuery("SELECT email, role, supervisor_type, college_id, signature_path FROM users WHERE role LIKE '%SUPERVISOR%'");
        while(rs.next()) {
            System.out.println(rs.getString(1) + " | Role: " + rs.getString(2) + " | Type: " + rs.getString(3) + " | College: " + rs.getString(4) + " | Sig: " + rs.getString(5));
        }
        c.close();
    }
}
