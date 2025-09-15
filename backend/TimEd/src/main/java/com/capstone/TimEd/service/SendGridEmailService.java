package com.capstone.TimEd.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridEmailService {

    @Value("${SENDGRID_API_KEY:}")
    private String sendGridApiKey;

    public void sendOtpEmail(String toEmail, String otp) throws IOException {
        System.out.println("--- SendGrid API: Starting email send ---");
        System.out.println("To: " + toEmail);
        System.out.println("API Key configured: " + (sendGridApiKey != null && !sendGridApiKey.isEmpty()));

        Email from = new Email("timeedsystem@gmail.com", "TimEd System");
        Email to = new Email(toEmail);
        String subject = "TimEd Admin Login OTP";
        Content content = new Content("text/plain", 
            "Your OTP for TimEd admin login is: " + otp + "\nThis OTP will expire in 5 minutes.");

        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            System.out.println("Sending email via SendGrid API...");
            Response response = sg.api(request);
            
            System.out.println("SendGrid Response Status: " + response.getStatusCode());
            System.out.println("SendGrid Response Body: " + response.getBody());
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                System.out.println("--- SendGrid API: Email sent successfully ---");
            } else {
                throw new IOException("SendGrid API error: " + response.getStatusCode() + " - " + response.getBody());
            }
            
        } catch (IOException ex) {
            System.err.println("--- SendGrid API: Email sending failed ---");
            System.err.println("Error: " + ex.getMessage());
            throw ex;
        }
    }
}
