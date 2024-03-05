package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.List;
@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Query(value = "SELECT * from indexes where lemma_id LIKE %:lemma_id% AND page_id LIKE %:page_id%", nativeQuery = true)
    Index findAllContains(int lemma_id, int page_id);
    List<Index> findByPage_id(Integer page_id);
    List<Index> findByLemma_id(Integer lemma_id);
}
