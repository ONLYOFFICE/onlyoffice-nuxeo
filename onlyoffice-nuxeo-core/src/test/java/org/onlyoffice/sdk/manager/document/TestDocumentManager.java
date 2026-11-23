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

package org.onlyoffice.sdk.manager.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.inject.Inject;

import com.onlyoffice.manager.document.DocumentManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.ActiveWebContext;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper;
import org.onlyoffice.utils.Utils;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestDocumentManager {

    @Inject
    private DocumentManager documentManager;

    @Inject
    private Utils utils;

    @Inject
    private CoreSession session;

    private DocumentModel createDoc() {
        return TestHelper.createFile(session, "doc", "report.docx", "application/octet-stream", "x");
    }

    @Test
    public void documentKeyEncodesIdAndUserChangeToken() {
        DocumentModel model = createDoc();
        String userChangeToken = utils.getUserChangeToken(model.getChangeToken());

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            String key = documentManager.getDocumentKey(model.getId(), false);
            String decoded = new String(Base64.getDecoder().decode(key), StandardCharsets.UTF_8);

            assertEquals(model.getId() + "__" + userChangeToken, decoded);
            assertEquals(userChangeToken, utils.getChangeToken(key));
        }
    }

    @Test
    public void embeddedDocumentKeyIsDifferent() {
        DocumentModel model = createDoc();

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            String key = documentManager.getDocumentKey(model.getId(), false);
            String embeddedKey = documentManager.getDocumentKey(model.getId(), true);
            String decoded = new String(Base64.getDecoder().decode(embeddedKey), StandardCharsets.UTF_8);

            assertNotEquals(key, embeddedKey);
            assertTrue(decoded.endsWith("_embedded"));
        }
    }

    @Test
    public void documentKeyChangesWhenUserModifiesDocument() {
        DocumentModel model = createDoc();

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            String before = documentManager.getDocumentKey(model.getId(), false);

            model.setPropertyValue("dc:description", "changed");
            model.putContextData(CoreSession.USER_CHANGE, Boolean.TRUE);
            session.saveDocument(model);
            session.save();

            assertNotEquals(before, documentManager.getDocumentKey(model.getId(), false));
        }
    }

    @Test
    public void documentKeyStaysWhenDocumentIsUpdatedInBackground() {
        DocumentModel model = createDoc();
        String changeToken = session.getDocument(model.getRef()).getChangeToken();

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            String before = documentManager.getDocumentKey(model.getId(), false);

            // what listeners such as the thumbnail update do: a save not marked as a user change
            model.setPropertyValue("dc:description", "changed");
            session.saveDocument(model);
            session.save();

            assertNotEquals(changeToken, session.getDocument(model.getRef()).getChangeToken());
            assertEquals(before, documentManager.getDocumentKey(model.getId(), false));
        }
    }

    @Test
    public void documentNameIsBlobFileName() {
        DocumentModel model = createDoc();

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            assertEquals("report.docx", documentManager.getDocumentName(model.getId()));
        }
    }
}
