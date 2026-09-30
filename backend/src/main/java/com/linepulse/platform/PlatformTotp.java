package com.linepulse.platform;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class PlatformTotp {
    private final String secret;
    private final JdbcTemplate jdbc;
    public PlatformTotp(@Value("${PLATFORM_TOTP_SECRET:}") String secret, JdbcTemplate jdbc) {
        this.secret = secret; this.jdbc = jdbc;
    }
    public void validateConfiguration() {
        if (!secret.matches("[A-Z2-7]{32,128}")) throw new IllegalStateException("Configure PLATFORM_TOTP_SECRET com uma chave Base32 de pelo menos 32 caracteres.");
    }
    public boolean consume(UUID userId, String code) {
        if (code == null || !code.matches("[0-9]{6}") || secret.isBlank()) return false;
        long current = Instant.now().getEpochSecond() / 30;
        for (long step = current - 1; step <= current + 1; step++) {
            if (java.security.MessageDigest.isEqual(code.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    codeAt(secret, step).getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                return jdbc.update("UPDATE users SET platform_totp_counter=? WHERE id=? AND platform_admin=TRUE AND active=TRUE AND platform_totp_counter<?", step, userId, step) == 1;
            }
        }
        return false;
    }
    public static String codeAt(String secret, long step) {
        try {
            String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
            java.io.ByteArrayOutputStream decoded = new java.io.ByteArrayOutputStream();
            int buffer = 0, bits = 0;
            for (char character : secret.toUpperCase(Locale.ROOT).toCharArray()) {
                int value = alphabet.indexOf(character);
                if (value < 0) throw new IllegalArgumentException("Invalid Base32");
                buffer = (buffer << 5) | value; bits += 5;
                if (bits >= 8) { bits -= 8; decoded.write((buffer >> bits) & 255); }
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decoded.toByteArray(), "HmacSHA1"));
            byte[] digest = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = digest[digest.length - 1] & 15;
            int binary = ((digest[offset] & 127) << 24) | ((digest[offset + 1] & 255) << 16)
                    | ((digest[offset + 2] & 255) << 8) | (digest[offset + 3] & 255);
            return String.format(Locale.ROOT, "%06d", binary % 1000000);
        } catch (java.security.GeneralSecurityException error) { throw new IllegalStateException(error); }
    }
}
