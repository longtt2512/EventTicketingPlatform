package com.eventticketing.identity.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "identity")
public class UserEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "preferred_locale", nullable = false, length = 8)
    private String preferredLocale = "vi";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountStatus status = AccountStatus.PENDING_EMAIL;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", schema = "identity", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    private Set<String> roles = new HashSet<>();

    protected UserEntity() {
    }

    public UserEntity(String email, String passwordHash, String displayName, String preferredLocale) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.preferredLocale = preferredLocale;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.roles.add("CUSTOMER");
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getPreferredLocale() { return preferredLocale; }
    public AccountStatus getStatus() { return status; }
    public boolean isEmailVerified() { return emailVerified; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<String> getRoles() { return roles; }

    public void verifyEmail() {
        this.emailVerified = true;
        if (this.status == AccountStatus.PENDING_EMAIL) this.status = AccountStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void changePassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
        this.failedLoginAttempts = 0;
        this.updatedAt = Instant.now();
    }

    public void lock() {
        this.status = AccountStatus.LOCKED;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    public void unlock() {
        this.status = emailVerified ? AccountStatus.ACTIVE : AccountStatus.PENDING_EMAIL;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    public void recordFailedLogin(Instant until) {
        this.failedLoginAttempts++;
        this.lockedUntil = until;
        if (until != null) this.status = AccountStatus.LOCKED;
        this.updatedAt = Instant.now();
    }

    public void clearFailedLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    public void addRole(String role) {
        this.roles.add(role);
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String displayName, String locale) {
        this.displayName = displayName.trim();
        this.preferredLocale = locale;
        this.updatedAt = Instant.now();
    }
}
