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

package org.onlyoffice.sdk.service.settings;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import jakarta.inject.Inject;

import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.model.common.CommonResponse;
import com.onlyoffice.model.settings.validation.ValidationResult;
import com.onlyoffice.model.settings.validation.status.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.ActiveWebContext;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper.FieldOverrides;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestSettingsValidationService {
    private static final String[] CHECKS = {"documentServer", "commandService", "convertService"};

    @Inject
    private SettingsValidationService settingsValidationService;

    @Inject
    private CoreSession session;

    private DocumentServerClient documentServerClient;
    private FieldOverrides overrides;
    private ActiveWebContext webContext;

    @Before
    public void setUp() {
        documentServerClient = mock(DocumentServerClient.class);
        overrides = new FieldOverrides().set(settingsValidationService, "documentServerClient", documentServerClient);
        webContext = ActiveWebContext.activate(session);
    }

    @After
    public void tearDown() {
        webContext.close();
        overrides.close();
    }

    @Test
    public void allChecksAreReported() {
        Map<String, ValidationResult> result = settingsValidationService.validateSettings();

        assertEquals(CHECKS.length, result.size());
        for (String check : CHECKS) {
            assertEquals(check, true, result.containsKey(check));
        }
    }

    @Test
    public void healthyDocumentServerIsReportedAsSuccess() {
        when(documentServerClient.healthcheck()).thenReturn(true);

        assertEquals(Status.SUCCESS, settingsValidationService.validateSettings().get("documentServer").getStatus());
    }

    @Test
    public void connectionErrorsAreReportedPerCheck() {
        RuntimeException failure = new RuntimeException("Connection refused");
        when(documentServerClient.healthcheck()).thenThrow(failure);
        when(documentServerClient.command(any())).thenThrow(failure);
        when(documentServerClient.convert(any())).thenThrow(failure);

        Map<String, ValidationResult> result = settingsValidationService.validateSettings();

        for (String check : CHECKS) {
            assertEquals(check, Status.FAILED, result.get(check).getStatus());
            assertEquals(check, CommonResponse.Error.CONNECTION, result.get(check).getError());
        }
    }
}
