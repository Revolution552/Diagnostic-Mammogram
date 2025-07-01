package com.diagnostic.mammogram.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username")
        })
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;


    public enum Role {
        ADMIN,
        DOCTOR,
        RADIOLOGIST;

        public String getAuthority() {
            return "ROLE_" + this.name();
        }

        public static Role fromString(String value) throws IllegalArgumentException {
                try {
                    return Role.valueOf(value.toUpperCase());
                } catch (NullPointerException | IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            String.format("Invalid role '%s'. Valid roles are: %s",
                                    value,
                                    Arrays.toString(Role.values()))
                    );
                }
        }
    }

    // ========== UserDetails Implementation ==========
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // Using the enabled field instead of hardcoded true
    @Override
    public boolean isEnabled() {
        return enabled;
    }

}