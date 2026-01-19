package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
    private static MockWebServer mockServer;

    @BeforeAll
    static void beforeAll() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        mockServer.close();
    }

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
            assertThat(response.body().string()).contains("https://ru.hexlet.io");
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
                client.post("/urls", "url=" + testUrls[i]);
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
                client.post("/urls", "url=" + testUrls[i]);
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
                client.post("/urls", "url=" + testUrl);
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

    @Test
    public void testUrlCheck() throws SQLException {
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Test Page Title</title>
                    <meta name="description" content="Test page description">
                </head>
                <body>
                    <h1>Test Header</h1>
                    <p>Test content</p>
                </body>
                </html>
                """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(html)
                .addHeader("Content-Type", "text/html; charset=utf-8"));

        String mockUrl = mockServer.url("/").toString();
        mockUrl = mockUrl.substring(0, mockUrl.length() - 1);
        String finalMockUrl = mockUrl;

        JavalinTest.test(app, (server, client) -> {

            client.post("/urls", "url=" + finalMockUrl);

            Url url = UrlRepository.getEntities().get(0);
            assertThat(url.getName()).isEqualTo(finalMockUrl);

            // Запускаем проверку
            client.post("/urls/" + url.getId() + "/checks", "");

            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).hasSize(1);

            UrlCheck check = checks.get(0);
            assertThat(check.getStatusCode()).isEqualTo(200);
            assertThat(check.getTitle()).isEqualTo("Test Page Title");
            assertThat(check.getH1()).isEqualTo("Test Header");
            assertThat(check.getDescription()).isEqualTo("Test page description");
            assertThat(check.getUrlId()).isEqualTo(url.getId());
            assertThat(check.getCreatedAt()).isNotNull();

            Response response = client.get("/urls/" + url.getId());
            assertThat(response.code()).isEqualTo(200);
            String body = response.body().string();

            assertThat(body).contains("200");
            assertThat(body).contains("Test Page Title");
            assertThat(body).contains("Test Header");
            assertThat(body).contains("Test page description");
        });
    }

    @Test
    public void testUrlCheckWithoutMetaDescription() throws SQLException {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Page Without Description</title>
            </head>
            <body>
                <h1>Header</h1>
            </body>
            </html>
            """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(html));

        String mockUrl = mockServer.url("/").toString();
        mockUrl = mockUrl.substring(0, mockUrl.length() - 1);
        String finalMockUrl = mockUrl;

        JavalinTest.test(app, (server, client) -> {
            client.post("/urls", "url=" + finalMockUrl);
            Url url = UrlRepository.getEntities().get(0);

            client.post("/urls/" + url.getId() + "/checks", "");
            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).hasSize(1);

            UrlCheck check = checks.get(0);
            assertThat(check.getStatusCode()).isEqualTo(200);
            assertThat(check.getTitle()).isEqualTo("Page Without Description");
            assertThat(check.getH1()).isEqualTo("Header");
            assertThat(check.getDescription()).isNull();
        });

    }

    @Test
    public void testUrlCheckWithoutH1() throws SQLException {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Page Without H1</title>
                <meta name="description" content="Description here">
            </head>
            <body>
                <p>No header here</p>
            </body>
            </html>
            """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(html));

        String mockUrl = mockServer.url("/").toString();
        mockUrl = mockUrl.substring(0, mockUrl.length() - 1);

        String finalMockUrl = mockUrl;
        JavalinTest.test(app, (server, client) -> {
            client.post("/urls", "url=" + finalMockUrl);
            Url url = UrlRepository.getEntities().get(0);

            client.post("/urls/" + url.getId() + "/checks", "");

            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            UrlCheck check = checks.get(0);

            assertThat(check.getH1()).isNull();
            assertThat(check.getTitle()).isEqualTo("Page Without H1");
            assertThat(check.getDescription()).isEqualTo("Description here");
        });
    }

    @Test
    public void testUrlCheckServerError() throws SQLException {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("<html><body>Internal Server Error</body></html>"));

        String mockUrl = mockServer.url("/").toString();
        mockUrl = mockUrl.substring(0, mockUrl.length() - 1);

        String finalMockUrl = mockUrl;
        JavalinTest.test(app, (server, client) -> {
            client.post("/urls", "url=" + finalMockUrl);
            Url url = UrlRepository.getEntities().get(0);

            client.post("/urls/" + url.getId() + "/checks", "");

            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).hasSize(1);
            assertThat(checks.get(0).getStatusCode()).isEqualTo(500);
        });
    }

    @Test
    public void testMultipleChecks() throws SQLException {

        String html1 = "<html><head><title>First Check</title></head><body><h1>Header 1</h1></body></html>";
        String html2 = "<html><head><title>Second Check</title></head><body><h1>Header 2</h1></body></html>";
        String html3 = "<html><head><title>Third Check</title></head><body><h1>Header 3</h1></body></html>";

        mockServer.enqueue(new MockResponse().setResponseCode(200).setBody(html1));
        mockServer.enqueue(new MockResponse().setResponseCode(200).setBody(html2));
        mockServer.enqueue(new MockResponse().setResponseCode(404).setBody(html3));

        String mockUrl = mockServer.url("/").toString();
        mockUrl = mockUrl.substring(0, mockUrl.length() - 1);

        String finalMockUrl = mockUrl;
        JavalinTest.test(app, (server, client) -> {
            client.post("/urls", "url=" + finalMockUrl);
            Url url = UrlRepository.getEntities().get(0);

            client.post("/urls/" + url.getId() + "/checks", "");
            client.post("/urls/" + url.getId() + "/checks", "");
            client.post("/urls/" + url.getId() + "/checks", "");

            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).hasSize(3);

            assertThat(checks.get(0).getTitle()).isEqualTo("Third Check");
            assertThat(checks.get(0).getStatusCode()).isEqualTo(404);

            assertThat(checks.get(1).getTitle()).isEqualTo("Second Check");
            assertThat(checks.get(1).getStatusCode()).isEqualTo(200);

            assertThat(checks.get(2).getTitle()).isEqualTo("First Check");
            assertThat(checks.get(2).getStatusCode()).isEqualTo(200);
        });
    }

    @Test
    public void testUrlCheckDisplayedInList() throws SQLException {
        String html = "<html><head><title>Test</title></head><body><h1>Header</h1></body></html>";

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(html));

        String mockUrl = mockServer.url("/").toString();
        mockUrl = mockUrl.substring(0, mockUrl.length() - 1);

        String finalMockUrl = mockUrl;
        JavalinTest.test(app, (server, client) -> {

            client.post("/urls", "url=" + finalMockUrl);
            Url url = UrlRepository.getEntities().get(0);

            client.post("/urls/" + url.getId() + "/checks", "");

            Response listResponse = client.get("/urls");
            String listBody = listResponse.body().string();

            assertThat(listBody).contains("200");
            assertThat(listBody).contains(finalMockUrl);

            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).hasSize(1);
        });
    }

    @Test
    public void testUrlCheckInvalidUrl() throws SQLException {
        JavalinTest.test(app, (server, client) -> {

            String invalidUrl = "http://invalid-url-that-does-not-exist-12345.com";
            client.post("/urls", "url=" + invalidUrl);

            Url url = UrlRepository.getEntities().get(0);

            client.post("/urls/" + url.getId() + "/checks", "");

            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).isEmpty();
        });
    }
}
