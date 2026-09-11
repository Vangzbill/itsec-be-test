package asia.itsec.article.application;

import asia.itsec.article.domain.Article;
import asia.itsec.article.domain.ArticleRepository;
import asia.itsec.article.domain.EventPublisher;
import asia.itsec.shared.event.AuditEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String VIEWER = "ROLE_VIEWER";

    private final ArticleRepository articleRepository;
    private final EventPublisher eventPublisher;
    private final HttpServletRequest httpServletRequest;

    public Article createArticle(CreateArticleCommand command, String authorId) {
        Article article = Article.builder()
                .id(UUID.randomUUID().toString())
                .title(command.getTitle())
                .content(command.getContent())
                .authorId(authorId)
                .isPublic(command.isPublic())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Article saved = articleRepository.save(article);
        publishAudit("ARTICLE_CREATED", saved.getId());
        return saved;
    }

    public List<Article> getAllArticles() {
        List<Article> all = articleRepository.findAll();
        if (isViewerOrAnonymous()) {
            return all.stream().filter(Article::isPublic).collect(Collectors.toList());
        }
        return all;
    }

    public Article getArticleById(String id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));

        if (!article.isPublic() && isViewerOrAnonymous() && !isOwner(article)) {
            throw new AccessDeniedException("You cannot view this article");
        }
        return article;
    }

    public Article updateArticle(String id, UpdateArticleCommand command) {
        Article existing = getArticleById(id);
        requireOwnerOrSuperAdmin(existing, "You can only update your own articles");

        Article updated = Article.builder()
                .id(existing.getId())
                .title(command.getTitle())
                .content(command.getContent())
                .authorId(existing.getAuthorId())
                .isPublic(command.isPublic())
                .createdAt(existing.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        Article saved = articleRepository.save(updated);
        publishAudit("ARTICLE_UPDATED", saved.getId());
        return saved;
    }

    public void deleteArticle(String id) {
        Article existing = getArticleById(id);
        requireOwnerOrSuperAdmin(existing, "You can only delete your own articles");

        articleRepository.deleteById(existing.getId());
        publishAudit("ARTICLE_DELETED", existing.getId());
    }

    private void requireOwnerOrSuperAdmin(Article article, String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(SUPER_ADMIN));
        if (!isSuperAdmin && !isOwner(article)) {
            throw new AccessDeniedException(message);
        }
    }

    private boolean isOwner(Article article) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && article.getAuthorId().equals(auth.getName());
    }

    private boolean isViewerOrAnonymous() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true;
        }
        boolean hasElevatedRole = auth.getAuthorities().stream()
                .anyMatch(a -> !a.getAuthority().equals(VIEWER));
        return !hasElevatedRole;
    }

    private void publishAudit(String action, String articleId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorId = auth != null ? auth.getName() : null;
        String actorUsername = auth != null && auth.getDetails() != null ? auth.getDetails().toString() : null;

        eventPublisher.publish(AuditEvent.builder()
                .actorId(actorId)
                .actorUsername(actorUsername)
                .action(action)
                .entityType("ARTICLE")
                .entityId(articleId)
                .ipAddress(httpServletRequest.getRemoteAddr())
                .userAgent(httpServletRequest.getHeader("User-Agent"))
                .requestPath(httpServletRequest.getRequestURI())
                .httpMethod(httpServletRequest.getMethod())
                .status("SUCCESS")
                .createdAt(Instant.now())
                .build());
    }
}
