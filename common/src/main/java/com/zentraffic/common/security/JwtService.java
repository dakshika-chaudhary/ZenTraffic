package com.zentraffic.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class JwtService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String secret;
    private final long ttlSeconds;

    public JwtService(String secret, long ttlSeconds) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String generate(Long userId, String email, String role) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", email);
            payload.put("userId", userId);
            payload.put("role", role);
            payload.put("iat", Instant.now().getEpochSecond());
            payload.put("exp", Instant.now().plusSeconds(ttlSeconds).getEpochSecond());
            String unsigned = encode(MAPPER.writeValueAsBytes(header)) + "." + encode(MAPPER.writeValueAsBytes(payload));
            return unsigned + "." + sign(unsigned);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate JWT", ex);
        }
    }

    public Map<String, Object> validate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token format");
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!sign(unsigned).equals(parts[2])) {
                throw new IllegalArgumentException("Invalid token signature");
            }
            Map<String, Object> payload = MAPPER.readValue(Base64.getUrlDecoder().decode(parts[1]), new TypeReference<>() {
            });
            Number exp = (Number) payload.get("exp");
            if (exp == null || exp.longValue() < Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("Token expired");
            }
            return payload;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid token", ex);
        }
    }

    private String sign(String unsigned) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return encode(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
