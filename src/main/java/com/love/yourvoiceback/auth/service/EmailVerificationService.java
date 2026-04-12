package com.love.yourvoiceback.auth.service;

import com.love.yourvoiceback.auth.domain.EmailVerification;
import com.love.yourvoiceback.auth.repository.EmailVerificationRepository;
import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${auth.email-verification.expiration-seconds}")
    private long expirationSeconds;

    @Value("${auth.email-verification.from-address}")
    private String fromAddress;

    @Transactional
    public void sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw ApiException.error(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        String code = generateCode();
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseGet(() -> EmailVerification.builder()
                        .email(email)
                        .build());

        verification.setCode(code);
        verification.setExpiresAt(LocalDateTime.now().plusSeconds(expirationSeconds));
        verification.setVerifiedAt(null);
        emailVerificationRepository.save(verification);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject("[Your Voice Back] Email Verification Code");
            message.setText("""
                    안녕하세요.

                    Your Voice Back 이메일 인증번호는 %s 입니다.
                    인증번호는 %d분 동안 유효합니다.
                    """.formatted(code, expirationSeconds / 60));
            mailSender.send(message);
        } catch (MailException ex) {
            throw ApiException.error(ErrorCode.EMAIL_VERIFICATION_SEND_FAILED, ex);
        }
    }

    @Transactional
    public void verifyCode(String email, String code) {
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.error(ErrorCode.EMAIL_VERIFICATION_CODE_INVALID));

        if (verification.isExpired()) {
            throw ApiException.error(ErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED);
        }
        if (!verification.getCode().equals(code)) {
            throw ApiException.error(ErrorCode.EMAIL_VERIFICATION_CODE_INVALID);
        }

        verification.setVerifiedAt(LocalDateTime.now());
    }

    @Transactional
    public void ensureVerified(String email) {
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.error(ErrorCode.EMAIL_NOT_VERIFIED));

        if (!verification.isVerified()) {
            throw ApiException.error(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    @Transactional
    public void clearVerification(String email) {
        emailVerificationRepository.deleteByEmail(email);
    }

    private String generateCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
