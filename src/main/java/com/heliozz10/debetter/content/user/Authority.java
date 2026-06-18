package com.heliozz10.debetter.content.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "authority")
public class Authority implements GrantedAuthority {
    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    public Authority(String name) {
        this.name = name;
    }

    @Override
    public String getAuthority() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Authority that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}