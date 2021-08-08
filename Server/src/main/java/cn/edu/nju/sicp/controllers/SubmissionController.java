package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Submission;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import cn.edu.nju.sicp.repositories.UserRepository;
import cn.edu.nju.sicp.tasks.GradeSubmissionTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.naming.OperationNotSupportedException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Qualifier("threadPoolTaskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor gradeSubmissionExecutor;

    private final String dataPath;
    private final Logger logger;

    public SubmissionController(@Value("${spring.application.data-path}") String dataPath) {
        this.dataPath = dataPath;
        this.logger = LoggerFactory.getLogger(SubmissionController.class);
    }

    @GetMapping()
    public ResponseEntity<Page<Submission>> getSubmissions(String userId, String assignmentId, int page) {
        Submission target = new Submission();
        target.setUserId(userId);
        target.setAssignmentId(assignmentId);
        Page<Submission> submissions = submissionRepository
                .findAll(Example.of(target), PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new ResponseEntity<>(submissions, HttpStatus.OK);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Submission> createSubmission(@RequestPart("assignmentId") String assignmentId,
                                                       @RequestPart(value = "token", required = false) String token,
                                                       @RequestPart("file") MultipartFile file) throws OperationNotSupportedException, JsonProcessingException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();
        User user = userRepository.findByUsername(username);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // TODO: support submit by token
        if (token != null) throw new OperationNotSupportedException("token not supported");

        int limit = assignment.getSubmitCountLimit();
        if (limit > 0) {
            int count = submissionRepository.countByUserIdAndAssignmentId(user.getId(), assignment.getId());
            if (count >= limit) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交次数已达上限。");
            }
        } else if (limit == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "此作业不允许自行提交。");
        }

        Submission submission = new Submission();
        submission.setUserId(user.getId());
        submission.setAssignmentId(assignment.getId());
        submission.setCreatedAt(new Date());
        submission.setCreatedBy(null);
        submission.setResult(null);
        submission = submissionRepository.save(submission);

        try {
            Path path = Paths.get(dataPath, "submissions", submission.getId(), "submit" + assignment.getSubmitFileType());
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            file.transferTo(path);
            submission.setFilePath(path.toString());
            submissionRepository.save(submission);
            logger.info(String.format("CreateSubmission %s", submission));
        } catch (IOException e) {
            logger.error("Cannot save submission file: " + e.getMessage());
            submissionRepository.delete(submission);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法存储提交文件。");
        }

        GradeSubmissionTask task = new GradeSubmissionTask(assignment, submission, submissionRepository);
        gradeSubmissionExecutor.execute(task, AsyncTaskExecutor.TIMEOUT_IMMEDIATE);
        return new ResponseEntity<>(submission, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/rejudge")
    public ResponseEntity<Submission> rejudgeSubmission(@PathVariable String id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        submission.setResult(null);
        submissionRepository.save(submission);

        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
        GradeSubmissionTask task = new GradeSubmissionTask(assignment, submission, submissionRepository, GradeSubmissionTask.PRIORITY_LOW);
        gradeSubmissionExecutor.execute(task, AsyncTaskExecutor.TIMEOUT_IMMEDIATE);
        return new ResponseEntity<>(submission, HttpStatus.OK);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();
        User user = userRepository.findByUsername(username);

        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!(user.getRing() < 3 || user.getId().equals(submission.getUserId()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            Path path = Paths.get(submission.getFilePath());
            InputStreamResource resource = new InputStreamResource(new FileInputStream(path.toFile()));
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Cannot load submission file: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取提交文件。");
        }
    }
}
