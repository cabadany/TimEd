package com.capstone.TimEd;

import com.capstone.TimEd.config.EmailConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class EmailConfigTest {

    @Autowired
    private EmailConfig emailConfig;

    @Test
    public void testEmailConfigLoads() {
        JavaMailSender mailSender = emailConfig.getJavaMailSender();
        assertNotNull(mailSender, "JavaMailSender should not be null");
    }

    @Test
    public void testMailSenderProperties() {
        JavaMailSender mailSender = emailConfig.getJavaMailSender();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("test@example.com");
        message.setSubject("Test Subject");
        message.setText("Test Body");
        assertNotNull(mailSender, "JavaMailSender should not be null");
    }
} 