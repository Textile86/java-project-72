package hexlet.code.dto;

import hexlet.code.model.Url;
import hexlet.code.dto.BasePage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UrlPage extends BasePage {
    private Url url;
}
