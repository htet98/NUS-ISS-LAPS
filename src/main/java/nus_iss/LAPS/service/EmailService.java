package nus_iss.LAPS.service;

import nus_iss.LAPS.model.LeaveApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Service responsible for sending email notifications to employees when their
 * leave applications are approved or rejected.
 *
 * Email failures are logged but never propagate to the caller so that a mail
 * server misconfiguration cannot block approval/rejection operations.
 *
 * Author: Htet Nandar (Grace)
 */
@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    //app.base-url=http://localhost:8080
    private String baseUrl;

    @Value("${spring.mail.from:noreply@laps.app}")
    private String fromAddress;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Sends an approval notification to the employee.
     * Silently logs any error; never throws.
     */
    public void sendApprovalNotification(LeaveApplication application) {
        if (mailSender == null) {
            log.warn("Email not configured — skipping approval notification for application {}",
                    application.getLeaveApplicationId());
            return;
        }
        String to = resolveEmail(application);
        if (to == null) return;

        String subject = "✅ Your leave application has been approved – LAPS";
        String body = buildApprovalBody(application);
        sendHtml(to, subject, body);
    }

    /**
     * Sends a rejection notification to the employee.
     * Silently logs any error; never throws.
     */
    public void sendRejectionNotification(LeaveApplication application) {
        if (mailSender == null) {
            log.warn("Email not configured — skipping rejection notification for application {}",
                    application.getLeaveApplicationId());
            return;
        }
        String to = resolveEmail(application);
        if (to == null) return;

        String subject = "❌ Your leave application has been rejected – LAPS";
        String body = buildRejectionBody(application);
        sendHtml(to, subject, body);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} — subject: {}", to, subject);
        } catch (Exception e) {
            // Catch all (MessagingException, MailSendException, etc.) so that a
            // mail server misconfiguration never blocks leave approval / rejection.
            log.error("Failed to send email to {} — {}: {}", to, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String resolveEmail(LeaveApplication app) {
        if (app.getEmployee() == null || app.getEmployee().getEmail() == null) {
            log.warn("Cannot send email: no email address for application {}", app.getLeaveApplicationId());
            return null;
        }
        return app.getEmployee().getEmail();
    }

    private String buildApprovalBody(LeaveApplication app) {
        String loginUrl = baseUrl + "/login";
        String appId    = String.valueOf(app.getLeaveApplicationId());
        String empName  = app.getEmployee().getFirst_name() + " " + app.getEmployee().getLast_name();
        String leaveType = app.getLeaveType() != null ? app.getLeaveType().getName().toString() : "Leave";
        String start    = app.getStartDate() != null ? app.getStartDate().toString() : "–";
        String end      = app.getEndDate()   != null ? app.getEndDate().toString()   : "–";
        String duration = app.getDurationDisplay();
        String manager  = app.getApprovedBy() != null
                ? app.getApprovedBy().getFirst_name() + " " + app.getApprovedBy().getLast_name()
                : "Your Manager";

        return """
                <html><body style="font-family:Arial,sans-serif;color:#333;max-width:600px;margin-inline: auto;">
                <div style="background:#198754;padding:20px;border-radius:8px 8px 0 0;">
                  <h2 style="color:white;margin:0;">✅ Leave Application Approved</h2>
                </div>
                <div style="padding:24px;border:1px solid #ddd;border-top:none;border-radius:0 0 8px 8px;">
                  <p>Dear <strong>%s</strong>,</p>
                  <p>Your leave application <strong>#%s</strong> has been <strong style="color:#198754;">approved</strong> by %s.</p>
                  <table style="width:100%%;border-collapse:collapse;margin:16px 0;">
                    <tr><td style="padding:8px;background:#f8f9fa;font-weight:bold;width:35%%;">Leave Type</td><td style="padding:8px;">%s</td></tr>
                    <tr><td style="padding:8px;font-weight:bold;">Start Date</td><td style="padding:8px;">%s</td></tr>
                    <tr><td style="padding:8px;background:#f8f9fa;font-weight:bold;">End Date</td><td style="padding:8px;background:#f8f9fa;">%s</td></tr>
                    <tr><td style="padding:8px;font-weight:bold;">Duration</td><td style="padding:8px;">%s</td></tr>
                  </table>
                  <p>To view the full details of your application, please log in to LAPS:</p>
                  <p style="text-align:center;">
                    <a href="%s" style="background:#0d6efd;color:white;padding:12px 24px;border-radius:4px;text-decoration:none;display:inline-block;">View in LAPS →</a>
                  </p>
                  <hr style="margin:24px 0;border:none;border-top:1px solid #eee;">
                  <p style="color:#888;font-size:12px;">LAPS – Leave Application Processing System | NUS-ISS</p>
                </div></body></html>
                """.formatted(empName, appId, manager, leaveType, start, end, duration, loginUrl);
    }

    private String buildRejectionBody(LeaveApplication app) {
        String loginUrl = baseUrl + "/login";
        String appId    = String.valueOf(app.getLeaveApplicationId());
        String empName  = app.getEmployee().getFirst_name() + " " + app.getEmployee().getLast_name();
        String leaveType = app.getLeaveType() != null ? app.getLeaveType().getName().toString() : "Leave";
        String start    = app.getStartDate() != null ? app.getStartDate().toString() : "–";
        String end      = app.getEndDate()   != null ? app.getEndDate().toString()   : "–";
        String manager  = app.getApprovedBy() != null
                ? app.getApprovedBy().getFirst_name() + " " + app.getApprovedBy().getLast_name()
                : "Your Manager";
        String comment  = app.getManagerComment() != null ? app.getManagerComment() : "(no comment provided)";

        return """
                <html><body style="font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;">
                <div style="background:#dc3545;padding:20px;border-radius:8px 8px 0 0;">
                  <h2 style="color:white;margin:0;">❌ Leave Application Rejected</h2>
                </div>
                <div style="padding:24px;border:1px solid #ddd;border-top:none;border-radius:0 0 8px 8px;">
                  <p>Dear <strong>%s</strong>,</p>
                  <p>Your leave application <strong>#%s</strong> has been <strong style="color:#dc3545;">rejected</strong> by %s.</p>
                  <table style="width:100%%;border-collapse:collapse;margin:16px 0;">
                    <tr><td style="padding:8px;background:#f8f9fa;font-weight:bold;width:35%%;">Leave Type</td><td style="padding:8px;">%s</td></tr>
                    <tr><td style="padding:8px;font-weight:bold;">Start Date</td><td style="padding:8px;">%s</td></tr>
                    <tr><td style="padding:8px;background:#f8f9fa;font-weight:bold;">End Date</td><td style="padding:8px;background:#f8f9fa;">%s</td></tr>
                  </table>
                  <div style="background:#fff3cd;border:1px solid #ffc107;border-radius:4px;padding:16px;margin:16px 0;">
                    <strong>Manager's Comment:</strong>
                    <p style="margin:8px 0 0 0;">%s</p>
                  </div>
                  <p>You may submit a new application after reviewing the feedback. Log in to view full details:</p>
                  <p style="text-align:center;">
                    <a href="%s" style="background:#0d6efd;color:white;padding:12px 24px;border-radius:4px;text-decoration:none;display:inline-block;">View in LAPS →</a>
                  </p>
                  <hr style="margin:24px 0;border:none;border-top:1px solid #eee;">
                  <p style="color:#888;font-size:12px;">LAPS – Leave Application Processing System | NUS-ISS</p>
                </div></body></html>
                """.formatted(empName, appId, manager, leaveType, start, end, comment, loginUrl);
    }
}
