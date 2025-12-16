package com.capstone.TimEd.service;

import sendinblue.ApiClient;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
public class BrevoEmailService {

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    private TransactionalEmailsApi getApiInstance() {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(brevoApiKey);
        return new TransactionalEmailsApi();
    }

    /**
     * Send OTP email for admin login verification
     */
    public void sendOtpEmail(String toEmail, String otp) throws Exception {
        System.out.println("=== BREVO OTP EMAIL DEBUG ===");
        System.out.println("To: " + toEmail);
        System.out.println("API Key exists: " + (brevoApiKey != null && !brevoApiKey.isEmpty()));
        System.out.println("API Key length: " + (brevoApiKey != null ? brevoApiKey.length() : 0));
        System.out.println(
                "API Key starts with 'xkeysib-': " + (brevoApiKey != null && brevoApiKey.startsWith("xkeysib-")));
        if (brevoApiKey != null && brevoApiKey.length() > 10) {
            System.out.println("API Key preview: " + brevoApiKey.substring(0, 10) + "...[HIDDEN]");
        } else {
            System.out.println("WARNING: API Key appears to be missing or too short!");
        }

        try {
            TransactionalEmailsApi apiInstance = getApiInstance();

            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            sendSmtpEmail.setSubject("TimEd System - Admin Login Verification Code");

            // Set sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setName("TimEd System");
            sender.setEmail("timeedsystem@gmail.com");
            sendSmtpEmail.setSender(sender);

            // Set recipient
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(toEmail);
            sendSmtpEmail.setTo(Arrays.asList(recipient));

            // Set HTML content
            String htmlContent = "<html><body>" +
                    "<h2>TimEd System - Admin Login</h2>" +
                    "<p>Hello,</p>" +
                    "<p>Your verification code for TimEd admin login is: <strong>" + otp + "</strong></p>" +
                    "<p>This verification code will expire in 5 minutes for your security.</p>" +
                    "<p>If you did not request this code, please ignore this email.</p>" +
                    "<br>" +
                    "<p>Best regards,<br>TimEd System Team</p>" +
                    "</body></html>";

            sendSmtpEmail.setHtmlContent(htmlContent);

            // Set reply-to
            SendSmtpEmailReplyTo replyTo = new SendSmtpEmailReplyTo();
            replyTo.setEmail("timeedsystem@gmail.com");
            replyTo.setName("TimEd System");
            sendSmtpEmail.setReplyTo(replyTo);

            // Set tags for tracking
            sendSmtpEmail.setTags(Arrays.asList("otp-verification", "admin-login"));

            System.out.println("Sending OTP email via Brevo API...");
            CreateSmtpEmail result = apiInstance.sendTransacEmail(sendSmtpEmail);

            System.out.println("Brevo Response: " + result.toString());
            System.out.println("--- Brevo API: OTP email sent successfully ---");

        } catch (Exception e) {
            System.err.println("=== BREVO OTP EMAIL FAILED ===");
            System.err.println("Error Type: " + e.getClass().getSimpleName());
            System.err.println("Error Message: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("Unauthorized")) {
                System.err.println(">>> DIAGNOSIS: API key is invalid, expired, or not set correctly!");
                System.err.println(">>> Check: Environment variable BREVO_API_KEY in Render");
                System.err.println(">>> Check: API key should start with 'xkeysib-'");
                System.err.println(">>> Check: API key permissions in Brevo dashboard");
            }
            e.printStackTrace();
            throw new Exception("Failed to send OTP email via Brevo: " + e.getMessage());
        }
    }

    /**
     * Send certificate email with PDF attachment
     */
    public void sendCertificateEmail(String toEmail, String eventId, byte[] certificatePdf) throws Exception {
        System.out.println("=== BREVO CERTIFICATE EMAIL DEBUG ===");
        System.out.println("To: " + toEmail);
        System.out.println("EventId: " + eventId);
        System.out.println("PDF size: " + (certificatePdf != null ? certificatePdf.length : 0) + " bytes");
        System.out.println("API Key exists: " + (brevoApiKey != null && !brevoApiKey.isEmpty()));
        System.out.println("API Key length: " + (brevoApiKey != null ? brevoApiKey.length() : 0));
        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            System.err.println("ERROR: BREVO_API_KEY is not set! Cannot send email.");
            throw new Exception("BREVO_API_KEY environment variable is not configured");
        }
        System.out.println("Preparing to send certificate email to " + toEmail + " via Brevo API");

        try {
            TransactionalEmailsApi apiInstance = getApiInstance();

            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            sendSmtpEmail.setSubject("Your Certificate for Event: " + eventId);

            // Set sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setName("TimEd System");
            sender.setEmail("timeedsystem@gmail.com");
            sendSmtpEmail.setSender(sender);

            // Set recipient
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(toEmail);
            sendSmtpEmail.setTo(Arrays.asList(recipient));

            // Set content
            String htmlContent = "<html><body>" +
                    "<h2>Certificate of Attendance</h2>" +
                    "<p>Hi,</p>" +
                    "<p>Thank you for attending the event.</p>" +
                    "<p>Please find your certificate attached.</p>" +
                    "<br>" +
                    "<p>Best regards,<br>TimEd Team</p>" +
                    "</body></html>";

            sendSmtpEmail.setHtmlContent(htmlContent);

            // Add PDF attachment
            if (certificatePdf != null && certificatePdf.length > 0) {
                SendSmtpEmailAttachment attachment = new SendSmtpEmailAttachment();
                attachment.setContent(certificatePdf);
                attachment.setName("Certificate.pdf");
                sendSmtpEmail.setAttachment(Arrays.asList(attachment));
            }

            // Set tags for tracking
            sendSmtpEmail.setTags(Arrays.asList("certificate", "event-completion"));

            System.out.println("Sending certificate email via Brevo API...");
            CreateSmtpEmail result = apiInstance.sendTransacEmail(sendSmtpEmail);

            System.out.println("Certificate email sent successfully to " + toEmail);
            System.out.println("Brevo Response: " + result.toString());

        } catch (Exception e) {
            System.err.println("Error sending certificate email to " + toEmail + ": " + e.getMessage());
            throw new Exception("Failed to send certificate email via Brevo: " + e.getMessage());
        }
    }

    /**
     * Send notification email (for account approvals/rejections)
     */
    public void sendNotificationEmail(String toEmail, String subject, String htmlContent, String textContent)
            throws Exception {
        System.out.println("Sending notification email via Brevo to " + toEmail);

        try {
            TransactionalEmailsApi apiInstance = getApiInstance();

            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            sendSmtpEmail.setSubject(subject);

            // Set sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setName("TimEd System");
            sender.setEmail("timeedsystem@gmail.com");
            sendSmtpEmail.setSender(sender);

            // Set recipient
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(toEmail);
            sendSmtpEmail.setTo(Arrays.asList(recipient));

            // Set content
            sendSmtpEmail.setHtmlContent(htmlContent);
            sendSmtpEmail.setTextContent(textContent);

            // Set tags for tracking
            sendSmtpEmail.setTags(Arrays.asList("notification", "account-management"));

            System.out.println("Sending notification email via Brevo API...");
            CreateSmtpEmail result = apiInstance.sendTransacEmail(sendSmtpEmail);

            System.out.println("Notification email sent successfully to " + toEmail);
            System.out.println("Brevo Response: " + result.toString());

        } catch (Exception e) {
            System.err.println("Error sending notification email to " + toEmail + ": " + e.getMessage());
            throw new Exception("Failed to send notification email via Brevo: " + e.getMessage());
        }
    }
}