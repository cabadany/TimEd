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

    public void sendCertificateEmail(String to, String eventName, byte[] certificatePdf) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setFrom("johnwayne.largo@cit.edu");
        helper.setSubject("Certificate of Attendance");
        helper.setText("Dear Faculty Member,\n\n" +
                "Please find attached your Certificate of Attendance for the event.\n\n" +
                "Thank you for your participation!\n\n" +
                "Best regards,\nTimEd Team");

        helper.addAttachment("certificate.pdf", new ByteArrayResource(certificatePdf));

        mailSender.send(message);
    }
} 