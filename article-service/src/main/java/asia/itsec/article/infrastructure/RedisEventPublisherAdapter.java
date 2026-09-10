package asia.itsec.article.infrastructure;

import asia.itsec.article.domain.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisEventPublisherAdapter implements EventPublisher {

    private final StringRedisTemplate redisTemplate;
    private static final String CHANNEL = "ARTICLE_EVENTS";

    @Override
    public void publishArticleDeletedEvent(String articleId) {
        String payload = "{\"event\": \"ARTICLE_DELETED\", \"articleId\": \"" + articleId + "\"}";
        redisTemplate.convertAndSend(CHANNEL, payload);
    }
}
