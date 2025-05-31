package com.capstone.TimEd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {
    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private UserService userService;

    // Store OTPs with expiration time (schoolId -> [otp, expirationTime])
    private final ConcurrentHashMap<String, String[]> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_VALID_DURATION = 5 * 60 * 1000; // 5 minutes

    public String generateOtp(String schoolId) throws Exception {
        // Generate 6-digit OTP
        Random random = new Random();
        String otp = String.format("%06d", random.nextInt(1000000));

        // Get user's email
        var user = userService.getUserBySchoolId(schoolId);
        if (user == null || user.getEmail() == null) {
            throw new Exception("User not found or email not available");
        }

        // Store OTP with expiration time
        String[] otpData = {otp, String.valueOf(System.currentTimeMillis() + OTP_VALID_DURATION)};
        otpStore.put(schoolId, otpData);

        // Send OTP via email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("TimEd Admin Login OTP");
        message.setText("Your OTP for TimEd admin login is: " + otp + "\nThis OTP will expire in 5 minutes.");
        emailSender.send(message);

        return otp;
    }

    public boolean verifyOtp(String schoolId, String otp) {
        String[] storedOtpData = otpStore.get(schoolId);
        if (storedOtpData == null) {
            return false;
        }

        String storedOtp = storedOtpData[0];
        long expirationTime = Long.parseLong(storedOtpData[1]);

        // Check if OTP is expired
        if (System.currentTimeMillis() > expirationTime) {
            otpStore.remove(schoolId);
            return false;
        }

        // Verify OTP
        boolean isValid = storedOtp.equals(otp);
        if (isValid) {
            otpStore.remove(schoolId); // Remove OTP after successful verification
        }
        return isValid;
    }
} 