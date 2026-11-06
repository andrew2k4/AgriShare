package com.gaming.agricul.service;

import com.gaming.agricul.dto.SessionDto;
import com.gaming.agricul.security.SessionUserFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Lists and revokes the server-side sessions of a user. Sessions are indexed by principal
 * name, which is the user id (see SessionAuthentication).
 *
 * <p>A session id is as good as a password, so it is never returned: the API exposes a
 * hash of it instead, enough to tell sessions apart and revoke one.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public List<SessionDto> list(Long userId, String currentSessionId) {
        return sessionsOf(userId).values().stream()
                .sorted(Comparator.comparing(Session::getLastAccessedTime).reversed())
                .map(session -> new SessionDto(
                        handle(session.getId()),
                        session.getCreationTime(),
                        session.getLastAccessedTime(),
                        session.getAttribute(SessionUserFilter.CLIENT_IP),
                        session.getAttribute(SessionUserFilter.USER_AGENT),
                        session.getId().equals(currentSessionId)))
                .toList();
    }

    /** Returns false when the handle matches none of the user's sessions. */
    public boolean revoke(Long userId, String handle) {
        return sessionsOf(userId).keySet().stream()
                .filter(id -> handle(id).equals(handle))
                .findFirst()
                .map(id -> {
                    sessionRepository.deleteById(id);
                    return true;
                })
                .orElse(false);
    }

    public void revokeAll(Long userId) {
        sessionsOf(userId).keySet().forEach(sessionRepository::deleteById);
    }

    public void revokeAllExcept(Long userId, String keptSessionId) {
        sessionsOf(userId).keySet().stream()
                .filter(id -> !id.equals(keptSessionId))
                .forEach(sessionRepository::deleteById);
    }

    private Map<String, ? extends Session> sessionsOf(Long userId) {
        return sessionRepository.findByPrincipalName(String.valueOf(userId));
    }

    static String handle(String sessionId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
