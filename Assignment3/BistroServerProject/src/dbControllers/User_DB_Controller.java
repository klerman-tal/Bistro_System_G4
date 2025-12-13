package dbControllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import entities.Enums;
import entities.User;

public class User_DB_Controller {

    private Connection conn;

    public User_DB_Controller(Connection conn) {
        this.conn = conn;
    }
    
	
	public User loginUser(String username, String password) {
	    User user = null;
	    
	   try {
	        PreparedStatement stmt = conn.prepareStatement("SELECT * from users WHERE username =? AND password = ?");
	        stmt.setString(1, username);
	        stmt.setString(2,password);
	        
	        ResultSet rs = stmt.executeQuery(); //Return result of a query in sql//
	        if (rs.next()) {
	        	user = new User();
	            // Handle user_id which may be NULL (guest user)
                Object idObj = rs.getObject("user_id");
                if (idObj != null)
                    user.setUserId((Integer) idObj);
                else
                    user.setUserId(null);

                user.setUserName(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setPhoneNumber(rs.getString("phone_number"));
                user.setEmail(rs.getString("email"));

                // Convert DB string -> Enum
                user.setUserRole(Enums.UserRole.valueOf(rs.getString("role")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user; // returns null if not found
    }
}
	        
	        	
	        

	        

	        
	   
