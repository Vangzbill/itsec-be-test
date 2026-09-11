package asia.itsec.article.infrastructure;

import asia.itsec.article.domain.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleRepositoryAdapterTest {

    @Mock private JpaArticleRepository jpaRepository;

    private ArticleRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ArticleRepositoryAdapter(jpaRepository);
    }

    private ArticleEntity entity() {
        return ArticleEntity.builder().id("article-1").title("T").content("C").authorId("author-1")
                .isPublic(true).createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    @Test
    void save_mapsDomainToEntityAndBack() {
        Article article = Article.builder().id("article-1").title("T").content("C").authorId("author-1")
                .isPublic(true).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(jpaRepository.save(any(ArticleEntity.class))).thenReturn(entity());

        Article result = adapter.save(article);

        assertThat(result.getId()).isEqualTo("article-1");
    }

    @Test
    void findAll_mapsAllEntities() {
        when(jpaRepository.findAll()).thenReturn(List.of(entity()));

        List<Article> result = adapter.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void findById_found_mapsToDomain() {
        when(jpaRepository.findById("article-1")).thenReturn(Optional.of(entity()));

        Optional<Article> result = adapter.findById("article-1");

        assertThat(result).isPresent();
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(jpaRepository.findById("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    void deleteById_delegatesToJpa() {
        adapter.deleteById("article-1");
        verify(jpaRepository).deleteById("article-1");
    }
}
