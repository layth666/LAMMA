package service;


import java.util.Properties;
import java.util.Random;
import jakarta.mail.*;
import jakarta.mail.internet.*;

/**
 * Professional Email Service for Password Reset
 * Sends beautiful HTML emails with verification codes
 */
public class EmailService {

    // ✅ CONFIGURATION - Utilise Gmail SMTP
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "saifldh3@gmail.com"; // ← CHANGEZ ICI
    private static final String EMAIL_PASSWORD = "iabw fqpn yqpe cpqo"; // ← CHANGEZ ICI (App Password)

    /**
     * Send password reset email with verification code
     * @param toEmail Recipient email
     * @param userName User's name
     * @param verificationCode 6-digit code
     * @return true if sent successfully
     */
    public static boolean sendPasswordResetEmail(String toEmail, String userName, String verificationCode) {
        try {
            System.out.println("📧 Sending password reset email to: " + toEmail);

            // Setup mail server properties
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            // Create session with authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });

            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "LAMMA Security"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🔐 Password Reset Code - LAMMA");

            // HTML email content
            String htmlContent = createEmailHTML(userName, verificationCode);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            // Send email
            Transport.send(message);

            System.out.println("✅ Email sent successfully to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generate random 6-digit verification code
     */
    public static String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6 digits
        return String.valueOf(code);
    }

    /**
     * Create beautiful HTML email template
     */
    private static String createEmailHTML(String userName, String code) {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Password Reset</title>
</head>
<body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f8f9fa;">
    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f8f9fa; padding: 40px 0;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); overflow: hidden;">
                    
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #7B5FF5 0%%, #6B4FE5 100%%); padding: 40px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 32px; font-weight: 700; letter-spacing: 2px;">LAMMA</h1>
                            <p style="color: #e0e7ff; margin: 8px 0 0 0; font-size: 14px;">Security Team</p>
                        </td>
                    </tr>
                    
                    <!-- Body -->
                    <tr>
                        <td style="padding: 40px;">
                            <h2 style="color: #1e293b; margin: 0 0 16px 0; font-size: 24px; font-weight: 600;">Password Reset Request</h2>
                            <p style="color: #64748b; line-height: 1.6; margin: 0 0 24px 0; font-size: 16px;">Hi <strong style="color: #1e293b;">%s</strong>,</p>
                            <p style="color: #64748b; line-height: 1.6; margin: 0 0 24px 0; font-size: 16px;">We received a request to reset your password. Use the verification code below to proceed:</p>
                            
                            <!-- Verification Code Box -->
                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 32px 0;">
                                <tr>
                                    <td align="center">
                                        <div style="background: linear-gradient(135deg, #ede9fe 0%%, #dbeafe 100%%); border: 3px dashed #7B5FF5; border-radius: 12px; padding: 24px; display: inline-block;">
                                            <p style="color: #64748b; font-size: 14px; margin: 0 0 8px 0; text-transform: uppercase; letter-spacing: 1px; font-weight: 600;">Verification Code</p>
                                            <p style="color: #7B5FF5; font-size: 48px; font-weight: 700; margin: 0; letter-spacing: 8px; font-family: 'Courier New', monospace;">%s</p>
                                        </div>
                                    </td>
                                </tr>
                            </table>
                            
                            <p style="color: #64748b; line-height: 1.6; margin: 24px 0 0 0; font-size: 14px;">This code will expire in <strong style="color: #ef4444;">10 minutes</strong>.</p>
                            
                            <!-- Warning Box -->
                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 24px 0 0 0;">
                                <tr>
                                    <td style="background-color: #fef3c7; border-left: 4px solid #f59e0b; border-radius: 8px; padding: 16px;">
                                        <p style="color: #92400e; margin: 0; font-size: 14px; line-height: 1.5;">
                                            <strong>⚠️ Security Notice:</strong> If you didn't request this password reset, please ignore this email. Your account is safe.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 32px; text-align: center; border-top: 1px solid #e2e8f0;">
                            <p style="color: #94a3b8; margin: 0 0 8px 0; font-size: 14px;">This is an automated message from <strong style="color: #64748b;">LAMMA Security</strong></p>
                            <p style="color: #cbd5e1; margin: 0; font-size: 12px;">© 2026 LAMMA. All rights reserved.</p>
                        </td>
                    </tr>
                    
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
""".formatted(userName, code);
    }
}