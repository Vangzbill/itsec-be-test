package asia.itsec.article.domain;

public interface EventPublisher {
    void publishArticleDeletedEvent(String articleId);
}
