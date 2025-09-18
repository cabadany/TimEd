package com.capstone.TimEd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private BrevoEmailService brevoEmailService;

    public void sendCertificateEmail(String toEmail, String eventId, byte[] certificatePdf) {
        try {
            System.out.println("Preparing to send certificate email to " + toEmail + " via Brevo API");
            brevoEmailService.sendCertificateEmail(toEmail, eventId, certificatePdf);
            System.out.println("Certificate email sent successfully to " + toEmail);
            
        } catch (Exception e) {
            System.err.println("Error sending certificate email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}