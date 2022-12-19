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

package org.onlyoffice.service;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.api.SettingsService;
import org.onlyoffice.constants.SettingsConstants;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class SettingsServiceImpl extends DefaultComponent implements SettingsService {
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

}
