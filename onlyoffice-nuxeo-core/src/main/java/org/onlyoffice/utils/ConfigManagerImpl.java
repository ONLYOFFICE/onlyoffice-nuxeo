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

import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.constants.SettingsConstants;

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

    @Override
    public String getInnerDocServUrl() {
        String innerDocServUrl = Framework.getProperty(SettingsConstants.DOC_SERVER_INNER_URL, null);
        if (innerDocServUrl == null || innerDocServUrl.isEmpty()) {
            return getDocServUrl();
        } else {
            return appendSlash(innerDocServUrl);
        }
    }

    @Override
    public String getBaseNuxeoUrl(WebContext ctx) {
        String nuxeoServerInnerUrl = Framework.getProperty(SettingsConstants.NUXEO_SERVER_INNER_URL, null);
        if (nuxeoServerInnerUrl == null || nuxeoServerInnerUrl.isEmpty()) {
            return appendSlash(ctx.getServerURL().toString());
        } else {
            return appendSlash(nuxeoServerInnerUrl);
        }
    }

    private String getProp(String key, String defValue) {
        return Framework.getProperty(key, defValue);
    }


    @Override
    public String replaceDocEditorURLToInnner(String url) {
        String innerDocEditorUrl = getInnerDocServUrl();
        String publicDocEditorUrl = getDocServUrl();

        if (!publicDocEditorUrl.equals(innerDocEditorUrl)) {
            url = url.replace(publicDocEditorUrl, innerDocEditorUrl);
        }

        return url;
    }

    private String appendSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}
