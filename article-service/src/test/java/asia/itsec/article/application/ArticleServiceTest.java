package asia.itsec.article.application;

import asia.itsec.article.domain.Article;
import asia.itsec.article.domain.ArticleRepository;
import asia.itsec.article.domain.EventPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleServiceTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private HttpServletRequest httpServletRequest;

    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        articleService = new ArticleService(articleRepository, eventPublisher, httpServletRequest);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("JUnit");
        when(httpServletRequest.getRequestURI()).thenReturn("/articles");
        when(httpServletRequest.getMethod()).thenReturn("POST");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String userId, String... roles) {
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Article article(String authorId, boolean isPublic) {
        return Article.builder()
                .id("article-1")
                .title("Title")
                .content("Content")
                .authorId(authorId)
                .isPublic(isPublic)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createArticle_success_publishesAuditEvent() {
        authenticateAs("author-1", "ROLE_EDITOR");
        CreateArticleCommand command = new CreateArticleCommand();
        command.setTitle("Title");
        command.setContent("Content");
        command.setPublic(true);

        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        Article result = articleService.createArticle(command, "author-1");

        assertThat(result.getAuthorId()).isEqualTo("author-1");
        assertThat(result.isPublic()).isTrue();
        verify(eventPublisher).publish(any());
    }

    @Test
    void getAllArticles_asViewer_returnsOnlyPublicArticles() {
        authenticateAs("viewer-1", "ROLE_VIEWER");
        when(articleRepository.findAll()).thenReturn(List.of(
                article("author-1", true),
                article("author-2", false)
        ));

        List<Article> result = articleService.getAllArticles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPublic()).isTrue();
    }

    @Test
    void getAllArticles_asEditor_returnsAllArticlesRegardlessOfVisibility() {
        authenticateAs("editor-1", "ROLE_EDITOR");
        when(articleRepository.findAll()).thenReturn(List.of(
                article("author-1", true),
                article("author-2", false)
        ));

        List<Article> result = articleService.getAllArticles();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllArticles_anonymous_returnsOnlyPublicArticles() {
        when(articleRepository.findAll()).thenReturn(List.of(
                article("author-1", true),
                article("author-2", false)
        ));

        List<Article> result = articleService.getAllArticles();

        assertThat(result).hasSize(1);
    }

    @Test
    void getArticleById_privateArticle_asViewerNonOwner_throwsAccessDenied() {
        authenticateAs("viewer-1", "ROLE_VIEWER");
        when(articleRepository.findById("article-1")).thenReturn(Optional.of(article("author-1", false)));

        assertThatThrownBy(() -> articleService.getArticleById("article-1"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getArticleById_privateArticle_asOwner_succeeds() {
        authenticateAs("author-1", "ROLE_VIEWER");
        when(articleRepository.findById("article-1")).thenReturn(Optional.of(article("author-1", false)));

        Article result = articleService.getArticleById("article-1");

        assertThat(result.getId()).isEqualTo("article-1");
    }

    @Test
    void getArticleById_notFound_throws() {
        when(articleRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.getArticleById("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateArticle_asOwner_succeeds() {
        authenticateAs("author-1", "ROLE_EDITOR");
        when(articleRepository.findById("article-1")).thenReturn(Optional.of(article("author-1", true)));
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateArticleCommand command = new UpdateArticleCommand();
        command.setTitle("New Title");
        command.setContent("New Content");
        command.setPublic(true);

        Article result = articleService.updateArticle("article-1", command);

        assertThat(result.getTitle()).isEqualTo("New Title");
        verify(eventPublisher).publish(any());
    }

    @Test
    void updateArticle_asNonOwnerEditor_throwsAccessDenied() {
        authenticateAs("someone-else", "ROLE_EDITOR");
        when(articleRepository.findById("article-1")).thenReturn(Optional.of(article("author-1", true)));

        UpdateArticleCommand command = new UpdateArticleCommand();
        command.setTitle("New Title");
        command.setContent("New Content");

        assertThatThrownBy(() -> articleService.updateArticle("article-1", command))
                .isInstanceOf(AccessDeniedException.class);
        verify(articleRepository, never()).save(any());
    }

    @Test
    void updateArticle_asSuperAdmin_succeedsEvenWhenNotOwner() {
        authenticateAs("admin-1", "ROLE_SUPER_ADMIN");
        when(articleRepository.findById("article-1")).thenReturn(Optional.of(article("author-1", true)));
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateArticleCommand command = new UpdateArticleCommand();
        command.setTitle("Admin Edit");
        command.setContent("Admin Content");

        Article result = articleService.updateArticle("article-1", command);

        assertThat(result.getTitle()).isEqualTo("Admin Edit");
    }

    @Test
    void deleteArticle_asOwner_succeeds() {
        authenticateAs("author-1", "ROLE_EDITOR");
        when(articleRepository.findById("article-1")).thenReturn(Optional.of(article("author-1", true)));

        articleService.deleteArticle("article-1");

        verify(articleRepository).deleteById("article-1");
        verify(eventPublisher).publish(any());
    }

    @Test
    void deleteArticle_asNonOwnerEditor_throwsAccessDenied() {
        authenticateAs("someone-else", "ROLE_EDITOR");
        when(articleRepository.findById("article-1")).thenReturn(Optional.of(article("author-1", true)));

        assertThatThrownBy(() -> articleService.deleteArticle("article-1"))
                .isInstanceOf(AccessDeniedException.class);
        verify(articleRepository, never()).deleteById(any());
    }

    @Test
    void deleteArticle_asSuperAdmin_succeedsEvenWhenNotOwner() {
        authenticateAs("admin-1", "ROLE_SUPER_ADMIN");
        when(articleRepository.findById("article-1")).thenReturn(Optional.of(article("author-1", true)));

        articleService.deleteArticle("article-1");

        verify(articleRepository).deleteById("article-1");
    }
}
