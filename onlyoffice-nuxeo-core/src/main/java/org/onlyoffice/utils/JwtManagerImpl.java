/**
 *
 * (c) Copyright Ascensio System SIA 2022
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onlyoffice.utils;

import java.util.Base64;
import java.util.Base64.Encoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.constants.SettingsConstants;

public class JwtManagerImpl extends DefaultComponent implements JwtManager {

    @Override
    public Boolean isEnabled() {
        String secret = getJwtSecret();
        return secret != null && !secret.isEmpty();
    }

    @Override
    public String createToken(JSONObject payload) {
        JSONObject header = new JSONObject();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Encoder enc = Base64.getUrlEncoder();

        String encHeader = null;
        String encPayload = null;
        String hash = null;

        try {
            encHeader = enc.encodeToString(header.toString().getBytes("UTF-8")).replace("=", "");
            encPayload = enc.encodeToString(payload.toString().getBytes("UTF-8")).replace("=", "");
            hash = calculateHash(encHeader, encPayload);
        } catch (Exception e) {
            throw ExceptionUtils.runtimeException(e);
        }

        return encHeader + "." + encPayload + "." + hash;
    }

    @Override
    public Boolean verify(String token) {
        if (!isEnabled()) return false;

        String[] jwt = token.split("\\.");
        if (jwt.length != 3) {
            return false;
        }

        try {
            String hash = calculateHash(jwt[0], jwt[1]);
            if (!hash.equals(jwt[2])) return false;
        } catch(Exception ex) {
            return false;
        }

        return true;
    }

    public String getJwtHeader() {
        String jwtHeader = Framework.getProperty(SettingsConstants.JWT_HEADER, null);
        return jwtHeader == null || jwtHeader.isEmpty() ? "Authorization" : jwtHeader;
    }

    private String getJwtSecret() {
        return Framework.getProperty(SettingsConstants.JWT_SECRET, null);
    }

    private String calculateHash(String header, String payload) throws Exception {
        Mac hasher;
        hasher = getHasher();
        return Base64.getUrlEncoder().encodeToString(hasher.doFinal((header + "." + payload).getBytes("UTF-8"))).replace("=", "");
    }

    private Mac getHasher() throws Exception {
        String jwts = getJwtSecret();

        Mac sha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(jwts.getBytes("UTF-8"), "HmacSHA256");
        sha256.init(secret_key);

        return sha256;
    }
}
