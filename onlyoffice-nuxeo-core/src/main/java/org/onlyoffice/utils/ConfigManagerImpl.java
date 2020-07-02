package org.onlyoffice.utils;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class ConfigManagerImpl extends DefaultComponent implements ConfigManager {

    private static final String JWT_SECRET = "onlyoffice.jwt.secret";
    private static final String DOCSERV_URL = "onlyoffice.docserv.url";

    @Override
    public String getJwtSecret() {
        return getProp(JWT_SECRET, null);
    }

    @Override
    public String getDocServUrl() {
        return appendSlash(getProp(DOCSERV_URL, "http://127.0.0.1/"));
    }

    private String getProp(String key, String defValue) {
        return Framework.getProperty(key, defValue);
    }

    private String appendSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}
