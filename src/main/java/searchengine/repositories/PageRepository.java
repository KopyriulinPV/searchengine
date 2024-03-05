package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    List<Page> findByPath(String path);
    List<Page> findBySite_id(int site_id);

}
