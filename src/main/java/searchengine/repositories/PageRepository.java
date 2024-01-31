package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.List;
import java.util.Set;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Page findByPath(String path);

}
