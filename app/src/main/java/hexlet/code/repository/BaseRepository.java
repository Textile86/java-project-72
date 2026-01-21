package hexlet.code.repository;

import com.zaxxer.hikari.HikariDataSource;

public class BaseRepository {
    public static HikariDataSource dataSource;

    public static void setDataSource(HikariDataSource newDataSource) {
        dataSource = newDataSource;
    }
}
