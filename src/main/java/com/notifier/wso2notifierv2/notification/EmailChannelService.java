package com.notifier.wso2notifierv2.notification;

import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.entity.NotificationTarget;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailChannelService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * Sends an email alert to the specified target.
     */
    public void sendEmail(NotificationTarget target, AlertMessage alert) {
        log.info(">>> [EMAIL CHANNEL] Dispatching alert to {}: [{}] - {}",
                target.getContact(), alert.getSeverity(), alert.getAction());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(target.getContact());
            helper.setSubject(String.format("[%s] WSO2 Alert: %s",
                    alert.getSeverity(), alert.getUseCaseType().replace("_", " ")));

            String htmlBody = buildHtmlBody(alert);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Successfully sent detailed email to {}", target.getContact());

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}. Falling back to log simulation.",
                    target.getContact(), e.getMessage());

            // Fallback: log the body for visibility if SMTP fails
            log.warn("Mock Email Data:\nTo: {}\nSubject: {}\nDetails: {}",
                    target.getContact(), alert.getAction(), alert);
        }
    }

    private String buildHtmlBody(AlertMessage alert) {
        com.notifier.wso2notifierv2.entity.Severity sev = alert.getSeverity() != null ? alert.getSeverity()
                : com.notifier.wso2notifierv2.entity.Severity.MEDIUM;

        String severityColor = switch (sev) {
            case CRITICAL -> "#ef4444";
            case HIGH -> "#f97316";
            case MEDIUM -> "#f59e0b";
            case LOW -> "#22c55e";
        };

        StringBuilder sb = new StringBuilder();
        sb.append(
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0;'>");
        sb.append(
                "<div style='max-width: 600px; margin: 20px auto; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);'>");

        // Header
        sb.append(String.format("<div style='background-color: %s; padding: 20px; color: white; text-align: center;'>",
                severityColor));
        sb.append(String.format(
                "<h2 style='margin: 0; text-transform: uppercase; letter-spacing: 1px;'>%s Priority Alert</h2>",
                sev));
        sb.append(String.format("<p style='margin: 5px 0 0; opacity: 0.9;'>%s</p>",
                alert.getUseCaseType().replace("_", " ")));
        sb.append("</div>");

        // Body
        sb.append("<div style='padding: 30px; background-color: #ffffff;'>");
        sb.append(String.format("<p style='font-size: 1.1em; color: #1a202c; font-weight: bold; margin-top: 0;'>%s</p>",
                alert.getAction()));

        if (alert.getDescription() != null && !alert.getDescription().isBlank()) {
            sb.append(String.format("<p style='color: #4a5568; margin-bottom: 20px;'>%s</p>", alert.getDescription()));
        }

        sb.append("<table style='width: 100%; border-collapse: collapse;'>");
        appendRow(sb, "Resource Type", alert.getResourceType());
        appendRow(sb, "Resource Name", alert.getResourceName());
        appendRow(sb, "Performed By", alert.getPerformedBy());

        // Custom fields based on Use Case
        if (alert.getCount() != null)
            appendRow(sb, "Call Count", alert.getCount().toString());
        if (alert.getResponseLatency() != null)
            appendRow(sb, "Response Time", alert.getResponseLatency() + " ms");
        if (alert.getBackendLatency() != null)
            appendRow(sb, "Backend Time", alert.getBackendLatency() + " ms");
        if (alert.getResponseCode() != null)
            appendRow(sb, "HTTP Code", alert.getResponseCode().toString());
        if (alert.getErrorCode() != null)
            appendRow(sb, "Error Code", alert.getErrorCode().toString());
        if (alert.getErrorMessage() != null)
            appendRow(sb, "Error Msg", alert.getErrorMessage());

        // Brute Force Specific
        if (alert.getIpAddress() != null)
            appendRow(sb, "Source IP", alert.getIpAddress());
        if (alert.getFailedAttempts() != null)
            appendRow(sb, "Fail Count", alert.getFailedAttempts().toString());
        if (alert.getPortalsTargeted() != null && !alert.getPortalsTargeted().isEmpty()) {
            appendRow(sb, "Portals", String.join(", ", alert.getPortalsTargeted()));
        }
        if (alert.getUsernamesTried() != null && !alert.getUsernamesTried().isEmpty()) {
            appendRow(sb, "Usernames", String.join(", ", alert.getUsernamesTried()));
        }

        appendRow(sb, "Timestamp", alert.getTimestamp());
        sb.append("</table>");

        sb.append(
                "<div style='margin-top: 30px; padding-top: 20px; border-top: 1px solid #edf2f7; text-align: center;'>");
        sb.append(
                "<p style='font-size: 0.85em; color: #718096;'>This is an automated security alert from WSO2 Notifier Rules Engine.</p>");
        sb.append("</div>");

        sb.append("</div></div></body></html>");

        return sb.toString();
    }

    private void appendRow(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank() || value.equals("null"))
            return;
        sb.append("<tr>");
        sb.append(String.format(
                "<td style='padding: 8px 0; color: #718096; font-size: 0.9em; width: 140px; vertical-align: top;'>%s</td>",
                label));
        sb.append(String.format(
                "<td style='padding: 8px 0; color: #2d3748; font-weight: 500; font-size: 0.9em;'>%s</td>", value));
        sb.append("</tr>");
    }
}
