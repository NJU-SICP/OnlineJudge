package cn.edu.nju.sicp.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sicp.admin")
public class AdminConfig {

    private String username;
    private String password;
    private String fullName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String toString() {
        return "AdminConfig{" +
                "username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                '}';
    }

}
