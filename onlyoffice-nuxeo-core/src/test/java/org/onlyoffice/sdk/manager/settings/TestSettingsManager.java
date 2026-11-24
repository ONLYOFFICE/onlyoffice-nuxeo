/**
 *
 * (c) Copyright Ascensio System SIA 2026
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.inject.Inject;

import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.settings.Settings;
import com.onlyoffice.model.settings.SettingsConstants;
import com.onlyoffice.model.settings.security.Security;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper.FrameworkProperties;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestSettingsManager {
    private static final String URL_PROPERTY = "onlyoffice." + SettingsConstants.URL;
    private static final String KEY_PROPERTY = "onlyoffice." + SettingsConstants.SECURITY_KEY;
    private static final String HEADER_PROPERTY = "onlyoffice." + SettingsConstants.SECURITY_HEADER;
    private static final String EXTRA_PROPERTY = "onlyoffice.extra";
    private static final String OTHER_ADDON_PROPERTY = "onlyoffice-other-addon.password";

    @Inject
    private SettingsManager settingsManager;

    private FrameworkProperties properties;

    private File settingsFile() {
        return new File(Environment.getDefault().getConfig(), "onlyoffice.properties");
    }

    /**
     * Reads the persisted settings the same way the Nuxeo runtime does on startup.
     */
    private Properties loadPersistedSettings() throws IOException {
        Properties persisted = new Properties();
        try (Reader reader = Files.newBufferedReader(settingsFile().toPath())) {
            persisted.load(reader);
        }
        return persisted;
    }

    private void save(final String url, final String key) throws Exception {
        settingsManager.setSettings(Settings.builder()
                .url(url)
                .security(Security.builder().key(key).header("AuthorizationJwt").build())
                .build());
    }

    @Before
    public void setUp() throws IOException {
        properties = new FrameworkProperties()
                .set(URL_PROPERTY, null)
                .set(KEY_PROPERTY, null)
                .set(HEADER_PROPERTY, null)
                .set(EXTRA_PROPERTY, null)
                .set(OTHER_ADDON_PROPERTY, null);
        Files.deleteIfExists(settingsFile().toPath());
    }

    @After
    public void tearDown() throws IOException {
        properties.close();
        Files.deleteIfExists(settingsFile().toPath());
    }

    @Test
    public void getSettingReadsPrefixedFrameworkProperty() {
        properties.set(URL_PROPERTY, "http://docs.example.com/");

        assertEquals("http://docs.example.com/", settingsManager.getSetting(SettingsConstants.URL));
    }

    @Test
    public void securityIsDisabledWithoutKey() {
        assertFalse(settingsManager.isSecurityEnabled());

        properties.set(KEY_PROPERTY, "secret");

        assertTrue(settingsManager.isSecurityEnabled());
    }

    @Test
    public void setSettingsUpdatesFrameworkProperties() throws Exception {
        save("http://docs.example.com/", "secret");

        assertEquals("http://docs.example.com/", Framework.getProperty(URL_PROPERTY));
        assertEquals("secret", settingsManager.getSetting(SettingsConstants.SECURITY_KEY));
        assertEquals("AuthorizationJwt", settingsManager.getSetting(SettingsConstants.SECURITY_HEADER));
    }

    @Test
    public void settingsFileHoldsOnlyTheSettingsOfThisAddon() throws Exception {
        properties.set(OTHER_ADDON_PROPERTY, "not ours");

        save("http://docs.example.com/", "secret");

        assertFalse(loadPersistedSettings().containsKey(OTHER_ADDON_PROPERTY));
    }

    @Test
    public void setSettingsPersistsSettingsFile() throws Exception {
        save("http://docs.example.com/", "secret");

        Properties persisted = loadPersistedSettings();
        assertEquals("http://docs.example.com/", persisted.getProperty(URL_PROPERTY));
        assertEquals("secret", persisted.getProperty(KEY_PROPERTY));
    }

    /**
     * BUG: the settings file is written as raw {@code key=value} lines, so characters that are special for the
     * properties format (backslash, leading spaces, line breaks) are lost when Nuxeo reloads the file on restart.
     */
    @Test
    public void persistedSettingsSurviveReloadWithSpecialCharacters() throws Exception {
        String key = "  se\\cr=et:#!\n line ü中";

        save("http://docs.example.com/", key);

        assertEquals(key, loadPersistedSettings().getProperty(KEY_PROPERTY));
    }

    @Test
    public void extraIsNotPersisted() throws Exception {
        Map<String, Object> extra = new HashMap<>();
        extra.put("pathApiUrl", "/web-apps/apps/api/documents/api.js");
        extra.put("unknownField", "value");

        settingsManager.setSettings(Settings.builder().url("http://docs.example.com/").extra(extra).build());

        assertNull(Framework.getProperty(EXTRA_PROPERTY));
        Properties persisted = loadPersistedSettings();
        assertFalse(persisted.containsKey(EXTRA_PROPERTY));
        assertEquals("http://docs.example.com/", persisted.getProperty(URL_PROPERTY));
    }

    @Test
    public void extraSavedByEarlierVersionsIsRemoved() throws Exception {
        properties.set(EXTRA_PROPERTY, "{pathApiUrl=/web-apps/apps/api/documents/api.js}");

        save("http://docs.example.com/", "secret");

        assertNull(Framework.getProperty(EXTRA_PROPERTY));
        assertFalse(loadPersistedSettings().containsKey(EXTRA_PROPERTY));
    }
}
