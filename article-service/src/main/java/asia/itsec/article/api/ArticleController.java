package asia.itsec.article.api;

import asia.itsec.article.application.ArticleService;
import asia.itsec.article.application.CreateArticleCommand;
import asia.itsec.article.application.UpdateArticleCommand;
import asia.itsec.article.domain.Article;
import asia.itsec.shared.payload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Article>>> getAllArticles() {
        return ResponseEntity.ok(ApiResponse.<List<Article>>builder()
                .success(true)
                .message("Articles retrieved successfully")
                .data(articleService.getAllArticles())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Article>> getArticleById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<Article>builder()
                .success(true)
                .message("Article retrieved successfully")
                .data(articleService.getArticleById(id))
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Article>> createArticle(@Valid @RequestBody CreateArticleCommand command) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String authorId = auth.getName();
        
        Article article = articleService.createArticle(command, authorId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Article>builder()
                .success(true)
                .message("Article created successfully")
                .data(article)
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Article>> updateArticle(@PathVariable String id, @Valid @RequestBody UpdateArticleCommand command) {
        Article article = articleService.updateArticle(id, command);
        
        return ResponseEntity.ok(ApiResponse.<Article>builder()
                .success(true)
                .message("Article updated successfully")
                .data(article)
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteArticle(@PathVariable String id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}
