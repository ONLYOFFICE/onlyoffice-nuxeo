package org.onlyoffice.utils;

import org.json.JSONObject;

public interface JwtManager {
    public Boolean isEnabled();
    public String createToken(JSONObject payload) throws Exception;
    public Boolean verify(String token);
}
