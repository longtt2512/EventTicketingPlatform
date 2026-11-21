package com.eventticketing.identity.application;

import com.eventticketing.identity.domain.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owner-approved 60-second interval; same email lock as initial registration. */
@Service
public class VerificationResendService {
    public static final int COOLDOWN_SECONDS = 60;
    private final JdbcTemplate db;
    private final UserRepository users;
    private final VerificationTokenRepository verificationTokens;
    private final TokenService tokens;
    private final EmailNotificationService emails;
    private final long verificationHours;
    public VerificationResendService(JdbcTemplate db, UserRepository users, VerificationTokenRepository verificationTokens,
            TokenService tokens, EmailNotificationService emails, @Value("${app.email.verification-hours:24}") long verificationHours) {
        this.db=db;this.users=users;this.verificationTokens=verificationTokens;this.tokens=tokens;this.emails=emails;this.verificationHours=verificationHours;
    }
    @Transactional
    public void resend(String address) { resend(address, null); }
    @Transactional
    public void resend(String address, com.eventticketing.identity.api.AuthContext context) {
        String email=address.trim().toLowerCase(Locale.ROOT);
        db.queryForObject("select pg_advisory_xact_lock(hashtext(?))",Object.class,email);
        var account=users.findByEmail(email);
        if(account.isEmpty())return;
        var user=account.get();
        if(user.isEmailVerified() || user.getStatus()!=AccountStatus.PENDING_EMAIL)return;
        boolean recent=Boolean.TRUE.equals(db.queryForObject("""
            select exists(select 1 from identity.verification_tokens where user_id=?
              and purpose='EMAIL_VERIFICATION' and created_at > clock_timestamp() - make_interval(secs => ?))
            """,Boolean.class,user.getId(),COOLDOWN_SECONDS));
        if(recent)return;
        String raw=tokens.randomToken();
        verificationTokens.save(new VerificationTokenEntity(user.getId(),tokens.hash(raw),TokenPurpose.EMAIL_VERIFICATION,
                Instant.now().plus(verificationHours,ChronoUnit.HOURS)));
        if (context == null) emails.sendVerification(email,raw);
        else emails.sendVerification(email,raw,context);
    }
}
