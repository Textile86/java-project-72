package hexlet.code.model;

public class NamedRoutes {
    public static String urlsPath() {
        return "/urls";
    }

    public static String buildUrlsPath() {
        return "/urls/build";
    }

    public static String urlPath(String id) {
        return "/urls/" + id;
    }
}
