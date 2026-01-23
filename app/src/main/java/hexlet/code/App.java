package hexlet.code;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import hexlet.code.controller.UrlCheckController;
import hexlet.code.controller.UrlController;
import hexlet.code.dto.BasePage;
import hexlet.code.model.NamedRoutes;
import hexlet.code.repository.BaseRepository;
import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;
import io.javalin.rendering.template.JavalinJte;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.stream.Collectors;

import static io.javalin.rendering.template.TemplateUtil.model;


public class    App {
    public static Javalin getApp(String databaseUrl) throws SQLException {
        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(databaseUrl);

        var dataSource = new HikariDataSource(hikariConfig);

        var url = App.class.getClassLoader().getResourceAsStream("schema.sql");
        var sql = new BufferedReader(new InputStreamReader(url))
                .lines().collect(Collectors.joining("\n"));

        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }
        BaseRepository.dataSource = dataSource;

        var app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(createTemplateEngine()));
        });

        app.exception(NotFoundResponse.class, (e, ctx) -> {
            ctx.status(404);
            ctx.result("Страница не найдена: " + e.getMessage());
        });

        app.exception(SQLException.class, (e, ctx) -> {
            ctx.status(500);
            ctx.sessionAttribute("flash-error", "Ошибка базы данных: " + e.getMessage());
            ctx.redirect("/");
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.sessionAttribute("flash-error", "Произошла ошибка: " + e.getMessage());
            ctx.redirect("/");
        });

        app.get("/", ctx -> {
            String flashError = ctx.consumeSessionAttribute("flash-error");
            BasePage page = new BasePage();
            if (flashError != null) {
                page.setFlashError(flashError);
            }
            ctx.render("index.jte", model("page", page));
        });

        app.post(NamedRoutes.urlsPath(), UrlController::create);
        app.get(NamedRoutes.urlsPath(), UrlController::index);
        app.get(NamedRoutes.urlPath("{id}"), UrlController::show);
        app.post(NamedRoutes.urlPath("{id}") + "/checks", UrlCheckController::create);

        return app;
    }

    public static Javalin getApp() throws SQLException {
        return getApp(getDatabaseUrl());
    }

    public static void main(String[] args) throws SQLException {
        Javalin app = getApp();
        app.start(getPort());
    }

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "7070");
        return Integer.valueOf(port);
    }

    private static String getDatabaseUrl() {
        return System.getenv().getOrDefault("JDBC_DATABASE_URL", "jdbc:h2:mem:project;DB_CLOSE_DELAY=-1;");
    }

    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
        return templateEngine;
    }
}
