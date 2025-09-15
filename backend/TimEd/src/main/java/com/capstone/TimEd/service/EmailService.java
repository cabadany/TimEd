package com.capstone.TimEd.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;

@Service
public class EmailService {

    @Value("${SENDGRID_API_KEY:}")
    private String sendGridApiKey;

    public void sendCertificateEmail(String toEmail, String eventId, byte[] certificatePdf) {
        try {
            System.out.println("Preparing to send certificate email to " + toEmail + " via SendGrid API");

            Email from = new Email("timeedsystem@gmail.com", "TimEd System");
            Email to = new Email(toEmail);
            String subject = "Your Certificate for Event: " + eventId;
            Content content = new Content("text/plain", 
                "Hi,\n\nThank you for attending the event.\n\nPlease find your certificate attached.\n\nBest regards,\nTimEd Team");

            Mail mail = new Mail(from, subject, to, content);

            // Add PDF attachment
            Attachments attachments = new Attachments();
            attachments.setContent(Base64.getEncoder().encodeToString(certificatePdf));
            attachments.setType("application/pdf");
            attachments.setFilename("Certificate.pdf");
            attachments.setDisposition("attachment");
            mail.addAttachments(attachments);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            System.out.println("Sending certificate email via SendGrid API...");
            Response response = sg.api(request);
            
            System.out.println("SendGrid Response Status: " + response.getStatusCode());
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                System.out.println("Certificate email sent successfully to " + toEmail);
            } else {
                System.err.println("SendGrid API error: " + response.getStatusCode() + " - " + response.getBody());
            }

        } catch (IOException e) {
            System.err.println("Error sending certificate email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}