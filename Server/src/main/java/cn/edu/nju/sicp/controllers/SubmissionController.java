package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.configs.RolesConfig;
import cn.edu.nju.sicp.dtos.SubmissionInfo;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Submission;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import cn.edu.nju.sicp.repositories.UserRepository;
import cn.edu.nju.sicp.tasks.GradeSubmissionTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.*;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    @Autowired
    private ProjectionFactory projectionFactory;

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
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_SELF) or hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Page<SubmissionInfo>> listSubmissions(String userId, String assignmentId, Integer page, Integer size) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.getId().equals(userId) && user.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_ASSIGNMENT_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Submission target = new Submission();
        target.setUserId(userId);
        target.setAssignmentId(assignmentId);
        Page<Submission> submissions = submissionRepository
                .findAll(Example.of(target), PageRequest.of(page == null ? 0 : page,
                        size == null ? 20 : size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new ResponseEntity<>(submissions.map(s -> projectionFactory.createProjection(SubmissionInfo.class, s)), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_SELF) or hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Submission> viewSubmissions(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Submission submission = submissionRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!submission.getUserId().equals(user.getId()) &&
                user.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_ASSIGNMENT_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(submission, HttpStatus.OK);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_CREATE)")
    public ResponseEntity<Submission> createSubmission(@RequestPart("assignmentId") String assignmentId,
                                                       @RequestPart(value = "token", required = false) String token,
                                                       @RequestPart("file") MultipartFile file) throws OperationNotSupportedException, JsonProcessingException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
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
            logger.info(String.format("CreateSubmission %s by %s", submission, user));
        } catch (IOException e) {
            logger.error(String.format("CreateSubmission failed: %s %s by %s", e.getMessage(), submission, user));
            submissionRepository.delete(submission);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法存储提交文件。");
        }

        GradeSubmissionTask task = new GradeSubmissionTask(assignment, submission, submissionRepository);
        gradeSubmissionExecutor.execute(task, AsyncTaskExecutor.TIMEOUT_IMMEDIATE);
        return new ResponseEntity<>(submission, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_UPDATE)")
    public ResponseEntity<Submission> updateSubmission(@PathVariable String id, @RequestBody Submission updatedSubmission) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        submission.setResult(updatedSubmission.getResult());
        submissionRepository.save(submission);
        logger.info(String.format("UpdateSubmission %s by %s", submission, user));
        return new ResponseEntity<>(submission, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_DELETE)")
    public ResponseEntity<Void> deleteSubmission(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        submissionRepository.delete(submission);
        logger.info(String.format("DeleteSubmission %s by %s", submission, user));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{id}/rejudge")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_UPDATE)")
    public ResponseEntity<Submission> rejudgeSubmission(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        submission.setResult(null);
        submissionRepository.save(submission);

        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
        GradeSubmissionTask task = new GradeSubmissionTask(assignment, submission, submissionRepository, GradeSubmissionTask.PRIORITY_LOW);
        gradeSubmissionExecutor.execute(task, AsyncTaskExecutor.TIMEOUT_IMMEDIATE);
        logger.info(String.format("RejudgeSubmission %s by %s", submission, user));
        return new ResponseEntity<>(submission, HttpStatus.OK);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_SELF)")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();
        User user = userRepository.findByUsername(username);

        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!user.getId().equals(submission.getUserId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            Path path = Paths.get(submission.getFilePath());
            InputStreamResource resource = new InputStreamResource(new FileInputStream(path.toFile()));
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } catch (IOException e) {
            logger.error(String.format("DownloadSubmission failed: %s %s by %s", e.getMessage(), submission, user));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取提交文件。");
        }
    }
}
