package asia.itsec.article.api;

import asia.itsec.article.application.ArticleService;
import asia.itsec.article.application.CreateArticleCommand;
import asia.itsec.article.application.UpdateArticleCommand;
import asia.itsec.article.domain.Article;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

    @Mock private ArticleService articleService;

    private ArticleController controller;

    @BeforeEach
    void setUp() {
        controller = new ArticleController(articleService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Article article() {
        return Article.builder().id("article-1").title("T").content("C").authorId("author-1")
                .isPublic(true).createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    @Test
    void getAllArticles_returnsOkWithList() {
        when(articleService.getAllArticles()).thenReturn(List.of(article()));

        ResponseEntity<?> response = controller.getAllArticles();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getArticleById_returnsOkWithArticle() {
        when(articleService.getArticleById("article-1")).thenReturn(article());

        ResponseEntity<?> response = controller.getArticleById("article-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void createArticle_usesAuthenticatedUserAsAuthor_returnsCreated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("author-1", null, List.of()));

        CreateArticleCommand command = new CreateArticleCommand();
        when(articleService.createArticle(eq(command), eq("author-1"))).thenReturn(article());

        ResponseEntity<?> response = controller.createArticle(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(articleService).createArticle(command, "author-1");
    }

    @Test
    void updateArticle_returnsOkWithUpdatedArticle() {
        UpdateArticleCommand command = new UpdateArticleCommand();
        when(articleService.updateArticle(eq("article-1"), any())).thenReturn(article());

        ResponseEntity<?> response = controller.updateArticle("article-1", command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteArticle_returnsNoContent() {
        ResponseEntity<Void> response = controller.deleteArticle("article-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(articleService).deleteArticle("article-1");
    }
}
