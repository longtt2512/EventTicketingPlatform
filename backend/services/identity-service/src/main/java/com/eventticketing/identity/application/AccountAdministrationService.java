package com.eventticketing.identity.application;

import com.eventticketing.identity.api.ApiException;
import com.eventticketing.identity.api.UserResponse;
import com.eventticketing.identity.domain.RefreshSessionEntity;
import com.eventticketing.identity.domain.RefreshSessionRepository;
import com.eventticketing.identity.domain.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAdministrationService {
    private final UserRepository users;
    private final RefreshSessionRepository sessions;
    private final JdbcTemplate jdbc;
    public AccountAdministrationService(UserRepository users, RefreshSessionRepository sessions, JdbcTemplate jdbc) {
        this.users = users; this.sessions = sessions; this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public UserPage list(UUID after, int limit) {
        var ids = jdbc.query("select id from identity.users where (?::uuid is null or id > ?::uuid) order by id limit ?",
                (rs, row) -> rs.getObject(1, UUID.class), after, after, limit + 1);
        var items = ids.stream().limit(limit).map(id -> UserResponse.from(users.findById(id).orElseThrow())).toList();
        return new UserPage(items, ids.size() > limit ? items.getLast().id().toString() : null);
    }

    @Transactional
    public UserResponse changeStatus(UUID actor, UUID userId, boolean lock, String reason) {
        var user = users.lockById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "User was not found."));
        if (lock) user.lock(); else user.unlock();
        sessions.findAllByUserId(userId).forEach(RefreshSessionEntity::revoke);
        jdbc.update("insert into identity.security_audit(id,actor_user_id,subject_user_id,action,reason) values (?,?,?,?,?)",
                UUID.randomUUID(), actor, userId, lock ? "ACCOUNT_LOCKED" : "ACCOUNT_UNLOCKED", reason);
        return UserResponse.from(user);
    }

    public record UserPage(List<UserResponse> items, String nextCursor) {}
}
