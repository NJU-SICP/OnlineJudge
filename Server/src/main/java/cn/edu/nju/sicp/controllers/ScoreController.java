package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.configs.RolesConfig;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.repositories.UserRepository;
import cn.edu.nju.sicp.services.ScoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.DoubleSummaryStatistics;

@RestController
@RequestMapping("/scores")
public class ScoreController {

    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final ScoreService scoreService;

    public ScoreController(UserRepository userRepository,
                           AssignmentRepository assignmentRepository,
                           ScoreService scoreService) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.scoreService = scoreService;
    }

    @GetMapping("/single")
    @PreAuthorize("hasAuthority(@Roles.OP_SCORE_READ_SELF) or hasAuthority(@Roles.OP_SCORE_READ_ALL)")
    public ResponseEntity<DoubleSummaryStatistics> getSingle(
            @RequestParam(required = false) String userId,
            @RequestParam String assignmentId) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userId != null) {
            if (!userId.equals(user.getId()) && user.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_SUBMISSION_READ_ALL))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        DoubleSummaryStatistics statistics = scoreService.getStatistics(user, assignment);
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }

}
