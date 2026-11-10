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

package org.onlyoffice.sdk.service.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;

import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Status;
import com.onlyoffice.model.settings.SettingsConstants;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.ActiveWebContext;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper;
import org.onlyoffice.test.TestHelper.FieldOverrides;
import org.onlyoffice.test.TestHelper.FrameworkProperties;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestCallbackService {
    private static final String USER = "jdoe";
    private static final String CO_EDITOR = "bob";
    private static final String FILE_URL = "http://docs.example.com/cache/files/output.docx";
    private static final String SAVED_CONTENT = "saved by ONLYOFFICE";
    private static final String MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String KEY_PROPERTY = "onlyoffice." + SettingsConstants.SECURITY_KEY;
    private static final int SC_CONFLICT = 409;

    @Inject
    private CallbackService callbackService;

    @Inject
    private DocumentManager documentManager;

    @Inject
    private JwtManager jwtManager;

    @Inject
    private CoreSession session;

    @Inject
    private CoreFeature coreFeature;

    private DocumentServerClient documentServerClient;
    private FieldOverrides overrides;
    private FrameworkProperties properties;
    private DocumentModel model;

    @Before
    public void setUp() {
        documentServerClient = mock(DocumentServerClient.class);
        doAnswer(invocation -> {
            byte[] bytes = SAVED_CONTENT.getBytes(StandardCharsets.UTF_8);
            invocation.getArgument(1, OutputStream.class).write(bytes);
            return bytes.length;
        }).when(documentServerClient).getFile(eq(FILE_URL), any(OutputStream.class));

        overrides = new FieldOverrides().set(callbackService, "documentServerClient", documentServerClient);
        properties = new FrameworkProperties().set(KEY_PROPERTY, null);

        model = TestHelper.createFile(session, "doc", "report.docx", MIME_TYPE, "original");
        TestHelper.grant(session, model, USER, SecurityConstants.READ_WRITE);
        TestHelper.grant(session, model, CO_EDITOR, SecurityConstants.READ_WRITE);
    }

    @After
    public void tearDown() {
        overrides.close();
        properties.close();
    }

    private String currentKey(final CoreSession userSession) {
        try (ActiveWebContext ignored = ActiveWebContext.activate(userSession)) {
            return documentManager.getDocumentKey(model.getId(), false);
        }
    }

    private static Callback callback(final Status status, final String key, final String url) {
        Callback callback = new Callback();
        callback.setStatus(status);
        callback.setKey(key);
        callback.setUrl(url);
        return callback;
    }

    private void process(final CoreSession userSession, final Callback callback) throws Exception {
        try (ActiveWebContext ignored = ActiveWebContext.activate(userSession)) {
            callbackService.processCallback(callback, model.getId());
        }
    }

    private String contentOf(final DocumentModel document) throws IOException {
        return ((Blob) document.getPropertyValue("file:content")).getString();
    }

    @Test
    public void editingLocksDocumentForCurrentUser() throws Exception {
        CoreSession userSession = coreFeature.getCoreSession(USER);

        process(userSession, callback(Status.EDITING, currentKey(userSession), null));

        assertEquals(USER, session.getLockInfo(model.getRef()).getOwner());
    }

    @Test
    public void editingKeepsExistingLockWhenCoEditorJoins() throws Exception {
        CoreSession userSession = coreFeature.getCoreSession(USER);
        String key = currentKey(userSession);

        process(userSession, callback(Status.EDITING, key, null));
        process(coreFeature.getCoreSession(CO_EDITOR), callback(Status.EDITING, key, null));

        assertEquals(USER, session.getLockInfo(model.getRef()).getOwner());
    }

    @Test
    public void closedUnlocksDocument() throws Exception {
        CoreSession userSession = coreFeature.getCoreSession(USER);
        String key = currentKey(userSession);

        process(userSession, callback(Status.EDITING, key, null));
        process(userSession, callback(Status.CLOSED, key, null));

        assertNull(session.getLockInfo(model.getRef()));
    }

    @Test
    public void closedOnUnlockedDocumentIsNoop() throws Exception {
        CoreSession userSession = coreFeature.getCoreSession(USER);

        process(userSession, callback(Status.CLOSED, currentKey(userSession), null));

        assertNull(session.getLockInfo(model.getRef()));
    }

    @Test
    public void saveReplacesContentKeepsMetadataAndUnlocks() throws Exception {
        CoreSession userSession = coreFeature.getCoreSession(USER);
        String key = currentKey(userSession);

        process(userSession, callback(Status.EDITING, key, null));
        process(userSession, callback(Status.SAVE, key, FILE_URL));

        DocumentModel saved = session.getDocument(model.getRef());
        Blob blob = (Blob) saved.getPropertyValue("file:content");

        assertEquals(SAVED_CONTENT, contentOf(saved));
        assertEquals("report.docx", blob.getFilename());
        assertEquals(MIME_TYPE, blob.getMimeType());
        assertNull(session.getLockInfo(model.getRef()));
        assertFalse("a minor version is created", session.getVersions(model.getRef()).isEmpty());
    }

    @Test
    public void saveCorruptedIsHandledAsSave() throws Exception {
        CoreSession userSession = coreFeature.getCoreSession(USER);

        process(userSession, callback(Status.SAVE_CORRUPTED, currentKey(userSession), FILE_URL));

        assertEquals(SAVED_CONTENT, contentOf(session.getDocument(model.getRef())));
    }

    @Test
    public void coEditorCanSaveDocumentLockedByFirstEditor() throws Exception {
        CoreSession userSession = coreFeature.getCoreSession(USER);
        String key = currentKey(userSession);

        process(userSession, callback(Status.EDITING, key, null));
        process(coreFeature.getCoreSession(CO_EDITOR), callback(Status.SAVE, key, FILE_URL));

        assertEquals(SAVED_CONTENT, contentOf(session.getDocument(model.getRef())));
        assertNull(session.getLockInfo(model.getRef()));
    }

    @Test
    public void saveRejectsChangesMadeOutsideEditorMeanwhile() throws Exception {
        CoreSession userSession = coreFeature.getCoreSession(USER);
        String key = currentKey(userSession);

        DocumentModel concurrent = session.getDocument(model.getRef());
        concurrent.setPropertyValue("dc:description", "changed outside ONLYOFFICE");
        // the same flag the REST API / Web UI set for user-initiated modifications
        concurrent.putContextData(CoreSession.USER_CHANGE, Boolean.TRUE);
        session.saveDocument(concurrent);
        session.save();

        try {
            process(userSession, callback(Status.SAVE, key, FILE_URL));
            fail("Stale document key must be rejected");
        } catch (ConcurrentUpdateException e) {
            assertEquals("original", contentOf(session.getDocument(model.getRef())));
        }
    }

    @Test
    public void saveFailsWithConflictWhenDocumentHasNoContent() throws Exception {
        DocumentModel empty = TestHelper.createFile(session, "empty", null, null, null);

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            callbackService.processCallback(callback(Status.SAVE, "key", FILE_URL), empty.getId());
            fail("Expected NuxeoException");
        } catch (NuxeoException e) {
            assertEquals(SC_CONFLICT, e.getStatusCode());
        }
        verify(documentServerClient, never()).getFile(any(String.class), any(OutputStream.class));
    }

    @Test
    public void forcesaveIsIgnored() throws Exception {
        CoreSession userSession = coreFeature.getCoreSession(USER);

        process(userSession, callback(Status.FORCESAVE, currentKey(userSession), FILE_URL));

        assertEquals("original", contentOf(session.getDocument(model.getRef())));
    }

    @Test
    public void verifyCallbackWithoutSecurityReturnsCallbackAsIs() throws Exception {
        Callback callback = callback(Status.EDITING, "key", null);

        assertEquals(callback, callbackService.verifyCallback(callback, null));
    }

    @Test
    public void verifyCallbackReadsPayloadFromBodyToken() throws Exception {
        properties.set(KEY_PROPERTY, "secret");
        Callback signed = callback(Status.SAVE, "key", FILE_URL);
        Callback request = callback(Status.EDITING, "tampered", null);
        request.setToken(jwtManager.createToken(signed));

        Callback verified = callbackService.verifyCallback(request, null);

        assertEquals(Status.SAVE, verified.getStatus());
        assertEquals("key", verified.getKey());
        assertEquals(FILE_URL, verified.getUrl());
    }

    @Test(expected = SecurityException.class)
    public void verifyCallbackRequiresTokenWhenSecurityEnabled() throws Exception {
        properties.set(KEY_PROPERTY, "secret");

        callbackService.verifyCallback(callback(Status.SAVE, "key", FILE_URL), null);
    }

    @Test
    public void verifyCallbackRejectsTokenSignedWithAnotherKey() throws Exception {
        properties.set(KEY_PROPERTY, "another-secret");
        Callback request = callback(Status.SAVE, "key", FILE_URL);
        request.setToken(jwtManager.createToken(request));
        properties.set(KEY_PROPERTY, "secret");

        try {
            callbackService.verifyCallback(request, null);
            fail("Token signed with a different key must be rejected");
        } catch (Exception e) {
            assertTrue(e.getClass().getName(), e.getClass().getName().contains("JWT")
                    || e.getClass().getName().contains("Signature"));
        }
    }
}
