DROP TABLE IF EXISTS urls;

CREATE TABLE IF NOT EXISTS urls (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         created_at TIMESTAMP NOT NULL
);
