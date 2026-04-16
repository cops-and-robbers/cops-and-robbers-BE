package com.team.cops_and_robbers.common.util;

import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.exception.InfrastructureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class AesEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_256_KEY_LENGTH = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey secretKey;

    public AesEncryptor(@Value("${encryption.secret-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != AES_256_KEY_LENGTH) {
            throw new InfrastructureException(CommonException.INVALID_ENCRYPTION_KEY);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = generateRandomIv();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(prependIvTo(encryptedData, iv));

        } catch (Exception e) {
            throw new InfrastructureException(CommonException.ENCRYPTION_FAILED);
        }
    }

    public String decrypt(String base64CipherText) {
        try {
            byte[] ivPrependedData = Base64.getDecoder().decode(base64CipherText);
            byte[] iv = extractIvFrom(ivPrependedData);
            byte[] encryptedData = extractEncryptedDataFrom(ivPrependedData);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(encryptedData), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new InfrastructureException(CommonException.DECRYPTION_FAILED);
        }
    }

    private byte[] generateRandomIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    private byte[] prependIvTo(byte[] encryptedData, byte[] iv) {
        byte[] ivPrependedData = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, ivPrependedData, 0, iv.length);
        System.arraycopy(encryptedData, 0, ivPrependedData, iv.length, encryptedData.length);
        return ivPrependedData;
    }

    private byte[] extractIvFrom(byte[] ivPrependedData) {
        return Arrays.copyOfRange(ivPrependedData, 0, GCM_IV_LENGTH);
    }

    private byte[] extractEncryptedDataFrom(byte[] ivPrependedData) {
        return Arrays.copyOfRange(ivPrependedData, GCM_IV_LENGTH, ivPrependedData.length);
    }
}
