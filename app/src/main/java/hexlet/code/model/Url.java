package hexlet.code.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;

@Getter
@Setter
@ToString
public final class Url {
    private Long id;
    private String name;
    private Timestamp created_at;

    public Url(Long id, String name, Timestamp created_at) {
        this.id = id;
        this.name = name;
        this.created_at = created_at;
    }

    public Url(String name, Timestamp created_at) {
        this.name = name;
        this.created_at = created_at;
    }
}