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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import com.onlyoffice.manager.document.DocumentManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.ActiveWebContext;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper;
import org.onlyoffice.utils.Utils;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestDocumentManager {
    private static final String SECRET_STORE = "onlyoffice";
    private static final String SECRET_KEY = "onlyoffice:link-token-secret";
    private static final int SECRET_LENGTH = 32;

    @Inject
    private DocumentManager documentManager;

    @Inject
    private Utils utils;

    @Inject
    private CoreSession session;

    @Inject
    private AutomationService automationService;

    private DocumentModel createDoc() {
        return TestHelper.createFile(session, "doc", "report.docx", "application/octet-stream", "x");
    }

    private static Blob newContent() {
        return newContent("replaced");
    }

    private static Blob newContent(final String content) {
        return Blobs.createBlob(content, "application/octet-stream", StandardCharsets.UTF_8.name(), "report.docx");
    }

    private DocumentModel replaceContent(final DocumentModel model, final String content) {
        DocumentModel current = session.getDocument(model.getRef());
        current.setPropertyValue("file:content", (Serializable) newContent(content));
        current = session.saveDocument(current);
        session.save();
        return current;
    }

    @Test
    public void documentKeyIsNotReusedForOtherContentAfterVersionRestore() {
        DocumentModel model = createDoc();

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            DocumentRef version = session.checkIn(model.getRef(), VersioningOption.MAJOR, null);
            session.save();
            replaceContent(model, "replaced");
            String replacedKey = documentManager.getDocumentKey(model.getId(), false);

            // restoring brings back the change token of the version, so the next replacement repeats a token
            session.restoreToVersion(model.getRef(), version);
            session.save();
            replaceContent(model, "replaced again");

            assertNotEquals(replacedKey, documentManager.getDocumentKey(model.getId(), false));
        }
    }

    @Test
    public void documentKeyChangesWhenContentIsReplacedWithoutUserChangeFlag() {
        DocumentModel model = createDoc();

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            String before = documentManager.getDocumentKey(model.getId(), false);

            model.setPropertyValue("file:content", (Serializable) newContent());
            session.saveDocument(model);
            session.save();

            assertNotEquals(before, documentManager.getDocumentKey(model.getId(), false));
        }
    }

    @Test
    public void documentKeyChangesWhenContentIsAttachedByAutomation() throws Exception {
        DocumentModel model = createDoc();

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            String before = documentManager.getDocumentKey(model.getId(), false);

            Map<String, Object> params = new HashMap<>();
            params.put("document", model.getRef());
            try (OperationContext ctx = new OperationContext(session)) {
                ctx.setInput(newContent());
                automationService.run(ctx, "Blob.AttachOnDocument", params);
            }
            session.save();

            assertNotEquals(before, documentManager.getDocumentKey(model.getId(), false));
        }
    }

    @Test
    public void documentKeyIsOpaqueAndFitsDocumentServer() {
        DocumentModel model = createDoc();

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            String key = documentManager.getDocumentKey(model.getId(), false);

            assertTrue(key, key.matches("[A-Za-z0-9_-]{43}"));
            assertEquals("the same revision gives the same key", key,
                    documentManager.getDocumentKey(model.getId(), false));
        }
    }

    /**
     * Without JWT, Document Server opens the editing session of whatever key it is given: a key computed from the
     * id, the change token and the file of a document would give access to it.
     */
    @Test
    public void documentKeyCannotBeComputedWithoutTheSecret() {
        DocumentModel model = createDoc();
        KeyValueStore store = Framework.getService(KeyValueService.class).getKeyValueStore(SECRET_STORE);

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            String key = documentManager.getDocumentKey(model.getId(), false);
            byte[] secret = store.get(SECRET_KEY);
            store.put(SECRET_KEY, new byte[SECRET_LENGTH]);
            try {
                assertNotEquals(key, documentManager.getDocumentKey(model.getId(), false));
            } finally {
                store.put(SECRET_KEY, secret);
            }
        }
    }

    @Test
    public void documentsWithTheSameFileHaveDifferentKeys() {
        DocumentModel first = TestHelper.createFile(session, "first", "blank.docx", "application/octet-stream", "x");
        DocumentModel second = TestHelper.createFile(session, "second", "blank.docx", "application/octet-stream", "x");

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            assertNotEquals(documentManager.getDocumentKey(first.getId(), false),
                    documentManager.getDocumentKey(second.getId(), false));
        }
    }

    @Test
    public void documentWithoutContentHasNoName() {
        DocumentModel model = TestHelper.createFile(session, "empty", null, null, null);

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            assertNull(documentManager.getDocumentName(model.getId()));
        }
    }

    /**
     * ONLYOFFICE.Convert may run in an automation chain of a listener or a workflow, outside any web request.
     */
    @Test
    public void documentKeyAndNameAreBuiltOutsideAWebRequest() throws Exception {
        DocumentModel model = createDoc();
        String key;
        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            key = documentManager.getDocumentKey(model.getId(), false);
        }

        try (NuxeoLoginContext ignored = TestHelper.loginAs(session)) {
            assertEquals(key, documentManager.getDocumentKey(model.getId(), false));
            assertEquals("report.docx", documentManager.getDocumentName(model.getId()));
        }
    }

    @Test
    public void embeddedDocumentKeyIsDifferent() {
        DocumentModel model = createDoc();

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            String key = documentManager.getDocumentKey(model.getId(), false);
            String embeddedKey = documentManager.getDocumentKey(model.getId(), true);

            assertNotEquals(key, embeddedKey);
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
