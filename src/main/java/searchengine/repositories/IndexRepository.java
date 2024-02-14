package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Index;
import searchengine.model.Lemma;

import java.util.List;

public interface IndexRepository extends JpaRepository<Index, Integer> {
    List<Index> findByPage_id(Integer page_id);

    List<Index> findByLemma_id(Integer lemma_id);
}
