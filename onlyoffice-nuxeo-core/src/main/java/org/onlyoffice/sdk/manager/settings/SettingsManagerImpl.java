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

package org.onlyoffice.sdk.manager.settings;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.onlyoffice.manager.settings.DefaultSettingsManager;
import com.onlyoffice.model.settings.Settings;
import org.apache.commons.io.FileUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class SettingsManagerImpl extends DefaultSettingsManager {
    private static final String SETTINGS_PREFIX = "onlyoffice";
    private final JavaPropsMapper javaPropsMapper = new JavaPropsMapper();

    @Override
    public String getSetting(String name) {
        return Framework.getProperty(SETTINGS_PREFIX + "." + name);
    }

    @Override
    public void setSetting(String name, String value) {
        Properties properties = new Properties();

        properties.put(name, value);

        try {
            setSettings(properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setSettings(final Settings settings)  {
        try {
            Properties settingsProperties = new Properties();
            Map<String, String> settingsMap = convertObjectToDotNotationMap(settings);

            for (Map.Entry<String, String> setting : settingsMap.entrySet()){
                settingsProperties.setProperty(setting.getKey(), setting.getValue());
            }

            setSettings(settingsProperties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void setSettings(Properties properties) throws IOException {
        Properties frameworkProperties = Framework.getProperties();

        Set<String>propertyNames = properties.stringPropertyNames();
        for(String propertyName : propertyNames) {
            frameworkProperties.put(SETTINGS_PREFIX + "." + propertyName, properties.get(propertyName));
        }

        frameworkProperties = Framework.getProperties();

        Map<String, Object> backupProperties = frameworkProperties.stringPropertyNames()
                .stream()
                .filter(k -> k.startsWith("onlyoffice"))
                .collect(toMap(Function.identity(), frameworkProperties::get));

        File sittingsFile = new File(Environment.getDefault().getConfig(), "onlyoffice.properties");
        sittingsFile.createNewFile();

        List<String> listProperties = new ArrayList<>();
        for (Map.Entry<String, Object> property : backupProperties.entrySet()){
            listProperties.add(property.getKey() + "=" + property.getValue());
        }

        FileUtils.writeLines(sittingsFile, listProperties);
    }
}
