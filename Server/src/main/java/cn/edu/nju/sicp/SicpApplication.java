package cn.edu.nju.sicp;

import cn.edu.nju.sicp.configs.AdminConfig;
import cn.edu.nju.sicp.configs.DockerConfig;
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

        @Autowired
        private DockerConfig dockerConfig;

        private final Logger logger;

        public StartupRunner() {
            this.logger = LoggerFactory.getLogger(StartupRunner.class);
        }

        @Override
        public void run(String... args) {
            logger.info(String.format("Applying admin config %s", adminConfig));
            try {
                String username = adminConfig.getUsername();
                String password = adminConfig.getPassword();
                String fullName = adminConfig.getFullName();
                User admin = repository.findByUsername(username).orElseGet(() -> {
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(password);
                    user.setFullName(fullName);
                    return user;
                });
                if (!admin.validatePassword(password)) {
                    admin.setPassword(password);
                }
                admin.setEnabled(true);
                admin.setLocked(false);
                admin.setExpires(null);
                admin.setRoles(List.of(RolesConfig.ROLE_ADMIN));
                repository.save(admin);
            } catch (Exception e) {
                logger.error(String.format("Cannot apply: %s %s", e.getClass().getName(), e.getMessage()));
                throw e;
            }
            logger.info("Apply admin config OK");

            logger.info(String.format("Applying docker config %s", dockerConfig));
            try {
                dockerConfig.getInstance().pingCmd().exec();
            } catch (Exception e) {
                logger.error(String.format("Cannot apply: %s %s", e.getClass().getName(), e.getMessage()));
                throw e;
            }
            logger.info("Apply docker config OK");
        }

    }

}
