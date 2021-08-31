package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.configs.RolesConfig;
import cn.edu.nju.sicp.configs.S3Config;
import cn.edu.nju.sicp.dtos.SubmissionInfo;
import cn.edu.nju.sicp.dtos.UserInfo;
import cn.edu.nju.sicp.models.*;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import cn.edu.nju.sicp.repositories.TokenRepository;
import cn.edu.nju.sicp.repositories.UserRepository;
import cn.edu.nju.sicp.tasks.GradeSubmissionTask;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.*;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private TokenRepository tokenRepository;

    @Autowired
    private ProjectionFactory projectionFactory;

    @Qualifier("threadPoolTaskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor gradeSubmissionExecutor;

    @Autowired
    private DockerConfig dockerConfig;

    private final String s3Bucket;
    private final S3Client s3Client;
    private final Logger logger;

    public SubmissionController(S3Config s3Config) {
        this.s3Bucket = s3Config.getBucket();
        this.s3Client = s3Config.getInstance();
        this.logger = LoggerFactory.getLogger(SubmissionController.class);
    }

    @GetMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_SELF) or hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Page<SubmissionInfo>> listSubmissions(@RequestParam(required = false) String userId,
                                                                @RequestParam(required = false) String assignmentId,
                                                                @RequestParam(required = false) Boolean graded,
                                                                @RequestParam(required = false) Integer page,
                                                                @RequestParam(required = false) Integer size) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.getId().equals(userId) && user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_SUBMISSION_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Assignment assignment = assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId).orElse(null);

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setAssignmentId(assignment == null ? null : assignment.getId());
        submission.setGraded(graded);
        Page<SubmissionInfo> infos = submissionRepository
                .findAll(Example.of(submission),
                        PageRequest.of(page == null || page < 0 ? 0 : page,
                                size == null || size < 0 ? 20 : size,
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(s -> projectionFactory.createProjection(SubmissionInfo.class, s));
        return new ResponseEntity<>(infos, HttpStatus.OK);
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Long> countSubmissions(@RequestParam(required = false) String userId,
                                                 @RequestParam(required = false) String assignmentId,
                                                 @RequestParam(required = false) Boolean graded) {
        Assignment assignment = assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId).orElse(null);
        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setAssignmentId(assignment == null ? null : assignment.getId());
        submission.setGraded(graded);
        long count = submissionRepository.count(Example.of(submission));
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_ALL) and hasAuthority(@Roles.OP_USER_READ)")
    public ResponseEntity<Page<UserInfo>> getUsers(@RequestParam String assignmentId,
                                                   @RequestParam Boolean submitted,
                                                   @RequestParam(defaultValue = RolesConfig.ROLE_STUDENT) String role,
                                                   @RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size) {
        Assignment assignment = assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId).orElse(null);
        Submission submission = new Submission();
        submission.setAssignmentId(assignment == null ? null : assignment.getId());
        List<String> userIds = submissionRepository
                .findAll(Example.of(submission)).stream()
                .map(Submission::getUserId).collect(Collectors.toList());
        Page<User> users;
        PageRequest pageRequest = PageRequest.of(page == null || page < 0 ? 0 : page,
                size == null || size < 0 ? 20 : size,
                Sort.by(Sort.Direction.ASC, "id"));
        if (submitted) {
            users = userRepository.findAllByIdInAndRolesContains(userIds, role, pageRequest);
        } else {
            users = userRepository.findAllByIdNotInAndRolesContains(userIds, role, pageRequest);
        }
        Page<UserInfo> infos = users.map(u -> projectionFactory.createProjection(UserInfo.class, u));
        return new ResponseEntity<>(infos, HttpStatus.OK);
    }

    @GetMapping("/users/count")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Long> countUsers(@RequestParam String assignmentId,
                                           @RequestParam Boolean submitted,
                                           @RequestParam String role) {
        Assignment assignment = assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId).orElse(null);
        Submission submission = new Submission();
        submission.setAssignmentId(assignment == null ? null : assignment.getId());
        List<String> userIds = submissionRepository
                .findAll(Example.of(submission)).stream()
                .map(Submission::getUserId).collect(Collectors.toList());
        long count;
        if (submitted) {
            count = userRepository.countAllByIdInAndRolesContains(userIds, role);
        } else {
            count = userRepository.countAllByIdNotInAndRolesContains(userIds, role);
        }
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    @GetMapping("/scores/statistics")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_SELF) or hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<DoubleSummaryStatistics> getAverageScore(@RequestParam String assignmentId,
                                                                   @RequestParam(required = false) String userId,
                                                                   @RequestParam(required = false) Boolean unique) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userId != null && !userId.equals(user.getId()) && user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_SUBMISSION_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Assignment assignment = assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId).orElse(null);

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setAssignmentId(assignment == null ? null : assignment.getId());

        Stream<Submission> stream;
        if (unique == null || !unique) {
            stream = submissionRepository
                    .findAll(Example.of(submission)).stream()
                    .filter(s -> s.getResult() != null && s.getResult().getScore() != null);
        } else {
            stream = submissionRepository
                    .findAll(Example.of(submission)).stream()
                    .filter(s -> s.getResult() != null && s.getResult().getScore() != null)
                    .collect(Collectors.toMap(Submission::getUserId, Function.identity(),
                            BinaryOperator.maxBy(Comparator.comparing(s -> s.getResult().getScore()))))
                    .values().stream();
        }

        if (userId == null) {
            List<String> studentUserIds = userRepository
                    .findAllByRolesContains(RolesConfig.ROLE_STUDENT).stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            stream = stream.filter(s -> studentUserIds.contains(s.getUserId()));
        }

        DoubleSummaryStatistics statistics = stream
                .map(Submission::getResult)
                .map(Result::getScore)
                .mapToDouble(Double::valueOf)
                .summaryStatistics();
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_SELF) or hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Submission> viewSubmission(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!submission.getUserId().equals(user.getId()) && user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_SUBMISSION_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(submission, HttpStatus.OK);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_CREATE)")
    public ResponseEntity<Submission> createSubmission(@RequestPart("assignmentId") String assignmentId,
                                                       @RequestPart(value = "token", required = false) String token,
                                                       @RequestPart("file") MultipartFile file) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Assignment assignment = assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (file.getSize() > assignment.getSubmitFileSize() * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交的文件超过作业限制的文件大小。");
        } else if (!Objects.equals("." + FilenameUtils.getExtension(file.getOriginalFilename()), assignment.getSubmitFileType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交的文件类型不符合作业限制的文件类型。");
        } else if (token == null && (new Date()).after(assignment.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交时间晚于作业截止时间。");
        }

        String createdBy = null; // if submit with token, set to issuer user info, else null
        if (token != null) {
            Token example = new Token();
            example.setToken(token);
            example.setUserId(user.getId());
            example.setAssignmentId(assignment.getId());
            Token _token = tokenRepository.findOne(Example.of(example))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交密钥不合法。"));
            User issuer = userRepository.findById(_token.getIssuedBy())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交密钥不合法（内部错误）。"));
            logger.info(String.format("ConsumeToken %s", _token));
            createdBy = String.format("%s %s", issuer.getUsername(), issuer.getFullName());
            tokenRepository.delete(_token);
        } else {
            long limit = assignment.getSubmitCountLimit();
            if (limit > 0) {
                Submission submission = new Submission();
                submission.setUserId(user.getId());
                submission.setAssignmentId(assignment.getId());
                long count = submissionRepository.count(Example.of(submission));
                if (count >= limit) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交次数已达上限。");
                }
            } else if (limit == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "此作业不允许自行提交。");
            }
        }

        Submission example = new Submission();
        example.setUserId(user.getId());
        example.setAssignmentId(assignment.getId());
        long count = submissionRepository.count(Example.of(example));

        Submission submission = new Submission();
        submission.setUserId(user.getId());
        submission.setAssignmentId(assignment.getId());
        submission.setKey(String.format("submissions/%s-%s-%s%s", user.getUsername(),
                assignment.getSlug(), count, assignment.getSubmitFileType()));
        submission.setCreatedAt(new Date());
        submission.setCreatedBy(createdBy);
        submission.setGraded(false);
        submission.setResult(null);
        submission = submissionRepository.save(submission);

        try {
            String key = submission.getKey();
            software.amazon.awssdk.core.sync.RequestBody requestBody =
                    software.amazon.awssdk.core.sync.RequestBody.fromInputStream(file.getInputStream(), file.getSize());
            s3Client.putObject(builder -> builder.bucket(s3Bucket).key(key).build(), requestBody);
        } catch (S3Exception | IOException e) {
            logger.error(String.format("CreateSubmission failed: %s %s by %s", e.getMessage(), submission, user), e);
            submissionRepository.delete(submission);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取或存储提交文件。");
        }
        logger.info(String.format("CreateSubmission %s by %s", submission, user));

        int priority = GradeSubmissionTask.PRIORITY_NORM;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 12);
        if (calendar.getTime().after(assignment.getEndTime())) {
            priority = GradeSubmissionTask.PRIORITY_HIGH;
        }
        GradeSubmissionTask task = new GradeSubmissionTask(assignment, submission, submissionRepository,
                s3Bucket, s3Client, dockerConfig.getInstance(), priority);
        gradeSubmissionExecutor.execute(task, AsyncTaskExecutor.TIMEOUT_IMMEDIATE);
        return new ResponseEntity<>(submission, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_UPDATE)")
    public ResponseEntity<Submission> updateSubmission(@PathVariable String id,
                                                       @RequestBody Submission updatedSubmission) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        submission.setGraded(updatedSubmission.getGraded());
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

        try {
            String key = submission.getKey();
            s3Client.deleteObject(builder -> builder.bucket(s3Bucket).key(key).build());
        } catch (S3Exception e) {
            logger.error(String.format("DeleteSubmission failed: %s %s by %s", e.getMessage(), submission, user), e);
        }

        logger.info(String.format("DeleteSubmission %s by %s", submission, user));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_SELF) or hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!user.getId().equals(submission.getUserId()) && user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_SUBMISSION_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            String key = submission.getKey();
            InputStream stream = s3Client.getObject(builder -> builder.bucket(s3Bucket).key(key).build());
            InputStreamResource resource = new InputStreamResource(stream);
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } catch (S3Exception e) {
            logger.error(String.format("DownloadSubmission failed: %s %s by %s", e.getMessage(), submission, user), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取提交文件。");
        }
    }

    @PostMapping("/rejudge")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_UPDATE)")
    public ResponseEntity<List<SubmissionInfo>> rejudgeSubmissions(@RequestBody Submission example) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<Submission> submissions = submissionRepository.findAll(Example.of(example));
        for (Submission submission : submissions) {
            submission.setGraded(false);
            submission.setResult(null);
            submissionRepository.save(submission);

            logger.info(String.format("RejudgeSubmission %s by %s", submission, user));
            Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
            GradeSubmissionTask task = new GradeSubmissionTask(assignment, submission, submissionRepository,
                    s3Bucket, s3Client, dockerConfig.getInstance(), GradeSubmissionTask.PRIORITY_LOW);
            gradeSubmissionExecutor.execute(task, AsyncTaskExecutor.TIMEOUT_IMMEDIATE);
        }

        List<SubmissionInfo> infos = submissions.stream()
                .map(s -> projectionFactory.createProjection(SubmissionInfo.class, s))
                .collect(Collectors.toList());
        return new ResponseEntity<>(infos, HttpStatus.OK);
    }

}
