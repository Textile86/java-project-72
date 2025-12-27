package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.repository.UrlRepository;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class AppTest {
    private Javalin app;

    @BeforeEach
    public final void setUp() throws IOException, SQLException {
        app = App.getApp();
        UrlRepository.deleteAll();
    }

    @Test
    public void testMainPage() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("Анализатор страниц");
        });
    }

    @Test
    public void testUrlsPage() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    public void testUrlPage() {
        JavalinTest.test(app, (server, client) -> {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Url url = new Url("https://ru.hexlet.io/projects/72/members/49091", now);
            UrlRepository.save(url);
            var response = client.get("/urls/" + url.getId());
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void testUrlNotFound() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls/999999");
            assertThat(response.code()).isEqualTo(404);
        });
    }

    @Test
    public void testCreateUrl() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            String testUrl = "https://example.com";
            assertThat(UrlRepository.getEntities()).isEmpty();

            client.post("/urls", "url=" + testUrl);

            var listResponse = client.get("/urls");
            assertThat(listResponse.code()).isEqualTo(200);
            String listBody = listResponse.body().string();
            assertThat(listBody).contains("example.com");

            List<Url> urls = UrlRepository.getEntities();
            assertThat(urls.size()).isEqualTo(1);
            assertThat(urls.get(0).getName()).isEqualTo("https://example.com");
        });
    }

    @Test
    public void testFindByName() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            String testUrl = "https://example.com";
            client.post("/urls", "url=" + testUrl);

            Optional<Url> foundUrl = UrlRepository.findByName(testUrl);
            assertThat(foundUrl.isPresent()).isTrue();

            Url url = foundUrl.get();
            assertThat(url.getId()).isNotNull();
            assertThat(url.getName()).isEqualTo(testUrl);
            assertThat(url.getCreated_at()).isNotNull();
        });
    }

    @Test
    public void testNotValidUrl() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            String[] testUrls = {"abracadabra_123_@dasdasd",
                    "hsppt://example.com",
                    "example.com",
                    ""};

            for (int i = 0; i < testUrls.length; i++) {
                client.post("/urls","url=" + testUrls[i]);
            }

            assertThat(UrlRepository.getEntities()).hasSize(0);

            Response mainPageResponse = client.get("/");
            String mainPageBody = mainPageResponse.body().string();
            assertThat(mainPageBody.contains("Некорректный URL"));
        });
    }

    @Test
    public void testNormalization() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            String[] testUrls = {"https://example.com",
                    "https://EXAMPLE.COM",
                    "https://www.example.com"};

            for (int i = 0; i < testUrls.length; i++) {
                client.post("/urls","url=" + testUrls[i]);
            }
            assertThat(UrlRepository.getEntities()).hasSize(1);

            Response mainPageResponse = client.get("/");
            String mainPageBody = mainPageResponse.body().string();
            assertThat(mainPageBody.contains("Страница уже существует"));
        });
    }

    @Test
    public void testMultipleUrls() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            int testCount = 5;
            for (int i = 1; i <= testCount; i++) {
                String testUrl = "https://example" + i + ".com";
                client.post("/urls","url=" + testUrl);
            }
            assertThat(UrlRepository.getEntities()).hasSize(testCount);

            Response listResponce = client.get("/urls");
            assertThat(listResponce.code()).isEqualTo(200);
            String listBody = listResponce.body().string();

            for (int i = 1; i <= testCount; i++) {
                assertThat(listBody.contains("https://example" + i + ".com"));
            }
        });
    }


}
