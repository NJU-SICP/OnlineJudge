package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.configs.RolesConfig;
import cn.edu.nju.sicp.dtos.SubmissionInfo;
import cn.edu.nju.sicp.dtos.UserInfo;
import cn.edu.nju.sicp.models.*;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import cn.edu.nju.sicp.repositories.TokenRepository;
import cn.edu.nju.sicp.repositories.UserRepository;
import cn.edu.nju.sicp.services.SubmissionService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionService service;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final TokenRepository tokenRepository;
    private final ProjectionFactory projectionFactory;
    private final Logger logger;

    public SubmissionController(SubmissionService service, UserRepository userRepository,
                                AssignmentRepository assignmentRepository, SubmissionRepository submissionRepository,
                                TokenRepository tokenRepository, ProjectionFactory projectionFactory) {
        this.service = service;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.tokenRepository = tokenRepository;
        this.projectionFactory = projectionFactory;
        this.logger = LoggerFactory.getLogger(SubmissionController.class);
    }

    @GetMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_SELF) or hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Page<SubmissionInfo>> listSubmissions(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String assignmentId,
            @RequestParam(required = false) Boolean graded,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.getId().equals(userId) && user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_SUBMISSION_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Assignment assignment =
                assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId).orElse(null);

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
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_SELF) or hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Long> countSubmissions(@RequestParam(required = false) String userId,
                                                 @RequestParam(required = false) String assignmentId,
                                                 @RequestParam(required = false) Boolean graded) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.getId().equals(userId) && user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_SUBMISSION_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Assignment assignment =
                assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId).orElse(null);
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
        Assignment assignment =
                assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId).orElse(null);
        Submission submission = new Submission();
        submission.setAssignmentId(assignment == null ? null : assignment.getId());
        List<String> userIds = submissionRepository.findAll(Example.of(submission)).stream()
                .map(Submission::getUserId).collect(Collectors.toList());
        Page<User> users;
        PageRequest pageRequest = PageRequest.of(page == null || page < 0 ? 0 : page,
                size == null || size < 0 ? 20 : size, Sort.by(Sort.Direction.ASC, "id"));
        if (submitted) {
            users = userRepository.findAllByIdInAndRolesContains(userIds, role, pageRequest);
        } else {
            users = userRepository.findAllByIdNotInAndRolesContains(userIds, role, pageRequest);
        }
        Page<UserInfo> infos =
                users.map(u -> projectionFactory.createProjection(UserInfo.class, u));
        return new ResponseEntity<>(infos, HttpStatus.OK);
    }

    @GetMapping("/users/count")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Long> countUsers(@RequestParam String assignmentId,
                                           @RequestParam Boolean submitted, @RequestParam String role) {
        Assignment assignment =
                assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId).orElse(null);
        Submission submission = new Submission();
        submission.setAssignmentId(assignment == null ? null : assignment.getId());
        List<String> userIds = submissionRepository.findAll(Example.of(submission)).stream()
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
    public ResponseEntity<DoubleSummaryStatistics> getAverageScore(
            @RequestParam String assignmentId, @RequestParam(required = false) String userId,
            @RequestParam(required = false) Boolean unique) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userId != null && !userId.equals(user.getId()) && user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_SUBMISSION_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Assignment assignment =
                assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId).orElse(null);

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setAssignmentId(assignment == null ? null : assignment.getId());

        Stream<Submission> stream;
        if (unique == null || !unique) {
            stream = submissionRepository.findAll(Example.of(submission)).stream()
                    .filter(s -> s.getResult() != null && s.getResult().getScore() != null);
        } else {
            stream = submissionRepository.findAll(Example.of(submission)).stream()
                    .filter(s -> s.getResult() != null && s.getResult().getScore() != null)
                    .collect(Collectors.toMap(Submission::getUserId, Function.identity(),
                            BinaryOperator
                                    .maxBy(Comparator.comparing(s -> s.getResult().getScore()))))
                    .values().stream();
        }

        if (userId == null) {
            List<String> studentUserIds =
                    userRepository.findAllByRolesContains(RolesConfig.ROLE_STUDENT).stream()
                            .map(User::getId).collect(Collectors.toList());
            stream = stream.filter(s -> studentUserIds.contains(s.getUserId()));
        }

        DoubleSummaryStatistics statistics = stream.map(Submission::getResult).map(Result::getScore)
                .mapToDouble(Double::valueOf).summaryStatistics();
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_SELF) or hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Submission> viewSubmission(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_SUBMISSION_READ_ALL))) {
            if (!submission.getUserId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            submission.getResult().setLog(null); // hide log from students
        }
        return new ResponseEntity<>(submission, HttpStatus.OK);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_CREATE)")
    public ResponseEntity<Submission> createSubmission(
            @RequestPart("assignmentId") String assignmentId,
            @RequestPart(value = "token", required = false) String token,
            @RequestPart("file") MultipartFile file) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Assignment assignment = assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (file.getSize() > assignment.getSubmitFileSize() * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交的文件超过作业限制的文件大小。");
        } else if (!Objects.equals("." + FilenameUtils.getExtension(file.getOriginalFilename()),
                assignment.getSubmitFileType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交的文件类型不符合作业限制的文件类型。");
        }

        String createdBy = null; // if submit with token, set to issuer user info, else null
        if (token != null) {
            Token example = new Token();
            example.setToken(token);
            example.setUserId(user.getId());
            example.setAssignmentId(assignment.getId());
            Token _token = tokenRepository.findOne(Example.of(example)).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交密钥不合法。"));
            User issuer = userRepository.findById(_token.getIssuedBy()).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交密钥不合法（内部错误）。"));
            logger.info(String.format("ConsumeToken %s", _token));
            createdBy = String.format("%s %s", issuer.getUsername(), issuer.getFullName());
            tokenRepository.delete(_token);
        } else if (user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_SUBMISSION_UPDATE))) {
            Date now = new Date();
            if (now.before(assignment.getBeginTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交时间早于作业开始时间。");
            } else if (now.after(assignment.getEndTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交时间晚于作业截止时间。");
            }
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
        submission.setKey(String.format("submissions/%s/%s/%s%s", assignment.getSlug(),
                user.getUsername(), count + 1, assignment.getSubmitFileType()));
        submission.setCreatedAt(new Date());
        submission.setCreatedBy(createdBy);
        submission.setGraded(false);
        submission.setResult(null);
        submission = submissionRepository.save(submission);

        try {
            service.uploadSubmissionFile(submission, file.getInputStream(), file.getSize());
            service.sendGradeSubmissionMessage(submission);
            logger.info(String.format("CreateSubmission %s %s", submission, user));
        } catch (Exception e) {
            submissionRepository.delete(submission);
            logger.error(String.format("%s %s %s", e.getMessage(), submission, user), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取或存储提交文件。");
        }
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
        logger.info(String.format("UpdateSubmission %s %s", submission, user));
        return new ResponseEntity<>(submission, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_DELETE)")
    public ResponseEntity<Void> deleteSubmission(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            logger.info(String.format("DeleteSubmission %s %s", submission, user));
            submissionRepository.delete(submission);
            service.deleteSubmissionFile(submission);
        } catch (Exception e) {
            logger.error(String.format("%s %s %s", e.getMessage(), submission, user), e);
        }
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
            InputStream stream = service.getSubmissionFile(submission);
            Resource resource = new InputStreamResource(stream);
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } catch (Exception e) {
            logger.error(String.format("%s %s %s", e.getMessage(), submission, user), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取提交文件。");
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_READ_ALL)")
    public ResponseEntity<Resource> exportSubmissions(@RequestParam String assignmentId) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            byte[] bytes = service.exportSubmissions(assignment.getId());
            Resource resource = new ByteArrayResource(bytes);
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } catch (Exception e) {
            logger.error(String.format("%s %s %s", e.getMessage(), assignmentId, user), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法导出提交文件。");
        }
    }

    @PostMapping("/rejudge")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_UPDATE)")
    public ResponseEntity<List<SubmissionInfo>> rejudgeSubmissions(
            @RequestBody Submission example) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<Submission> submissions = submissionRepository.findAll(Example.of(example));
        logger.info(String.format("RejudgeSubmissions size=%d criteria=%s %s", submissions.size(), example, user));
        for (Submission submission : submissions) {
            service.rejudgeSubmission(submission);
        }

        List<SubmissionInfo> infos = submissions.stream()
                .map(s -> projectionFactory.createProjection(SubmissionInfo.class, s))
                .collect(Collectors.toList());
        return new ResponseEntity<>(infos, HttpStatus.OK);
    }

}
