package hexlet.code.dto;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import io.javalin.validation.ValidationError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class BuildUrlPage extends BasePage {
    private String name;
    private Timestamp created_at;
    private Map<String, List<ValidationError<Object>>> errors;
}
