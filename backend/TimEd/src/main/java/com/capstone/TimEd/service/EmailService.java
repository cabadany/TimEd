package com.capstone.TimEd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendCertificateEmail(String toEmail, String eventId, byte[] certificatePdf) {
        try {
            System.out.println("Preparing to send email to " + toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Certificate for Event: " + eventId);
            helper.setText("Hi,\n\nThank you for attending the event.\n\nPlease find your certificate attached.\n\nBest regards,\nTimEd Team");

            InputStreamSource attachment = new ByteArrayResource(certificatePdf);
            helper.addAttachment("Certificate.pdf", attachment);

            mailSender.send(message);

            System.out.println("Certificate email sent to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}