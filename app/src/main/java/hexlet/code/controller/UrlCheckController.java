package hexlet.code.controller;

import hexlet.code.dto.UrlPage;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static io.javalin.rendering.template.TemplateUtil.model;

public class UrlCheckController {
    public static void create(Context ctx) throws SQLException {
        try {
            Long urlId = Long.parseLong(ctx.pathParam("id"));
            Optional<Url> url = UrlRepository.find(urlId);
            if (url.isEmpty()) {
                ctx.status(404).result("URL Not Found");
            }
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

                Timestamp now = new Timestamp(System.currentTimeMillis());
                UrlCheck check = new UrlCheck(urlId, statusCode, title, h1, description, now);

                UrlCheckRepository.save(check);
                ctx.sessionAttribute("flash", "Страница успешно проверена");
            } catch (Exception e) {
                ctx.sessionAttribute("flash-error", "Некорректный адрес");
            }
            ctx.redirect("/urls/" + urlId);
        } catch (Exception e) {
            ctx.status(500).result("Server error" + e.getMessage());
        }
    }

    public static void show(Context ctx) throws SQLException {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Url> url = UrlRepository.find(id);
            if (url.isPresent()) {
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
            } else  {
                ctx.status(404).result("URL Not Found");
            }
        } catch (Exception e) {
            ctx.status(500).result("Ошибка базы данных" + e.getMessage());
        }
    }
}
