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

package org.onlyoffice.sdk.service.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;

import jakarta.inject.Inject;

import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.ActiveWebContext;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestConfigService {
    private static final String USER = "jdoe";

    @Inject
    private ConfigService configService;

    @Inject
    private CoreSession session;

    @Inject
    private CoreFeature coreFeature;

    private DocumentModel createDoc(final String fileName) {
        return TestHelper.createFile(session, "doc", fileName, "application/octet-stream", "x");
    }

    private static NuxeoPrincipal principal(final String name, final String firstName, final String lastName) {
        NuxeoPrincipal principal = new UserPrincipal(name, Collections.emptyList(), false, false);
        principal.setFirstName(firstName);
        principal.setLastName(lastName);
        return principal;
    }

    @Test
    public void editAllowedForEditableFormatWithWritePermission() {
        DocumentModel model = createDoc("report.docx");

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            assertTrue(configService.getPermissions(model.getId()).getEdit());
        }
    }

    @Test
    public void editDeniedForViewOnlyFormat() {
        DocumentModel model = createDoc("scan.djvu");

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            assertFalse(configService.getPermissions(model.getId()).getEdit());
        }
    }

    @Test
    public void editAllowedForFillableForm() {
        DocumentModel model = createDoc("form.pdf");

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            assertTrue(configService.getPermissions(model.getId()).getEdit());
        }
    }

    @Test
    public void editDeniedWithoutWritePermission() {
        DocumentModel model = createDoc("report.docx");
        TestHelper.grant(session, model, USER, SecurityConstants.READ);

        try (ActiveWebContext ignored = ActiveWebContext.activate(coreFeature.getCoreSession(USER))) {
            assertFalse(configService.getPermissions(model.getId()).getEdit());
        }
    }

    @Test
    public void editorConfigHasCallbackOnlyWhenEditable() {
        DocumentModel model = createDoc("report.docx");
        TestHelper.grant(session, model, USER, SecurityConstants.READ);

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            Config config = configService.createConfig(model.getId(), Mode.EDIT, Type.DESKTOP);

            assertEquals(DocumentType.WORD, config.getDocumentType());
            assertEquals("report.docx", config.getDocument().getTitle());
            assertEquals("docx", config.getDocument().getFileType());
            assertNotNull(config.getDocument().getUrl());
            assertNotNull(config.getEditorConfig().getCallbackUrl());
        }

        try (ActiveWebContext ignored = ActiveWebContext.activate(coreFeature.getCoreSession(USER))) {
            Config config = configService.createConfig(model.getId(), Mode.EDIT, Type.DESKTOP);

            assertNull(config.getEditorConfig().getCallbackUrl());
        }
    }

    @Test
    public void userContainsIdAndFullName() {
        try (ActiveWebContext context = ActiveWebContext.activate(session)) {
            when(context.getWebContext().getPrincipal()).thenReturn(principal(USER, "John", "Doe"));

            User user = configService.getUser();

            assertEquals(USER, user.getId());
            assertEquals("John Doe", user.getName());
        }
    }

    /**
     * BUG: users without first/last name are shown to co-editors as "null null".
     */
    @Test
    public void userWithoutNamesFallsBackToLogin() {
        try (ActiveWebContext context = ActiveWebContext.activate(session)) {
            when(context.getWebContext().getPrincipal()).thenReturn(principal(USER, null, null));

            assertEquals(USER, configService.getUser().getName());
        }
    }

    /**
     * BUG: a user with only one of the names gets "null" in the displayed name.
     */
    @Test
    public void userWithOnlyFirstName() {
        try (ActiveWebContext context = ActiveWebContext.activate(session)) {
            when(context.getWebContext().getPrincipal()).thenReturn(principal(USER, "John", null));

            assertEquals("John", configService.getUser().getName());
        }
    }
}
