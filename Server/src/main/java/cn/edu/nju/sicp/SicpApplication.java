package cn.edu.nju.sicp;

import cn.edu.nju.sicp.configs.AdminConfig;
import cn.edu.nju.sicp.configs.RolesConfig;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.stereotype.Component;

import java.util.List;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class SicpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SicpApplication.class, args);
    }

    @Component
    static class StartupRunner implements CommandLineRunner {

        @Autowired
        private UserRepository repository;

        @Autowired
        private AdminConfig adminConfig;

        private final Logger logger;

        public StartupRunner() {
            this.logger = LoggerFactory.getLogger(StartupRunner.class);
        }

        @Override
        public void run(String... args) {
            String username = adminConfig.getUsername();
            String password = adminConfig.getPassword();
            String fullName = adminConfig.getFullName();
            User admin = repository.findByUsername(username);
            if (admin == null) {
                admin = new User();
                admin.setUsername(username);
                admin.setPassword(password);
                admin.setFullName(fullName);
            } else if (!admin.validatePassword(password)) {
                admin.setPassword(password);
            }
            admin.setEnabled(true);
            admin.setLocked(false);
            admin.setExpires(null);
            admin.setRoles(List.of(RolesConfig.ROLE_ADMIN));
            logger.info(String.format("Setup admin account %s", adminConfig));
            repository.save(admin);
        }

    }

}
