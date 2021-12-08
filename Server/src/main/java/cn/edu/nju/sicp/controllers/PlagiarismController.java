package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Plagiarism;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.repositories.UserRepository;
import cn.edu.nju.sicp.services.PlagiarismService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/plagiarisms")
public class PlagiarismController {

    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final PlagiarismService plagiarismService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PlagiarismController(UserRepository userRepository,
                                AssignmentRepository assignmentRepository,
                                PlagiarismService plagiarismService) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.plagiarismService = plagiarismService;
    }

    @GetMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_PLAGIARISM_READ_ALL)")
    public ResponseEntity<Page<Plagiarism>> listPlagiarisms(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageRequest request = PageRequest.of(
                page == null || page < 0 ? 0 : page,
                size == null || size < 0 ? 20 : size,
                Sort.by(Sort.Direction.DESC, "$natural"));
        Page<Plagiarism> plagiarisms = plagiarismService.listPlagiarisms(request);
        return new ResponseEntity<>(plagiarisms, HttpStatus.OK);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority(@Roles.OP_PLAGIARISM_READ_SELF)")
    public ResponseEntity<Plagiarism> findMyPlagiarism(@RequestParam String assignmentId) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Plagiarism plagiarism = plagiarismService.findPlagiarismByUser(user, assignment).orElse(null);
        return new ResponseEntity<>(plagiarism, HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_PLAGIARISM_CREATE)")
    public ResponseEntity<Plagiarism> createPlagiarism(@RequestBody Plagiarism createdPlagiarism) {
        User user = userRepository.findById(createdPlagiarism.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Assignment assignment = assignmentRepository.findById(createdPlagiarism.getAssignmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Plagiarism plagiarism = plagiarismService.findPlagiarismByUser(user, assignment).orElseGet(Plagiarism::new);
        plagiarism.setUserId(user.getId());
        plagiarism.setAssignmentId(assignment.getId());
        plagiarism.setDetail(createdPlagiarism.getDetail());
        plagiarism.setScore(createdPlagiarism.getScore());
        plagiarism = plagiarismService.savePlagiarism(plagiarism);
        User admin = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        logger.info(String.format("CreatePlagiarism %s %s", plagiarism, admin));
        return new ResponseEntity<>(plagiarism, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_PLAGIARISM_DELETE)")
    public ResponseEntity<Void> deletePlagiarism(@PathVariable String id) {
        Plagiarism plagiarism = plagiarismService.findPlagiarismById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User admin = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        plagiarismService.deletePlagiarism(plagiarism);
        logger.info(String.format("DeletePlagiarism %s %s", plagiarism, admin));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
