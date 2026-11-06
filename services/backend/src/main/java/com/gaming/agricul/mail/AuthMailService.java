package com.gaming.agricul.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the account e-mails. Messages go out asynchronously and only after the transaction
 * that requested them has committed: no mail for a rolled-back registration, and the response
 * time of "forgot password" does not reveal whether the account exists.
 *
 * <p>The token is put in the URL fragment (after '#'): browsers never send the fragment to a
 * server or in a Referer header, so the token does not end up in access logs.
 *
 * <p>No user-supplied text (such as the display name) is put in the messages, so the
 * registration form cannot be used to send arbitrary content from our address.
 */
@Slf4j
@Service
public class AuthMailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;
    private final String frontendUrl;

    public AuthMailService(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${app.mail.enabled}") boolean enabled,
                           @Value("${app.mail.from}") String from,
                           @Value("${app.frontend-url}") String frontendUrl,
                           @Value("${spring.mail.password:}") String smtpPassword) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        if (!enabled) {
            log.warn("E-mail sending is disabled (app.mail.enabled=false): account links are only logged.");
        } else if (smtpPassword.isBlank()) {
            log.warn("E-mail sending is enabled but MAIL_PASSWORD is empty: every e-mail will fail. "
                    + "Set MAIL_PASSWORD, or MAIL_ENABLED=false in local development.");
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmailVerificationRequested(EmailVerificationRequested event) {
        String link = frontendUrl + "/verify-email#token=" + event.rawToken();
        send(event.email(),
                "Confirmez votre adresse e-mail - AgriShare",
                """
                Bonjour,

                Bienvenue sur AgriShare. Pour activer votre compte, confirmez votre adresse \
                e-mail en ouvrant ce lien (valable 24 heures) :

                %s

                Si vous n'êtes pas à l'origine de cette inscription, ignorez simplement ce message.
                """.formatted(link),
                html("Confirmez votre adresse e-mail",
                        "Bienvenue sur AgriShare. Pour activer votre compte, confirmez votre adresse "
                                + "e-mail (lien valable 24 heures).",
                        "Confirmer mon adresse", link,
                        "Si vous n'êtes pas à l'origine de cette inscription, ignorez simplement ce message."),
                link);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordResetRequested(PasswordResetRequested event) {
        String link = frontendUrl + "/reset-password#token=" + event.rawToken();
        send(event.email(),
                "Réinitialisation de votre mot de passe - AgriShare",
                """
                Bonjour,

                Une réinitialisation du mot de passe de votre compte AgriShare a été demandée. \
                Pour choisir un nouveau mot de passe, ouvrez ce lien (valable 30 minutes) :

                %s

                Si vous n'avez rien demandé, ignorez ce message : votre mot de passe reste inchangé.
                """.formatted(link),
                html("Réinitialisation du mot de passe",
                        "Une réinitialisation du mot de passe de votre compte AgriShare a été demandée "
                                + "(lien valable 30 minutes).",
                        "Choisir un nouveau mot de passe", link,
                        "Si vous n'avez rien demandé, ignorez ce message : votre mot de passe reste inchangé."),
                link);
    }

    private void send(String to, String subject, String text, String html, String link) {
        if (!enabled) {
            log.info("[mail disabled] '{}' for {}: {}", subject, to, link);
            return;
        }
        try {
            JavaMailSender sender = mailSender.getObject();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            sender.send(message);
        } catch (Exception e) {
            // Never propagate: the request that triggered the mail has already succeeded.
            log.error("Failed to send '{}' e-mail to {}", subject, to, e);
        }
    }

    /** All arguments are our own constant text and links, never user input. */
    private static String html(String title, String intro, String buttonLabel, String link, String footer) {
        return """
                <!doctype html>
                <html lang="fr"><body style="margin:0;padding:24px;background:#f4f6f3;font-family:Arial,sans-serif;color:#1f2a1f">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:8px;padding:32px">
                    <h1 style="font-size:20px;margin:0 0 16px">%s</h1>
                    <p style="line-height:1.5">Bonjour,</p>
                    <p style="line-height:1.5">%s</p>
                    <p style="margin:28px 0"><a href="%s" style="background:#2f7d32;color:#ffffff;text-decoration:none;padding:12px 20px;border-radius:6px;display:inline-block">%s</a></p>
                    <p style="font-size:13px;line-height:1.5;color:#5b665b">Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br><span style="word-break:break-all">%s</span></p>
                    <p style="font-size:13px;line-height:1.5;color:#5b665b">%s</p>
                  </div>
                </body></html>
                """.formatted(title, intro, link, buttonLabel, link, footer);
    }
}
