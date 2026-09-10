package asia.itsec.article.application;

import asia.itsec.article.domain.Article;
import asia.itsec.article.domain.ArticleRepository;
import asia.itsec.article.domain.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final EventPublisher eventPublisher;

    public Article createArticle(CreateArticleCommand command, String authorId) {
        Article article = Article.builder()
                .id(UUID.randomUUID().toString())
                .title(command.getTitle())
                .content(command.getContent())
                .authorId(authorId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return articleRepository.save(article);
    }

    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }

    public Article getArticleById(String id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
    }

    public Article updateArticle(String id, UpdateArticleCommand command) {
        Article existing = getArticleById(id);
        
        Article updated = Article.builder()
                .id(existing.getId())
                .title(command.getTitle())
                .content(command.getContent())
                .authorId(existing.getAuthorId())
                .createdAt(existing.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
                
        return articleRepository.save(updated);
    }

    public void deleteArticle(String id) {
        Article existing = getArticleById(id);
        articleRepository.deleteById(existing.getId());
        eventPublisher.publishArticleDeletedEvent(existing.getId());
    }
}
