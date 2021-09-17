/*
 *  This file is part of OpenTSDB.
 *  Copyright (C) 2021 Yahoo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.opentsdb.horizon.alerting.core;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

/**
 * OpenSSL AES CBS Salted RSA256 Encryptor.
 *
 * The output should be the same as:
 * <pre>{@code
 *      > print -n "hello"  | openssl aes-256-cbc -pass "pass:password"  -md sha256 -a -e  -p                                  ~/Projects/corona-notification
 *      salt=1C9C03770516AF33
 *      key=39B6D42B06AB7452FB108F17E2A778281BF15FB7FCCB4BFDC98FD9EDF8338B8B
 *      iv =FB676C1B125E3665D6BAEC19C128389A
 *      U2FsdGVkX18cnAN3BRavM/tSgG5I9kVxsaJjsL325TI=
 * }</pre>
 *
 * The way to decode:
 * <pre>{@code
 *      > echo U2FsdGVkX18cnAN3BRavM/tSgG5I9kVxsaJjsL325TI= | openssl aes-256-cbc -pass "pass:password" -salt  -md sha256 -a -d  -p
 *      salt=1C9C03770516AF33
 *      key=39B6D42B06AB7452FB108F17E2A778281BF15FB7FCCB4BFDC98FD9EDF8338B8B
 *      iv =FB676C1B125E3665D6BAEC19C128389A
 *      hello%
 * }</pre>
 */
public class OpenSSLSaltedAESCBSRSA256Encryptor {

    private static final byte[] SALTED_PREFIX = "Salted__".getBytes(Charsets.UTF_8);

    private final byte[] salt;
    private final Cipher cipher;

    public OpenSSLSaltedAESCBSRSA256Encryptor(final String password) {
        this(password.getBytes(Charsets.UTF_8), generateSalt());
    }

    public OpenSSLSaltedAESCBSRSA256Encryptor(final byte[] password, final byte[] salt) {
        try {
            final MessageDigest sha256 = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_256);
            final byte[][] keyAndIV = EVPBytesToKey(sha256, password, salt);
            final SecretKeySpec keySpec = new SecretKeySpec(keyAndIV[0], "AES");
            final IvParameterSpec ivSpec = new IvParameterSpec(keyAndIV[1]);

            final Cipher aes256Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aes256Cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            this.salt = salt;
            this.cipher = aes256Cipher;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String encryptBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(encrypt(data));
    }

    public String encryptBase64(String data) {
        return Base64.getEncoder().encodeToString(encrypt(data.getBytes(Charsets.UTF_8)));
    }

    public String encryptHex(byte[] data) {
        return Hex.encodeHexString(encrypt(data));
    }

    public byte[] encrypt(byte[] data) {
        final byte[] encrypted;
        try {
            encrypted = cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 8 bytes of `Salted__` prefix, 8 bytes of salt, the rest is encrypted.
        byte[] result = new byte[8 /* Salted__ */ + 8 /* <salt> */ + encrypted.length];
        System.arraycopy(SALTED_PREFIX, 0, result, 0, SALTED_PREFIX.length);
        System.arraycopy(salt, 0, result, SALTED_PREFIX.length, salt.length);
        System.arraycopy(encrypted, 0, result, SALTED_PREFIX.length + salt.length, encrypted.length);
        return result;
    }

    // https://www.openssl.org/docs/man1.1.1/man3/EVP_BytesToKey.html
    // https://antofthy.gitlab.io/info/crypto/openssl.txt
    private static byte[][] EVPBytesToKey(MessageDigest md, byte[] data, byte[] salt) {
        md.reset();
        final ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        final int digestLength = md.getDigestLength();

        // To compute the first hash.
        final byte[] dataAndSalt = new byte[data.length + salt.length];
        System.arraycopy(data, 0, dataAndSalt, 0, data.length);
        System.arraycopy(salt, 0, dataAndSalt, data.length, salt.length);

        final byte[] digestAndDataAndSalt = new byte[digestLength + dataAndSalt.length];
        System.arraycopy(dataAndSalt, 0, digestAndDataAndSalt, digestLength, dataAndSalt.length);

        // Compute D_0 = MD(data || salt)
        byte[] D = md.digest(dataAndSalt);
        byteBuffer.put(D);

        // Compute D_i = MD(D_{i-1} || data || salt)
        while (byteBuffer.position() < 48) {
            System.arraycopy(D, 0, digestAndDataAndSalt, 0, digestLength);
            D = md.digest(digestAndDataAndSalt);
            byteBuffer.put(D);
        }

        byteBuffer.flip();
        final byte[] key = new byte[32];
        byteBuffer.get(key, 0, 32);
        final byte[] iv = new byte[16];
        byteBuffer.get(iv, 0, 16);
        return new byte[][]{key, iv};
    }

    private static byte[] generateSalt() {
        final SecureRandom rand;
        try {
            rand = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        final byte[] salt = new byte[8];
        rand.nextBytes(salt);
        return salt;
    }
}
