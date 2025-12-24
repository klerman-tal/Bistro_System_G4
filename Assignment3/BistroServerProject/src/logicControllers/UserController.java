package logicControllers;

import java.util.ArrayList;

import dbControllers.User_DB_Controller;
import entities.User;

public class UserController {
	//Test comment
    private User_DB_Controller userDB;



    public UserController(User_DB_Controller userDB2) {
		// TODO Auto-generated constructor stub
    	this.userDB = userDB2;
	}

	public Object login(ArrayList<?> data) {

        String username = data.get(1).toString();
        String password = data.get(2).toString();

        User user = userDB.loginUser(username, password);

        if (user == null) {
            return "LOGIN_FAILED"; 
        }

        return user;
    }
}
