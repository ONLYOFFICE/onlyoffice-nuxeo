/**
 *
 * (c) Copyright Ascensio System SIA 2023
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
import java.util.Iterator;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.constants.SettingsConstants;

public class JwtManagerImpl extends DefaultComponent implements JwtManager {

    private static final long ACCEPT_LEEWAY = 3;

    @Override
    public Boolean isEnabled() {
        String secret = getJwtSecret();
        return secret != null && !secret.isEmpty();
    }

    @Override
    public String createToken(JSONObject payload) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, ?> payloadMap = objectMapper.readValue(payload.toString(), Map.class);

        return createToken(payloadMap, getJwtSecret());
    }

    @Override
    public String verify(String token) {
        return verifyToken(token, getJwtSecret());
    }

    public String getJwtHeader() {
        String jwtHeader = Framework.getProperty(SettingsConstants.JWT_HEADER, null);
        return jwtHeader == null || jwtHeader.isEmpty() ? "Authorization" : jwtHeader;
    }

    private String getJwtSecret() {
        return Framework.getProperty(SettingsConstants.JWT_SECRET, null);
    }

    private String createToken(final Map<String, ?> payloadMap, final String key) {
        Algorithm algorithm = Algorithm.HMAC256(key);

        JWTCreator.Builder builder = JWT.create();

        Iterator var2 = payloadMap.entrySet().iterator();

        while(var2.hasNext()) {
            Map.Entry<String, ?> entry = (Map.Entry)var2.next();
            if (entry.getValue() instanceof Map) {
                builder.withClaim(entry.getKey(), (Map) entry.getValue());
            } else {
                builder.withClaim(entry.getKey(), (String) entry.getValue());
            }
        }

        String token = builder.sign(algorithm);

        return token;
    }

    private String verifyToken(final String token, final String key) {
        Algorithm algorithm = Algorithm.HMAC256(key);
        Base64.Decoder decoder = Base64.getUrlDecoder();

        DecodedJWT jwt = JWT.require(algorithm)
                .acceptLeeway(ACCEPT_LEEWAY)
                .build()
                .verify(token);

        return new String(decoder.decode(jwt.getPayload()));
    }
}
