package asia.itsec.article.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateArticleCommand {
    @NotBlank
    @Size(max = 255) // matches the varchar(255) title column
    private String title;
    @NotBlank
    @Size(max = 50000)
    private String content;

    private boolean isPublic = true;
}
