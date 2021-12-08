package cn.edu.nju.sicp.services;

import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Plagiarism;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.PlagiarismRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlagiarismService {

    private final PlagiarismRepository plagiarismRepository;

    public PlagiarismService(PlagiarismRepository plagiarismRepository) {
        this.plagiarismRepository = plagiarismRepository;
    }

    public Page<Plagiarism> listPlagiarisms(PageRequest request) {
        return plagiarismRepository.findAll(request);
    }

    public Optional<Plagiarism> findPlagiarismById(String id) {
        return plagiarismRepository.findById(id);
    }

    public Optional<Plagiarism> findPlagiarismByUser(User user, Assignment assignment) {
        Plagiarism example = new Plagiarism();
        example.setUserId(user.getId());
        example.setAssignmentId(assignment.getId());
        return plagiarismRepository.findOne(Example.of(example));
    }

    public Plagiarism savePlagiarism(Plagiarism plagiarism) {
        return plagiarismRepository.save(plagiarism);
    }

    public void deletePlagiarism(Plagiarism plagiarism) {
        plagiarismRepository.delete(plagiarism);
    }

}
