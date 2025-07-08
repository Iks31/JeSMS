import java.util.ArrayList;
import java.util.Random;

public class User {
    ArrayList<User> friends = new ArrayList<User>();
    String name;
    String username;
    String password;
    int UID;
    public User(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
        Random random = new Random();
        this.UID = random.nextInt(1000);
    }
}
