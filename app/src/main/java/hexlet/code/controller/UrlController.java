package hexlet.code.controller;

import hexlet.code.dto.BuildUrlPage;
import hexlet.code.dto.UrlPage;
import hexlet.code.dto.UrlsPage;
import hexlet.code.model.NamedRoutes;
import hexlet.code.model.Url;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.validation.ValidationError;
import io.javalin.validation.ValidationException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.rendering.template.TemplateUtil.model;

public class UrlController {
    public static void build(Context ctx) {
        var page = new BuildUrlPage();
        ctx.render("urls/build.jte", model("page", page));
    }

    public static void create(Context ctx) {
        String inputUrl = ctx.formParam("url");

        try {
            if (inputUrl == null || inputUrl.trim().isEmpty()) {
                ctx.sessionAttribute("flash-error", "URL не может быть пустым");
                ctx.render("/");
                return;
            }

            URI uri;
            try {
                uri = new URI(inputUrl.trim());
            } catch (URISyntaxException e) {
                ctx.sessionAttribute("flash-error", "Некорректный URL");
                ctx.redirect("/");
                return;
            }

            if (!uri.isAbsolute()) {
                ctx.sessionAttribute("flash-error", "Некорректный URL");
                ctx.redirect("/");
                return;
            }

            URL url;
            try {
                url = uri.toURL();
            } catch (MalformedURLException e) {
                ctx.sessionAttribute("flash-error", "Некорректный URL");
                ctx.redirect("/");
                return;
            }

            String protocol = url.getProtocol().toLowerCase();
            String host = url.getHost().toLowerCase();
            int port = url.getPort();
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            if (host == null) {
                ctx.sessionAttribute("flash-error", "Некорректный URL");
                ctx.redirect("/");
                return;
            }

            String normalizedUrl = protocol + "://" + host + (port != -1 ? ":" + port : "");

            List<Url> existingUrls = UrlRepository.getEntities();
            boolean exists = existingUrls.stream()
                    .anyMatch(u -> u.getName().equalsIgnoreCase(normalizedUrl));

            if (exists) {
                ctx.sessionAttribute("flash-error", "Страница уже существует");
                ctx.redirect(NamedRoutes.urlsPath());
                return;
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            Url newUrl = new Url(normalizedUrl, now);
            UrlRepository.save(newUrl);

            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.redirect(NamedRoutes.urlsPath());

        } catch (SQLException e) {
            ctx.sessionAttribute("flash-error", "Ошибка базы данных: " + e.getMessage());
            ctx.redirect("/");
        } catch (Exception e) {
            ctx.sessionAttribute("flash-error", "Некорректный URL");
            ctx.redirect("/");
        }
    }

    public static void index(Context ctx) {
        try {
            var term = ctx.queryParam("term");
            var header = "Сайты";

            List<Url> filteredUrls;
            List<Url> allUrls = UrlRepository.getEntities();

            if (term != null && !term.trim().isEmpty()) {
                String searchTerm = term.toLowerCase().trim();
                filteredUrls = allUrls.stream()
                        .filter(url ->
                                url.getName().toLowerCase().contains(searchTerm))
                        .collect(Collectors.toList());
            } else {
                filteredUrls = allUrls;
            }

            var page = new UrlsPage(filteredUrls, header, term);
            String flashSuccess = ctx.consumeSessionAttribute("flash");
            String flashError = ctx.consumeSessionAttribute("flash-error");

            if (flashSuccess != null) {
                page.setFlashSuccess(flashSuccess);
            } else if (flashError != null) {
                page.setFlashError(flashError);
            }
            ctx.render("urls/index.jte", model("page", page));
        } catch (SQLException e) {
            var page = new UrlsPage(List.of(), "Сайты", null);
            page.setFlashError("Не удалось загрузить сайты: " + e.getMessage());
            ctx.render("urls/index.jte", model("page", page));
        }
    }

    public static void show(Context ctx) {
        try {
            var id = Long.parseLong(ctx.pathParam("id"));
            Optional<Url> url = UrlRepository.find(id);

            if (url.isPresent()) {
                var page = new UrlPage(url.get());
                String flashSuccess = ctx.consumeSessionAttribute("flash");
                String flashError = ctx.consumeSessionAttribute("flash-error");
                if (flashSuccess != null) {
                    page.setFlashSuccess(flashSuccess);
                } else if (flashError != null) {
                    page.setFlashError(flashError);
                }
                ctx.render("urls/show.jte", model("page", page));
            } else {
                ctx.status(404).result("Url not found");
            }

        } catch (SQLException e) {
            ctx.status(500).result("Ошибка базы данных: " + e.getMessage());
        } catch (NumberFormatException e) {
            ctx.status(400).result("Неверный формат ID");
        }
    }

}
