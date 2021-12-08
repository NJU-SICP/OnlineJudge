package cn.edu.nju.sicp.services;

import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Plagiarism;
import cn.edu.nju.sicp.models.User;
import org.springframework.stereotype.Service;

import java.util.DoubleSummaryStatistics;
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

    public DoubleSummaryStatistics getStatistics(User user, Assignment assignment) {
        DoubleSummaryStatistics statistics = submissionService.getSubmissionStatistics(user, assignment);
        Optional<Plagiarism> optionalPlagiarism = plagiarismService.findPlagiarismByUser(user, assignment);
        if (optionalPlagiarism.isEmpty()) {
            return statistics;
        } else {
            Integer score = optionalPlagiarism.get().getScore();
            double s = score == null ? 0.0f : (double) score;
            long c = statistics.getCount();
            return new DoubleSummaryStatistics(c, s, s, s * c);
        }
    }

}
