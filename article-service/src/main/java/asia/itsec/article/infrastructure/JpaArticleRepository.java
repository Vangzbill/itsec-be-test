package asia.itsec.article.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaArticleRepository extends JpaRepository<ArticleEntity, String> {
}
