package asia.itsec.article.application;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateArticleCommand {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
}
