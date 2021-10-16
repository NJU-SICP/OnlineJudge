package cn.edu.nju.sicp.contests.hog;

import cn.edu.nju.sicp.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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

    private final MongoOperations mongo;
    public final Logger logger = LoggerFactory.getLogger(getClass());

    public HogController(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @GetMapping("/submission")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HogEntry> getSubmission() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        HogEntry entry = mongo.findOne(Query.query(Criteria.where("userId").is(user.getId())
                .andOperator(Criteria.where("valid").is(true))), HogEntry.class);
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(entry, HttpStatus.OK);
    }

    @GetMapping("/scoreboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HogEntry>> getScoreboard() {
        List<HogEntry> entries = mongo.find(Query.query(Criteria.where("valid").is(true)), HogEntry.class);
        return new ResponseEntity<>(entries, HttpStatus.OK);
    }

}
