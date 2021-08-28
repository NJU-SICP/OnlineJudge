package cn.edu.nju.sicp;

import cn.edu.nju.sicp.configs.AdminConfig;
import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.configs.RolesConfig;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Submission;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import cn.edu.nju.sicp.repositories.UserRepository;
import cn.edu.nju.sicp.tasks.BuildImageTask;
import cn.edu.nju.sicp.tasks.GradeSubmissionTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        @Autowired
        private AssignmentRepository assignmentRepository;

        @Autowired
        private SubmissionRepository submissionRepository;

        @Autowired
        private SimpleAsyncTaskExecutor buildImageExecutor;

        @Qualifier("threadPoolTaskExecutor")
        @Autowired
        private ThreadPoolTaskExecutor gradeSubmissionExecutor;

        private final Logger logger;

        public StartupRunner() {
            this.logger = LoggerFactory.getLogger(StartupRunner.class);
        }

        @Override
        public void run(String... args) {
            applyAdminConfig();
            applyDockerConfig();
            createBuildImageTasks();
            createGradeSubmissionTasks();
        }

        private void applyAdminConfig() {
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
        }

        private void applyDockerConfig() {
            logger.info(String.format("Applying docker config %s", dockerConfig));
            try {
                dockerConfig.getInstance().pingCmd().exec();
            } catch (Exception e) {
                logger.error(String.format("Cannot apply: %s %s", e.getClass().getName(), e.getMessage()));
                throw e;
            }
            logger.info("Apply docker config OK");
        }

        private void createBuildImageTasks() {
            logger.info("Creating Build Image Tasks");
            List<Assignment> assignments = assignmentRepository.findAll().stream()
                    .filter(a -> a.getGrader() != null && a.getGrader().getImageId() == null)
                    .collect(Collectors.toList());
            for (Assignment assignment : assignments) {
                BuildImageTask task = new BuildImageTask(assignment.getGrader(), assignmentRepository, dockerConfig.getInstance());
                buildImageExecutor.execute(task, AsyncTaskExecutor.TIMEOUT_IMMEDIATE);
                logger.info(String.format("BuildImage %s", assignment));
            }
            logger.info("Create Build Image Tasks OK");
        }

        private void createGradeSubmissionTasks() {
            logger.info("Creating Grade Submission Tasks");
            Submission example = new Submission();
            example.setGraded(false);
            List<Submission> submissions = submissionRepository.findAll(Example.of(example));
            for (Submission submission : submissions) {
                if (submission.getResult() == null ||
                        submission.getResult().getError() == null ||
                        submission.getResult().getRetryAt() != null) {
                    Optional<Assignment> optionalAssignment = assignmentRepository.findById(submission.getAssignmentId());
                    if (optionalAssignment.isPresent()) {
                        submission.setGraded(false);
                        submission.setResult(null);
                        submissionRepository.save(submission);
                        GradeSubmissionTask task = new GradeSubmissionTask(optionalAssignment.get(),
                                submission, submissionRepository, dockerConfig.getInstance(), GradeSubmissionTask.PRIORITY_LOW);
                        gradeSubmissionExecutor.execute(task, AsyncTaskExecutor.TIMEOUT_IMMEDIATE);
                        logger.info(String.format("RejudgeSubmission %s", submission));
                    }
                }
            }
            logger.info("Create Grade Submission Tasks OK");
        }

    }

}
