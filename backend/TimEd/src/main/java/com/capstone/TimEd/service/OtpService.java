package com.capstone.TimEd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    @Autowired
    private SendGridEmailService sendGridEmailService;

    @Autowired
    private UserService userService;

    // Store OTPs with expiration time (schoolId -> [otp, expirationTime])
    private final ConcurrentHashMap<String, String[]> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_VALID_DURATION = 5 * 60 * 1000; // 5 minutes

    public String generateOtp(String schoolId) throws Exception {
        System.out.println("--- OTP Service: Starting OTP generation ---");
        
        // Generate 6-digit OTP
        Random random = new Random();
        String otp = String.format("%06d", random.nextInt(1000000));
        System.out.println("Generated OTP: " + otp);

        // Get user's email
        System.out.println("Fetching user from UserService...");
        var user = userService.getUserBySchoolId(schoolId);
        if (user == null || user.getEmail() == null) {
            System.err.println("ERROR: User not found or email not available");
            throw new Exception("User not found or email not available");
        }
        
        System.out.println("User email: " + user.getEmail());

        // Store OTP with expiration time
        String[] otpData = {otp, String.valueOf(System.currentTimeMillis() + OTP_VALID_DURATION)};
        otpStore.put(schoolId, otpData);
        System.out.println("OTP stored in memory for school ID: " + schoolId);

        // Send OTP via SendGrid API (not SMTP)
        try {
            System.out.println("Sending OTP via SendGrid API to: " + user.getEmail());
            sendGridEmailService.sendOtpEmail(user.getEmail(), otp);
            System.out.println("--- OTP Service: Email sent successfully ---");
            
        } catch (Exception emailException) {
            System.err.println("--- OTP Service: EMAIL SENDING FAILED ---");
            System.err.println("Email error: " + emailException.getMessage());
            emailException.printStackTrace();
            throw new Exception("Failed to send OTP email: " + emailException.getMessage());
        }

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