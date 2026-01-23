package hexlet.code.controller;

import hexlet.code.dto.UrlPage;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static io.javalin.rendering.template.TemplateUtil.model;

public class UrlCheckController {
    public static void create(Context ctx) throws SQLException {
        Long urlId = Long.parseLong(ctx.pathParam("id"));
        Optional<Url> url = Optional.ofNullable(UrlRepository.find(urlId)
                .orElseThrow(() -> new NotFoundResponse("URL не найден")));
        String urlName = url.get().getName();
        try {
            HttpResponse<String> response = Unirest.get(urlName).asString();
            int statusCode = response.getStatus();
            String body = response.getBody();

            Document doc = Jsoup.parse(body);
            String title = doc.title();
            Element h1Element = doc.selectFirst("h1");
            String h1 = h1Element != null ? h1Element.text() : null;
            Element descElement = doc.selectFirst("meta[name=description]");
            String description = descElement != null ? descElement.attr("content") : null;

            UrlCheck check = new UrlCheck(urlId, statusCode, title, h1, description, LocalDateTime.now());

            UrlCheckRepository.save(check);
            ctx.sessionAttribute("flash", "Страница успешно проверена");
        } catch (Exception e) {
            ctx.sessionAttribute("flash-error", "Некорректный адрес");
        }
        ctx.redirect("/urls/" + urlId);
    }

    public static void show(Context ctx) throws SQLException {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Optional<Url> url = Optional.ofNullable(UrlRepository.find(id)
                .orElseThrow(() -> new NotFoundResponse("URL не найден")));

        List<UrlCheck> checks = UrlCheckRepository.findByUrlId(id);
        UrlPage page = new UrlPage(url.get(), checks);

        String flashSuccess = ctx.consumeSessionAttribute("flash");
        String flashError = ctx.consumeSessionAttribute("flash-error");

        if (flashSuccess != null) {
            page.setFlash(flashSuccess);
        }
        if (flashError != null) {
            page.setFlashError(flashError);
        }
        ctx.render("urls/show.jte", model("page", page));
    }
}
