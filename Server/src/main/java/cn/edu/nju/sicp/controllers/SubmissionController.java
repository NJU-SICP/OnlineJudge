package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Submission;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import cn.edu.nju.sicp.repositories.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.Objects;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    private final Logger logger;

    public SubmissionController() {
        logger = LoggerFactory.getLogger(SubmissionController.class);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<String> createSubmission(@RequestPart("assignmentId") String assignmentId,
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
        submission.setResults(null);
        submission.setCreatedAt(new Date());
        submission.setCreatedBy(null);
        submission = submissionRepository.save(submission);
        logger.info(String.format("CreateSubmission %s", submission));

        try {
            String path = String.format("D:\\Temp\\%s", submission.getId());
            String name = String.format("submit%s", assignment.getSubmitFileType());
            if (Files.notExists(Paths.get(path))) {
                Files.createDirectories(Paths.get(path));
            }
            file.transferTo(Paths.get(path, name));
        } catch (IOException e) {
            logger.error("Cannot save submission file: " + e.getMessage());
            submissionRepository.delete(submission);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法存储提交文件。");
        }

        String response = (new ObjectMapper()).writeValueAsString(submission);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();
        User user = userRepository.findByUsername(username);

        Submission submission = submissionRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!(user.getRing() < 3 || user.getId().equals(submission.getUserId()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        try {
            Path path = Paths.get(String.format("D:\\Temp\\%s", submission.getId()),
                    String.format("submit%s", assignment.getSubmitFileType()));
            InputStreamResource resource = new InputStreamResource(new FileInputStream(path.toFile()));
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Cannot load submission file: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取提交文件。");
        }
    }
}
