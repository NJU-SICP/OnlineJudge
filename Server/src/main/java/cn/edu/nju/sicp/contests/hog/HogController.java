package cn.edu.nju.sicp.contests.hog;

import cn.edu.nju.sicp.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/contests/hog")
public class HogController {

    public final HogRepository hogRepository;
    public final Logger logger = LoggerFactory.getLogger(getClass());

    public HogController(HogRepository hogRepository) {
        this.hogRepository = hogRepository;
    }

    @GetMapping("/submission")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HogEntry> getSubmission() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        HogEntry example = new HogEntry();
        example.setUserId(user.getId());
        example.setValid(true);
        HogEntry entry = hogRepository.findOne(Example.of(example))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new ResponseEntity<>(entry, HttpStatus.OK);
    }

    @GetMapping("/scoreboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HogEntry>> getScoreboard() {
        HogEntry example = new HogEntry();
        example.setValid(true);
        List<HogEntry> entries = hogRepository.findAll(Example.of(example));
        return new ResponseEntity<>(entries, HttpStatus.OK);
    }

}
