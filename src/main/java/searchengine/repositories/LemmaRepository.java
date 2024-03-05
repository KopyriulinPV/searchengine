package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;


import java.util.List;
@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Query(value = "SELECT * from lemmas where lemma LIKE %:wordPart% AND site_id LIKE %:id%", nativeQuery = true)
    List<Lemma> findAllContains(String wordPart, int id);
    List<Lemma> findBySite_id(int site_id);

    List<Lemma> findByLemma(String lemma);

   }
