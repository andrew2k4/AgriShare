package com.gaming.agricul.Controller;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.dto.AuthRequests;
import com.gaming.agricul.dto.SessionDto;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.security.ClientIp;
import com.gaming.agricul.security.CurrentUserResolver;
import com.gaming.agricul.security.SessionLogin;
import com.gaming.agricul.service.AuthService;
import com.gaming.agricul.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Accounts, sign-in and sessions")
public class AuthController {

    private static final Map<String, String> CHECK_YOUR_INBOX = Map.of("message",
            "If this address can be used, an e-mail has been sent to it");

    private final CurrentUserResolver currentUserResolver;
    private final AuthService authService;
    private final SessionService sessionService;
    private final SessionLogin sessionLogin;

    @GetMapping("/csrf")
    @Operation(summary = "Get a CSRF token; send it back in the X-XSRF-TOKEN header on every POST/PUT/DELETE "
            + "made with the session cookie")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    // ---------------------------------------------------------------- accounts

    @PostMapping("/register")
    @Operation(summary = "Create an account. Always 202, even if the e-mail is taken: its owner is told by e-mail")
    public ResponseEntity<Map<String, String>> register(@RequestBody AuthRequests.Register body, HttpServletRequest request) {
        try {
            authService.register(body.email(), body.username(), body.password(), body.displayName(), ClientIp.of(request));
        } catch (AuthService.SilentlyIgnored ignored) {
            // same answer as success
        }
        return ResponseEntity.accepted().body(CHECK_YOUR_INBOX);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Confirm the e-mail address with the token from the e-mailed link")
    public Map<String, String> verifyEmail(@RequestBody AuthRequests.Token body) {
        authService.verifyEmail(body.token());
        return Map.of("message", "E-mail address confirmed, you can now sign in");
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Send the confirmation e-mail again. Always 202")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody AuthRequests.Email body,
                                                                  HttpServletRequest request) {
        try {
            authService.resendVerification(body.email(), ClientIp.of(request));
        } catch (AuthService.SilentlyIgnored ignored) {
            // same answer as success
        }
        return ResponseEntity.accepted().body(CHECK_YOUR_INBOX);
    }

    // ---------------------------------------------------------------- sign-in / sign-out

    @PostMapping("/login")
    @Operation(summary = "Sign in with an e-mail or username and a password; opens a session (cookie)")
    public Map<String, Object> login(@RequestBody AuthRequests.Login body,
                                     HttpServletRequest request, HttpServletResponse response) {
        AppUser user = authService.authenticate(body.identifier(), body.password(), ClientIp.of(request));
        sessionLogin.open(user, request, response);
        return meBody(user);
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign out of this session")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        sessionLogin.close(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Sign out of every session of the current user, this one included")
    public ResponseEntity<Void> logoutAll(Authentication authentication, HttpServletRequest request) {
        sessionService.revokeAll(currentUserResolver.resolve(authentication).getId());
        return logout(request);
    }

    @GetMapping("/me")
    @Operation(summary = "The current user, including their role", security = @SecurityRequirement(name = "session"))
    public Map<String, Object> me(Authentication authentication) {
        return meBody(currentUserResolver.resolve(authentication));
    }

    // ---------------------------------------------------------------- sessions

    @GetMapping("/sessions")
    @Operation(summary = "Signed-in devices of the current user")
    public List<SessionDto> sessions(Authentication authentication, HttpServletRequest request) {
        return sessionService.list(currentUserResolver.resolve(authentication).getId(), currentSessionId(request));
    }

    @DeleteMapping("/sessions/{handle}")
    @Operation(summary = "Sign out one device, by the handle returned in the sessions list")
    public ResponseEntity<Void> revokeSession(@PathVariable String handle, Authentication authentication) {
        if (!sessionService.revoke(currentUserResolver.resolve(authentication).getId(), handle)) {
            throw new NotFoundException("Session not found");
        }
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- passwords

    @PostMapping("/password/forgot")
    @Operation(summary = "E-mail a password reset link. Always 202")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody AuthRequests.Email body,
                                                              HttpServletRequest request) {
        try {
            authService.forgotPassword(body.email(), ClientIp.of(request));
        } catch (AuthService.SilentlyIgnored ignored) {
            // same answer as success
        }
        return ResponseEntity.accepted().body(CHECK_YOUR_INBOX);
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Set a new password with the token from the e-mailed link; signs out every session")
    public Map<String, String> resetPassword(@RequestBody AuthRequests.ResetPassword body) {
        Long userId = authService.resetPassword(body.token(), body.newPassword());
        sessionService.revokeAll(userId);
        return Map.of("message", "Password changed, you can now sign in");
    }

    @PostMapping("/password/change")
    @Operation(summary = "Change the password (current one required); signs out every other session")
    public ResponseEntity<Void> changePassword(@RequestBody AuthRequests.ChangePassword body,
                                               Authentication authentication, HttpServletRequest request) {
        Long userId = currentUserResolver.resolve(authentication).getId();
        authService.changePassword(userId, body.currentPassword(), body.newPassword());
        sessionService.revokeAllExcept(userId, currentSessionId(request));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/username")
    @Operation(summary = "Choose a username, once, for an account that has none (e.g. created with Google)")
    public Map<String, Object> chooseUsername(@RequestBody AuthRequests.Username body, Authentication authentication) {
        AppUser user = authService.chooseUsername(currentUserResolver.resolve(authentication).getId(), body.username());
        return meBody(user);
    }

    // ---------------------------------------------------------------- helpers

    private static Map<String, Object> meBody(AppUser user) {
        // LinkedHashMap, not Map.of: several fields can be null
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", user.getId());
        body.put("username", user.getUsername());
        body.put("email", Objects.requireNonNullElse(user.getEmail(), ""));
        body.put("emailVerified", user.isEmailVerified());
        body.put("name", Objects.requireNonNullElse(user.getDisplayName(), ""));
        body.put("role", user.getRole());
        body.put("hasPassword", user.hasPassword());
        return body;
    }

    private static String currentSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : session.getId();
    }
}
