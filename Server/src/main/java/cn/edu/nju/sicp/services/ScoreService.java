package cn.edu.nju.sicp.services;

import cn.edu.nju.sicp.configs.CacheConfig;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Plagiarism;
import cn.edu.nju.sicp.models.Statistics;
import cn.edu.nju.sicp.models.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ScoreService {

    private final SubmissionService submissionService;
    private final PlagiarismService plagiarismService;

    public ScoreService(SubmissionService submissionService,
                        PlagiarismService plagiarismService) {
        this.submissionService = submissionService;
        this.plagiarismService = plagiarismService;
    }

    // Cache results for efficiency. If submission or plagiarism is saved, evict caches.D
    @Cacheable(cacheNames = CacheConfig.SCORE_CACHE_NAME, key = "#user.id + '-' + #assignment.id")
    public Statistics getScoreStatistics(User user, Assignment assignment) {
        Statistics statistics = submissionService.getSubmissionStatistics(user, assignment);
        Optional<Plagiarism> optionalPlagiarism = plagiarismService.findPlagiarismByUser(user, assignment);
        if (optionalPlagiarism.isEmpty()) {
            return statistics;
        } else {
            Long count = statistics.getCount();
            Integer score = optionalPlagiarism.get().getScore();
            return new Statistics(count, score == null ? 0 : score);
        }
    }

}
