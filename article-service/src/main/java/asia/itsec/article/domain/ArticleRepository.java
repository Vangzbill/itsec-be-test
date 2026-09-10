package asia.itsec.article.domain;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository {
    Article save(Article article);
    List<Article> findAll();
    Optional<Article> findById(String id);
    void deleteById(String id);
}
