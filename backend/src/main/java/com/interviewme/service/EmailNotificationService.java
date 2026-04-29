package com.interviewme.service;

import com.interviewme.config.NotificationProperties;
import com.interviewme.model.Profile;
import com.interviewme.model.User;
import com.interviewme.repository.ProfileRepository;
import com.interviewme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final NotificationProperties notificationProperties;

    public void sendVisitorChatNotification(Long profileId, String visitorName, String visitorCompany, String visitorJobRole, String locale) {
        if (!notificationProperties.isEnabled()) {
            log.debug("Email notifications disabled, skipping for profileId={}", profileId);
            return;
        }

        Profile profile = profileRepository.findById(profileId).orElse(null);
        if (profile == null) {
            log.warn("Profile not found for notification: profileId={}", profileId);
            return;
        }

        User user = userRepository.findById(profile.getUserId()).orElse(null);
        if (user == null) {
            log.warn("User not found for notification: userId={}", profile.getUserId());
            return;
        }

        String recipientEmail = user.getEmail();
        String visitorsUrl = notificationProperties.getBaseUrl() + "/visitors";
        String firstName = profile.getFullName().split(" ")[0];

        boolean isPtBr = "pt-BR".equalsIgnoreCase(locale) || "pt".equalsIgnoreCase(locale);

        String subject = isPtBr
                ? "Alguem entrevistou seu perfil no Interview Me!"
                : "Someone interviewed your profile on Interview Me!";

        String body;
        if (isPtBr) {
            body = """
                    Oi %s,

                    Voce recebeu um visitante no seu perfil do Interview Me!

                    Visitante: %s
                    Empresa: %s
                    Cargo: %s

                    Eles fizeram perguntas sobre sua experiencia atraves do chat do seu perfil publico.

                    Ver seus visitantes: %s

                    --
                    Interview Me - Live Resume & Career Copilot
                    """.formatted(firstName,
                    visitorName != null ? visitorName : "Anonimo",
                    visitorCompany != null ? visitorCompany : "Nao informado",
                    visitorJobRole != null ? visitorJobRole : "Nao informado",
                    visitorsUrl);
        } else {
            body = """
                    Hi %s,

                    You had a visitor on your Interview Me profile!

                    Visitor: %s
                    Company: %s
                    Role: %s

                    They asked questions about your experience through your public profile chat.

                    View your visitors: %s

                    --
                    Interview Me - Live Resume & Career Copilot
                    """.formatted(firstName,
                    visitorName != null ? visitorName : "Anonymous",
                    visitorCompany != null ? visitorCompany : "Not provided",
                    visitorJobRole != null ? visitorJobRole : "Not provided",
                    visitorsUrl);
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationProperties.getFrom());
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Visitor chat notification sent to={} for profileId={} visitor={} company={} locale={}",
                    recipientEmail, profileId, visitorName, visitorCompany, locale);
        } catch (Exception e) {
            log.error("Failed to send visitor chat notification to={}: {}", recipientEmail, e.getMessage());
        }
    }

    public void sendWelcomeEmail(String recipientEmail, String tenantName, String locale) {
        if (!notificationProperties.isEnabled()) {
            log.debug("Email notifications disabled, skipping welcome email for {}", recipientEmail);
            return;
        }

        String baseUrl = notificationProperties.getBaseUrl();
        boolean isPtBr = "pt-BR".equalsIgnoreCase(locale) || "pt".equalsIgnoreCase(locale);

        String subject = isPtBr
                ? "Bem-vindo ao Interview Me!"
                : "Welcome to Interview Me!";

        String body = isPtBr
                ? """
                Oi %s,

                Bem-vindo ao Interview Me! Estamos felizes em ter voce aqui.

                Proximos passos para configurar seu perfil:

                1. Preencha seu perfil: Adicione seu nome, titulo profissional, resumo e experiencias.
                   Voce tambem pode importar dados do LinkedIn para agilizar o processo.

                2. Seu perfil publico: Apos preencher seu perfil, voce tera uma pagina publica
                   onde recrutadores e empresas podem conhecer sua experiencia.
                   Sua URL sera: %s/p/seu-slug

                3. Assistente de Carreira com IA: Seu perfil publico tera um chat com IA integrado.
                   Recrutadores podem fazer perguntas sobre sua experiencia, habilidades e projetos,
                   e o assistente responde com base no seu perfil.

                Acesse sua conta: %s/dashboard

                Qualquer duvida, estamos aqui para ajudar!

                --
                Interview Me - Live Resume & Career Copilot
                """.formatted(tenantName, baseUrl, baseUrl)
                : """
                Hi %s,

                Welcome to Interview Me! We're excited to have you here.

                Next steps to set up your profile:

                1. Fill in your profile: Add your name, professional headline, summary, and experiences.
                   You can also import data from LinkedIn to speed up the process.

                2. Your public profile: Once your profile is set up, you'll have a public page
                   where recruiters and companies can learn about your experience.
                   Your URL will be: %s/p/your-slug

                3. AI Career Assistant: Your public profile will have an integrated AI chat.
                   Recruiters can ask questions about your experience, skills, and projects,
                   and the assistant responds based on your profile.

                Access your account: %s/dashboard

                If you have any questions, we're here to help!

                --
                Interview Me - Live Resume & Career Copilot
                """.formatted(tenantName, baseUrl, baseUrl);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationProperties.getFrom());
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Welcome email sent to={}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to={}: {}", recipientEmail, e.getMessage());
        }
    }

    public void sendTestEmail(String to) {
        String subject = "Interview Me - Test Email";
        String body = """
                This is a test email from Interview Me.

                If you received this, your email configuration is working correctly!

                Email notifications enabled: %s
                Configured from: %s
                Base URL: %s

                --
                Interview Me - Live Resume & Career Copilot
                """.formatted(
                notificationProperties.isEnabled(),
                notificationProperties.getFrom(),
                notificationProperties.getBaseUrl());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(notificationProperties.getFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Test email sent to={}", to);
    }

    public void sendApplicationStatusEmail(String candidateEmail, String jobTitle, String newStatus) {
        if (!notificationProperties.isEnabled()) {
            log.debug("Email notifications disabled, skipping application status email");
            return;
        }

        String subject = "Application Update: " + jobTitle;
        String body = """
                Hi,

                Your application for "%s" has been updated.

                New status: %s

                Log in to your account for more details: %s/dashboard

                --
                Interview Me - Live Resume & Career Copilot
                """.formatted(jobTitle, newStatus, notificationProperties.getBaseUrl());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationProperties.getFrom());
            message.setTo(candidateEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Application status email sent to={} job={} status={}", candidateEmail, jobTitle, newStatus);
        } catch (Exception e) {
            log.error("Failed to send application status email to={}: {}", candidateEmail, e.getMessage());
        }
    }
}
