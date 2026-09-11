package asia.itsec.article.application;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateArticleCommand {
    @NotBlank
    private String title;
    @NotBlank
    private String content;

    private boolean isPublic = true;
}
