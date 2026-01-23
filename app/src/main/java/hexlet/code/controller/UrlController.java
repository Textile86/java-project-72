package hexlet.code.controller;

import hexlet.code.dto.BuildUrlPage;
import hexlet.code.dto.UrlPage;
import hexlet.code.dto.UrlsPage;
import hexlet.code.model.NamedRoutes;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.javalin.rendering.template.TemplateUtil.model;

public class UrlController {
    public static void build(Context ctx) {
        var page = new BuildUrlPage();
        ctx.render("urls/build.jte", model("page", page));
    }

    public static void create(Context ctx) throws SQLException {
        String inputUrl = ctx.formParam("url");

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

        Url newUrl = new Url(normalizedUrl);
        UrlRepository.save(newUrl);

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.redirect(NamedRoutes.urlsPath());
    }

    public static void index(Context ctx) throws SQLException {
        var term = ctx.queryParam("term");
        var header = "Сайты";

        List<Url> allUrls = UrlRepository.getEntities();
        Map<Long, UrlCheck> latestChecks = new HashMap<>();
        for (Url url : allUrls) {
            Optional<UrlCheck> check = UrlCheckRepository.findLatestByUrlId(url.getId());
            check.ifPresent(c -> latestChecks.put(url.getId(), c));
        }

        var page = new UrlsPage(allUrls, latestChecks, header, term);
        String flashSuccess = ctx.consumeSessionAttribute("flash");
        String flashError = ctx.consumeSessionAttribute("flash-error");

        if (flashSuccess != null) {
            page.setFlash(flashSuccess);
        } else if (flashError != null) {
            page.setFlashError(flashError);
        }
        ctx.render("urls/index.jte", model("page", page));
    }

    public static void show(Context ctx) throws SQLException {
        var id = Long.parseLong(ctx.pathParam("id"));
        Optional<Url> url = Optional.ofNullable(UrlRepository.find(id)
                .orElseThrow(() -> new NotFoundResponse("URL не найден")));

        List<UrlCheck> checks = UrlCheckRepository.findByUrlId(id);
        var page = new UrlPage(url.get(), checks);
        String flashSuccess = ctx.consumeSessionAttribute("flash");
        String flashError = ctx.consumeSessionAttribute("flash-error");
        if (flashSuccess != null) {
            page.setFlashSuccess(flashSuccess);
        } else if (flashError != null) {
            page.setFlashError(flashError);
        }
        ctx.render("urls/show.jte", model("page", page));
    }
}
