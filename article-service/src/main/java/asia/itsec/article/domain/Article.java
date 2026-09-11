package asia.itsec.article.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class Article {
    private String id;
    private String title;
    private String content;
    private String authorId;
    private boolean isPublic;
    private Instant createdAt;
    private Instant updatedAt;
}
