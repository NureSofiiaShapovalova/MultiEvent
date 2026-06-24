package nure.com.eventflow.iam.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.shared.domain.BaseEntity;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserProfile profile;

    protected User() {
    }

    public static User register(String email, String passwordHash, Role userRole, UserProfile profile) {
        User user = new User();
        user.email = normalizeEmail(email);
        user.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        user.assignRole(userRole);
        user.changeProfile(profile);
        return user;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public UserProfile getProfile() {
        return profile;
    }

    public void assignRole(Role role) {
        roles.add(Objects.requireNonNull(role, "role must not be null"));
    }

    public void removeRole(Role role) {
        roles.remove(Objects.requireNonNull(role, "role must not be null"));
    }

    public void removeRole(RoleName roleName) {
        roles.removeIf(role -> role.getName() == Objects.requireNonNull(roleName, "roleName must not be null"));
    }

    public boolean hasRole(Role role) {
        return role != null && roles.contains(role);
    }

    public boolean hasRole(RoleName roleName) {
        return roles.stream().anyMatch(role -> role.getName() == Objects.requireNonNull(roleName, "roleName must not be null"));
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }

    public void markLoggedIn(Instant loginTime) {
        this.lastLoginAt = Objects.requireNonNull(loginTime, "loginTime must not be null");
    }

    public void lockAccount() {
        accountNonLocked = false;
    }

    public void unlockAccount() {
        accountNonLocked = true;
    }

    public void disable() {
        enabled = false;
    }

    public void enable() {
        enabled = true;
    }

    public void changeProfile(UserProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile must not be null");
        profile.attachTo(this);
    }

    private static String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email must not be null").trim().toLowerCase();
    }
}
