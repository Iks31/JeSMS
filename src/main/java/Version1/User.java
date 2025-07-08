package Version1;

import java.util.ArrayList;

public class User {
    public ArrayList<User> Friends;
    public String name;
    public int UID;
    public User(String name, int UID) {
        this.name = name;
        this.UID = UID;
    }

    public User(String name, String password) {
        //check password against MongoDB
    }
}
