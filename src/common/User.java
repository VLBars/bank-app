package common;

import java.io.Serializable;

public class User implements Serializable {
	private static final long serialVersionUID = 2L;
    private String login;
    private String password;
    
    public User(String login, String password) {
        this.login = login;
        this.password = password;
    }
    
    // Методы доступа (геттеры)
    public String getLogin() { return login; }
    public String getPassword() { return password; }
}