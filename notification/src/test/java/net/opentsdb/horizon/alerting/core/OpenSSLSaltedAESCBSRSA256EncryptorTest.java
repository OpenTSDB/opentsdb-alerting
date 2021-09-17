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

import com.google.common.base.Charsets;
import net.opentsdb.horizon.alerting.core.OpenSSLSaltedAESCBSRSA256Encryptor;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenSSLSaltedAESCBSRSA256EncryptorTest {

    /**
     * Test against OpenSSL encryption:
     *
     * Test are generated with:
     * > print -n "hello"  | openssl aes-256-cbc -pass "pass:password"  -md sha256 -a -e  -p                                  ~/Projects/corona-notification
     * salt=1C9C03770516AF33
     * key=39B6D42B06AB7452FB108F17E2A778281BF15FB7FCCB4BFDC98FD9EDF8338B8B
     * iv =FB676C1B125E3665D6BAEC19C128389A
     * U2FsdGVkX18cnAN3BRavM/tSgG5I9kVxsaJjsL325TI=
     */
    @Test
    void testEncryption() throws DecoderException {
        final byte[] password = "password".getBytes(Charsets.UTF_8);
        final byte[] data = "hello".getBytes(Charsets.UTF_8);

        final Map<String, String> saltEncrypted = new HashMap<>();
        saltEncrypted.put("3CA781AEE4050A89", "U2FsdGVkX188p4Gu5AUKiVhvF3ebLNON4anoTi0iB08=");
        saltEncrypted.put("1C9C03770516AF33", "U2FsdGVkX18cnAN3BRavM/tSgG5I9kVxsaJjsL325TI=");

        for (Map.Entry<String,String> entry: saltEncrypted.entrySet()) {
            final OpenSSLSaltedAESCBSRSA256Encryptor encryptor =
                    new OpenSSLSaltedAESCBSRSA256Encryptor(password, Hex.decodeHex(entry.getKey()));
            assertEquals(entry.getValue(), encryptor.encryptBase64(data));
        }
    }
}