package com.gaming.agricul.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthMailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> provider;

    @Mock
    private JavaMailSender sender;

    private AuthMailService service(boolean enabled) {
        return new AuthMailService(provider, enabled, "AgriShare <no-reply@agrishare.farm>",
                "https://app.agrishare.farm/", "smtp-secret");
    }

    @Test
    void verificationMail_isSentToTheUser_withTheTokenInTheUrlFragment() throws Exception {
        when(provider.getObject()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        service(true).onEmailVerificationRequested(new EmailVerificationRequested("marie@example.com", "tok-123"));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(sent.capture());
        MimeMessage message = sent.getValue();
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("marie@example.com");
        assertThat(message.getFrom()[0].toString()).contains("no-reply@agrishare.farm");
        assertThat(message.getSubject()).contains("Confirmez");
        assertThat(raw(message)).contains("https://app.agrishare.farm/verify-email#token=tok-123");
    }

    @Test
    void resetMail_pointsToTheResetPage() throws Exception {
        when(provider.getObject()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        service(true).onPasswordResetRequested(new PasswordResetRequested("marie@example.com", "tok-456"));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(sent.capture());
        assertThat(raw(sent.getValue())).contains("https://app.agrishare.farm/reset-password#token=tok-456");
    }

    @Test
    void disabledMail_sendsNothing() {
        service(false).onPasswordResetRequested(new PasswordResetRequested("marie@example.com", "tok"));

        verifyNoInteractions(provider);
    }

    @Test
    void smtpFailure_isLoggedNotThrown() {
        when(provider.getObject()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("smtp down")).when(sender).send(any(MimeMessage.class));

        assertThatNoException().isThrownBy(() -> service(true)
                .onEmailVerificationRequested(new EmailVerificationRequested("marie@example.com", "tok")));
        // Attempted exactly once: a failure is not retried in a loop
        verify(sender, times(1)).send(any(MimeMessage.class));
    }

    private static String raw(MimeMessage message) throws Exception {
        message.saveChanges();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        // Quoted-printable may wrap long lines with "=\r\n": undo it before searching
        return out.toString(java.nio.charset.StandardCharsets.UTF_8).replace("=\r\n", "").replace("=3D", "=");
    }
}
