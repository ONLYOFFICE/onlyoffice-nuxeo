package org.onlyoffice.utils;

import java.util.Base64;
import java.util.Base64.Encoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class JwtManagerImpl extends DefaultComponent implements JwtManager {

    private ConfigManager config;

    @Override
    public Boolean isEnabled() {
        getConfig();
        String secret = getConfig().getJwtSecret();
        return secret != null && !secret.isEmpty();
    }

    @Override
    public String createToken(JSONObject payload) throws Exception {
        JSONObject header = new JSONObject();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Encoder enc = Base64.getUrlEncoder();

        String encHeader = enc.encodeToString(header.toString().getBytes("UTF-8")).replace("=", "");
        String encPayload = enc.encodeToString(payload.toString().getBytes("UTF-8")).replace("=", "");
        String hash = calculateHash(encHeader, encPayload);

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

    private String calculateHash(String header, String payload) throws Exception {
        Mac hasher;
        hasher = getHasher();
        return Base64.getUrlEncoder().encodeToString(hasher.doFinal((header + "." + payload).getBytes("UTF-8"))).replace("=", "");
    }

    private Mac getHasher() throws Exception {
        String jwts = getConfig().getJwtSecret();

        Mac sha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(jwts.getBytes("UTF-8"), "HmacSHA256");
        sha256.init(secret_key);

        return sha256;
    }

    private ConfigManager getConfig() {
        if (config == null) {
            config = Framework.getService(ConfigManager.class);
        }
        return config;
    }
}
