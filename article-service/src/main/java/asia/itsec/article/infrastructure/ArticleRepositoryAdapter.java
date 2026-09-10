package asia.itsec.article.infrastructure;

import asia.itsec.article.domain.Article;
import asia.itsec.article.domain.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ArticleRepositoryAdapter implements ArticleRepository {

    private final JpaArticleRepository jpaRepository;

    @Override
    public Article save(Article article) {
        return jpaRepository.save(ArticleEntity.fromDomain(article)).toDomain();
    }

    @Override
    public List<Article> findAll() {
        return jpaRepository.findAll().stream()
                .map(ArticleEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Article> findById(String id) {
        return jpaRepository.findById(id).map(ArticleEntity::toDomain);
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }
}
