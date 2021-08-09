package cn.edu.nju.sicp.configs;

import cn.edu.nju.sicp.models.Role;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartupRunner implements CommandLineRunner {

    @Autowired
    private UserRepository repository;

    @Override
    public void run(String... args) throws Exception {
        String username = "admin";
        String password = "password";
        User admin = repository.findByUsername(username);
        if (admin == null) {
            admin = new User();
            admin.setUsername(username);
            admin.setPassword(password);
            admin.setFullName("Administrator");
        } else if (!admin.validatePassword(password)) {
            admin.setPassword(password);
        }
        admin.setEnabled(true);
        admin.setLocked(false);
        admin.setExpires(null);
        admin.setRoles(List.of(RolesConfig.ROLE_ADMIN));
        repository.save(admin);
    }

}
