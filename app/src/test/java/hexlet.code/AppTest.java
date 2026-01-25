package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.BaseRepository;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AppTest {
    private Javalin app;
    private static MockWebServer mockServer;
    private Url existingUrl;
    private UrlCheck existingUrlCheck;
    private HikariDataSource dataSource;
    private static final String TEST_DATABASE_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;";

    private static Path getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName)
                .toAbsolutePath().normalize();
    }

    private static String readFixture(String fileName) throws IOException {
        Path filePath = getFixturePath(fileName);
        return Files.readString(filePath).trim();
    }

    @BeforeAll
    static void beforeAll() throws IOException {
        mockServer = new MockWebServer();
        MockResponse mockedResponse = new MockResponse()
            .setBody(readFixture("index.html"));
        mockServer.enqueue(mockedResponse);
        mockServer.start();

    }

    @AfterAll
    public static void afterAll() throws IOException {
        mockServer.shutdown();
    }

    @BeforeEach
    public final void setUp() throws IOException, SQLException {
        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(TEST_DATABASE_URL);
        dataSource = new HikariDataSource(hikariConfig);

        BaseRepository.dataSource = dataSource;

        var schemaStream = AppTest.class.getClassLoader().getResourceAsStream("schema.sql");
        var sql = new String(schemaStream.readAllBytes())
                .lines()
                .collect(Collectors.joining("\n"));

        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }

        app = App.getApp(TEST_DATABASE_URL);

        String urlName = "https://en.hexlet.io";
        Url url = new Url(urlName);
        UrlRepository.save(url);
        existingUrl = UrlRepository.findByName(urlName).orElseThrow();

        UrlCheck check = new UrlCheck(
                existingUrl.getId(),
                200,
                "en title",
                "en h1",
                "en description",
                LocalDateTime.now()
        );
        UrlCheckRepository.save(check);
        existingUrlCheck = UrlCheckRepository.findLatestByUrlId(existingUrl.getId()).orElseThrow();
    }

    @AfterEach
    public final void tearDown() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement()) {
                statement.execute("DROP ALL OBJECTS");
            }
            dataSource.close();
        }
    }

    @Nested
    class RootTest {
        @Test
        void testIndex() {
            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/");
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).contains("Анализатор страниц");
            });
        }
    }

    @Nested
    class UrlTest {

        @Test
        void testIndex() {
            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/urls");
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string())
                        .contains(existingUrl.getName())
                        .contains(existingUrlCheck.getStatusCode().toString());
            });
        }

        @Test
        void testShow() {
            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/urls/" + existingUrl.getId());
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string())
                        .contains(existingUrl.getName())
                        .contains(existingUrlCheck.getStatusCode().toString());
            });
        }

        @Test
        void testStore() {
            String inputUrl = "https://ru.hexlet.io";

            JavalinTest.test(app, (server, client) -> {
                var requestBody = "url=" + inputUrl;
                var postResponse = client.post("/urls", requestBody);

                assertThat(postResponse.code()).isIn(200, 302);

                var response = client.get("/urls");
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).contains(inputUrl);

                var actualUrl = UrlRepository.findByName(inputUrl);
                assertThat(actualUrl).isPresent();
                assertThat(actualUrl.get().getName()).isEqualTo(inputUrl);
            });
        }

        @Test
        void testUrlNotFound() {
            JavalinTest.test(app, (server, client) -> {
                var response = client.get("/urls/999999");
                assertThat(response.code()).isEqualTo(404);
            });
        }

        @Test
        void testNotValidUrl() {
            JavalinTest.test(app, (server, client) -> {
                String[] invalidUrls = {
                    "invalid-url",
                    "htp://example.com",
                    "",
                    "not a url at all"
                };

                for (String invalidUrl : invalidUrls) {
                    if (!invalidUrl.isEmpty()) {
                        var requestBody = "url=" + invalidUrl;
                        client.post("/urls", requestBody);
                    }
                }

                var response = client.get("/urls");
                String body = response.body().string();

                assertThat(body).doesNotContain("invalid-url");
                assertThat(body).doesNotContain("htp://");
                assertThat(body).doesNotContain("not a url");
            });
        }

        @Test
        void testFindByName() throws SQLException {
            String testUrl = "https://test.hexlet.io";
            Url url = new Url(testUrl);
            UrlRepository.save(url);

            var foundUrl = UrlRepository.findByName(testUrl);
            assertThat(foundUrl).isPresent();
            assertThat(foundUrl.get().getName()).isEqualTo(testUrl);
            assertThat(foundUrl.get().getId()).isNotNull();
        }

        @Test
        void testDuplicateUrl() {
            JavalinTest.test(app, (server, client) -> {
                String url = "https://duplicate.example.com";

                var requestBody = "url=" + url;
                client.post("/urls", requestBody);

                client.post("/urls", requestBody);

                var response = client.get("/urls");
                String body = response.body().string();

                int count = (body.length() - body.replace(url, "").length()) / url.length();
                assertThat(count).isLessThanOrEqualTo(2); // Один раз в ссылке + один в названии
            });
        }
    }

    @Nested
    class UrlCheckTest {

        @Test
        void testStore() {
            String url = mockServer.url("/").toString().replaceAll("/$", "");

            JavalinTest.test(app, (server, client) -> {
                var requestBody = "url=" + url;
                var postResponse = client.post("/urls", requestBody);
                assertThat(postResponse.code()).isIn(200, 302);

                var actualUrl = UrlRepository.findByName(url);
                assertThat(actualUrl).isPresent();
                assertThat(actualUrl.get().getName()).isEqualTo(url);

                var checkResponse = client.post("/urls/" + actualUrl.get().getId() + "/checks", "");
                assertThat(checkResponse.code()).isIn(200, 302);

                var showResponse = client.get("/urls/" + actualUrl.get().getId());
                assertThat(showResponse.code()).isEqualTo(200);

                var actualCheck = UrlCheckRepository.findLatestByUrlId(actualUrl.get().getId());
                assertThat(actualCheck).isPresent();
                assertThat(actualCheck.get().getTitle()).isEqualTo("Test page");
                assertThat(actualCheck.get().getH1()).isEqualTo("Do not expect a miracle, miracles yourself!");
                assertThat(actualCheck.get().getDescription()).isEqualTo("statements of great people");
            });
        }

        @Test
        void testCheckNotFound() {
            JavalinTest.test(app, (server, client) -> {
                var response = client.post("/urls/999999/checks", "");
                assertThat(response.code()).isIn(404, 500);
            });
        }

    }

    @Test
    public void testCreateUrlWithDuplicate() {
        JavalinTest.test(app, (server, client) -> {
            String url = "https://example.com";
            client.post("/urls", "url=" + url);
            var response = client.post("/urls", "url=" + url);
            assertThat(response.code()).isIn(200, 302);
        });
    }

    @Test
    public void testUrlRepositoryFindNonExistent() throws SQLException {
        Optional<Url> url = UrlRepository.find(999999L);
        assertThat(url).isEmpty();
    }

    @Test
    public void testUrlRepositoryFindByNameNonExistent() throws SQLException {
        Optional<Url> url = UrlRepository.findByName("nonexistent.com");
        assertThat(url).isEmpty();
    }

    @Test
    public void testUrlModel() {
        LocalDateTime now = LocalDateTime.now();
        Url url = new Url(1L, "https://example.com", now);

        assertThat(url.getId()).isEqualTo(1L);
        assertThat(url.getName()).isEqualTo("https://example.com");
        assertThat(url.getCreatedAt()).isEqualTo(now);
    }

    @Test
    public void testUrlCheckModel() {

        UrlCheck check = new UrlCheck(1L, 200, "Title", "H1", "Description", LocalDateTime.now());

        check.setId(1L);
        assertThat(check.getId()).isEqualTo(1L);
        assertThat(check.getStatusCode()).isEqualTo(200);
        assertThat(check.getTitle()).isEqualTo("Title");
    }
}
