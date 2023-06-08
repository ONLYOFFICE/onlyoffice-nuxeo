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

package org.onlyoffice.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.api.ConvertService;
import org.onlyoffice.api.SettingsService;
import org.onlyoffice.constants.SettingsConstants;
import org.onlyoffice.utils.RequestManager;
import org.onlyoffice.utils.UrlManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class SettingsServiceImpl extends DefaultComponent implements SettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsServiceImpl.class);

    @Override
    public Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<>();

        String docUrl = Framework.getProperty(SettingsConstants.DOC_SERVER_URL, "http://127.0.0.1/");
        String jwtSecret = Framework.getProperty(SettingsConstants.JWT_SECRET, null);
        String jwtHeader = Framework.getProperty(SettingsConstants.JWT_HEADER, null);
        String docInnerUrl = Framework.getProperty(SettingsConstants.DOC_SERVER_INNER_URL, null);
        String nuxeoInnerUrl = Framework.getProperty(SettingsConstants.NUXEO_SERVER_INNER_URL, null);

        settings.put("docUrl", docUrl);
        settings.put("jwtSecret", jwtSecret);
        settings.put("jwtHeader", jwtHeader);
        settings.put("docInnerUrl", docInnerUrl);
        settings.put("nuxeoInnerUrl", nuxeoInnerUrl);

        return settings;
    }

    @Override
    public void updateSettings(JSONObject settings) throws IOException {
        Properties properties = Framework.getProperties();

        if (settings.has("docUrl")) {
            properties.put(SettingsConstants.DOC_SERVER_URL, settings.getString("docUrl").trim());
        }

        if (settings.has("jwtSecret")) {
            properties.put(SettingsConstants.JWT_SECRET, settings.getString("jwtSecret").trim());
        }

        if (settings.has("jwtHeader")) {
            properties.put(SettingsConstants.JWT_HEADER, settings.getString("jwtHeader").trim());
        }

        if (settings.has("docInnerUrl")) {
            properties.put(SettingsConstants.DOC_SERVER_INNER_URL, settings.getString("docInnerUrl").trim());
        }

        if (settings.has("nuxeoInnerUrl")) {
            properties.put(SettingsConstants.NUXEO_SERVER_INNER_URL, settings.getString("nuxeoInnerUrl").trim());
        }

        Properties frameworkProperties = Framework.getProperties();
        Map<String, Object> backupProperties = frameworkProperties.stringPropertyNames()
                .stream()
                .filter(k -> k.startsWith("onlyoffice"))
                .collect(toMap(Function.identity(), frameworkProperties::get));

        File sittingsFile = new File(Environment.getDefault().getConfig(), SettingsConstants.SETTINGS_FILE);
        sittingsFile.createNewFile();

        List<String> listProperties = new ArrayList<>();
        for (Map.Entry<String, Object> property : backupProperties.entrySet()){
            listProperties.add(property.getKey() + "=" + property.getValue());
        }

        FileUtils.writeLines(sittingsFile, listProperties);
    }

    @Override
    public String validateSettings(WebContext ctx) {
        if (!this.checkDocServUrl()) {
            return "docservunreachable";
        }

        try {
            if (!this.checkDocServCommandService()) {
                return "docservcommand";
            }

            if (!this.checkDocServConvert(ctx)) {
                return "docservconvert";
            }
        } catch (SecurityException e) {
            return "jwterror";
        }

        return null;
    }

    private Boolean checkDocServUrl() {
        UrlManager urlManager = Framework.getService(UrlManager.class);
        RequestManager requestManager = Framework.getService(RequestManager.class);

        String url = urlManager.getDocServUrl() + "healthcheck";

        try {
            return requestManager.executeRequestToDocumentServer(url, new RequestManager.Callback<Boolean>() {
                public Boolean doWork(HttpEntity httpEntity) throws IOException {
                    String content = IOUtils.toString(httpEntity.getContent(), "utf-8").trim();
                    return content.equalsIgnoreCase("true");
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private Boolean checkDocServCommandService() {
        RequestManager requestManager = Framework.getService(RequestManager.class);

        JSONObject body = new JSONObject();
        body.put("c", "version");

        try {
            return requestManager.executeRequestToCommandService(body, new RequestManager.Callback<Boolean>() {
                public Boolean doWork(HttpEntity httpEntity) throws IOException, JSONException {
                    String content = IOUtils.toString(httpEntity.getContent(), "utf-8");

                    JSONObject response = new JSONObject(content);

                    if (response.isNull("error")) {
                        return false;
                    }

                    Integer errorCode = response.getInt("error");

                    if (errorCode == 6) {
                        throw new SecurityException();
                    } else if (errorCode != 0) {
                        return false;
                    } else {
                        return true;
                    }
                }
            });
        }catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private Boolean checkDocServConvert(WebContext ctx) {
        ConvertService convertService = Framework.getService(ConvertService.class);
        UrlManager urlManager = Framework.getService(UrlManager.class);
        String key = new SimpleDateFormat("MMddyyyyHHmmss").format(new Date());

        try {
            JSONObject response = convertService.convert(key, "txt", "docx", urlManager.getTestTxtUrl(ctx), "en-US", false);

            if (!response.has("fileUrl") || response.getString("fileUrl").isEmpty()) {
                return false;
            }
        } catch (Exception e) {
            if (e instanceof SecurityException) {
                throw (SecurityException) e;
            }
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }
}
