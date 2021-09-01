package cn.edu.nju.sicp;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import cn.edu.nju.sicp.configs.AdminConfig;
import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.configs.RolesConfig;
import cn.edu.nju.sicp.configs.S3Config;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class SicpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SicpApplication.class, args);
    }

    @Component
    static class StartupRunner implements CommandLineRunner {

        private final AdminConfig adminConfig;
        private final S3Config s3Config;
        private final DockerConfig dockerConfig;
        private final UserRepository userRepository;
        private final Logger logger;

        public StartupRunner(AdminConfig adminConfig, S3Config s3Config, DockerConfig dockerConfig,
                UserRepository userRepository) {
            this.adminConfig = adminConfig;
            this.s3Config = s3Config;
            this.dockerConfig = dockerConfig;
            this.userRepository = userRepository;
            this.logger = LoggerFactory.getLogger(StartupRunner.class);
        }

        @Override
        public void run(String... args) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.getLogger("org.mongodb.driver").setLevel(Level.ERROR);

            applyAdminConfig();
            applyDockerConfig();
            applyS3Config();
        }

        private void applyAdminConfig() {
            logger.info(String.format("Applying admin config %s", adminConfig));
            try {
                String username = adminConfig.getUsername();
                String password = adminConfig.getPassword();
                String fullName = adminConfig.getFullName();
                User admin = userRepository.findByUsername(username).orElseGet(() -> {
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
                userRepository.save(admin);
            } catch (Exception e) {
                logger.error(String.format("Cannot apply: %s", e.getMessage()), e);
                throw e;
            }
            logger.info("Apply admin config OK");
        }

        private void applyDockerConfig() {
            logger.info(String.format("Applying docker config %s", dockerConfig));
            try {
                dockerConfig.getInstance().pingCmd().exec();
            } catch (Exception e) {
                logger.error(String.format("Cannot apply: %s", e.getMessage()), e);
                throw e;
            }
            logger.info("Apply docker config OK");
        }

        private void applyS3Config() {
            logger.info(String.format("Applying s3 config %s", s3Config));
            try {
                S3Client s3Client = s3Config.getInstance();
                try {
                    s3Client.headBucket(builder -> builder.bucket(s3Config.getBucket()).build());
                } catch (NoSuchBucketException e) {
                    s3Client.createBucket(builder -> builder.bucket(s3Config.getBucket()).build());
                }
            } catch (S3Exception e) {
                logger.error(String.format("Cannot apply: %s", e.getMessage()), e);
                throw e;
            }
            logger.info("Apply s3 config OK");
        }

    }

}
