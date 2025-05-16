package com.capstone.TimEd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendCertificateEmail(String to, String eventId, byte[] certificatePdf) throws MessagingException {
        System.out.println("Preparing to send certificate email to: " + to);
        System.out.println("Certificate PDF size: " + (certificatePdf != null ? certificatePdf.length : 0) + " bytes");

        if (certificatePdf == null || certificatePdf.length == 0) {
            throw new MessagingException("Certificate PDF is empty or null");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setFrom("johnwayne.largo@cit.edu");
            helper.setSubject("Certificate of Attendance");
            helper.setText("Dear Faculty Member,\n\n" +
                    "Please find attached your Certificate of Attendance for the event.\n\n" +
                    "This certificate has been automatically generated and sent by the TimEd system.\n\n" +
                    "Best regards,\nTimEd Team");

            // Attach the customized certificate PDF
            ByteArrayResource resource = new ByteArrayResource(certificatePdf);
            helper.addAttachment("Certificate.pdf", resource);
            System.out.println("Certificate attached successfully");

            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to);
            
        } catch (MessagingException e) {
            System.err.println("Error sending certificate email: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
} 