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

package org.onlyoffice.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.service.convert.ConvertService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.service.LockService;
import org.onlyoffice.test.ActiveWebContext;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestConvertOperation {
    private static final String USER = "jdoe";
    private static final String RESULT_URL = "http://docs.example.com/cache/files/result.docx";
    private static final String CONVERTED = "converted content";
    private static final String DOCX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final int HALF = 50;
    private static final int SC_BAD_REQUEST = 400;
    private static final int SC_CONFLICT = 409;

    @Mock
    @RuntimeService
    private ConvertService convertService;

    @Mock
    @RuntimeService
    private DocumentServerClient documentServerClient;

    @Inject
    private AutomationService automationService;

    @Inject
    private DocumentManager documentManager;

    @Inject
    private LockService lockService;

    @Inject
    private CoreSession session;

    @Inject
    private CoreFeature coreFeature;

    private DocumentModel createDoc(final String fileName) {
        return TestHelper.createFile(session, "doc", fileName, "application/msword", "legacy " + UUID.randomUUID());
    }

    private String convert(final CoreSession userSession, final DocumentModel model, final String language)
            throws OperationException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", model.getId());
        if (language != null) {
            params.put("language", language);
        }

        try (OperationContext ctx = new OperationContext(userSession)) {
            return (String) automationService.run(ctx, ConvertOperation.ID, params);
        }
    }

    private void respond(final boolean endConvert) throws Exception {
        ConvertResponse response = new ConvertResponse();
        response.setEndConvert(endConvert);
        response.setPercent(endConvert ? 2 * HALF : HALF);
        response.setFileUrl(endConvert ? RESULT_URL : null);
        when(convertService.processConvert(any(ConvertRequest.class), any(String.class))).thenReturn(response);

        doAnswer(invocation -> {
            byte[] bytes = CONVERTED.getBytes(StandardCharsets.UTF_8);
            invocation.getArgument(1, OutputStream.class).write(bytes);
            return bytes.length;
        }).when(documentServerClient).getFile(eq(RESULT_URL), any(OutputStream.class));
    }

    @Test
    public void finishedConversionReplacesContent() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        respond(true);

        String json = convert(session, model, null);

        Blob blob = (Blob) session.getDocument(model.getRef()).getPropertyValue("file:content");
        assertEquals("legacy.docx", blob.getFilename());
        assertEquals(CONVERTED, blob.getString());
        assertEquals(true, new ObjectMapper().readTree(json).get("endConvert").asBoolean());
    }

    @Test
    public void finishedConversionChangesDocumentKey() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        respond(true);

        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            String before = documentManager.getDocumentKey(model.getId(), false);

            convert(session, model, null);

            assertNotEquals(before, documentManager.getDocumentKey(model.getId(), false));
        }
    }

    /**
     * BUG: same as for the create operation, the whole file name is passed where an extension is expected.
     */
    @Test
    public void convertedContentHasMimeTypeOfTargetFormat() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        respond(true);

        convert(session, model, null);

        Blob blob = (Blob) session.getDocument(model.getRef()).getPropertyValue("file:content");
        assertEquals(DOCX_MIME_TYPE, blob.getMimeType());
    }

    @Test
    public void conversionRequestTargetsDefaultFormatAndLanguage() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        respond(false);

        convert(session, model, "de-DE");

        ArgumentCaptor<ConvertRequest> request = ArgumentCaptor.forClass(ConvertRequest.class);
        verify(convertService).processConvert(request.capture(), eq(model.getId()));
        assertEquals("docx", request.getValue().getOutputtype());
        assertEquals("de-DE", request.getValue().getRegion());
        assertTrue(request.getValue().getAsync());
    }

    @Test
    public void unfinishedConversionKeepsContentAndReportsProgress() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        respond(false);

        JsonNode json = new ObjectMapper().readTree(convert(session, model, null));

        Blob blob = (Blob) session.getDocument(model.getRef()).getPropertyValue("file:content");
        assertEquals("legacy.doc", blob.getFilename());
        assertEquals(HALF, json.get("percent").asInt());
        verify(documentServerClient, never()).getFile(any(String.class), any(OutputStream.class));
    }

    private String assertBadRequest(final DocumentModel model) throws Exception {
        return assertRejected(model, SC_BAD_REQUEST);
    }

    private String assertRejected(final DocumentModel model, final int status) throws Exception {
        try {
            convert(session, model, null);
            fail("Conversion of " + model.getName() + " must be rejected");
            return null;
        } catch (NuxeoException e) {
            assertEquals(status, e.getStatusCode());
            return e.getMessage();
        } finally {
            verify(convertService, never()).processConvert(any(ConvertRequest.class), any(String.class));
        }
    }

    @Test
    public void conversionKeepsTheOriginalFileInTheHistory() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        String original = ((Blob) model.getPropertyValue("file:content")).getString();
        respond(true);

        convert(session, model, null);

        List<DocumentModel> versions = session.getVersions(model.getRef());
        assertEquals(2, versions.size());
        Blob before = (Blob) versions.get(0).getPropertyValue("file:content");
        assertEquals("legacy.doc", before.getFilename());
        assertEquals(original, before.getString());
        Blob after = (Blob) versions.get(1).getPropertyValue("file:content");
        assertEquals("legacy.docx", after.getFilename());
        assertEquals(CONVERTED, after.getString());
    }

    @Test
    public void userWithWritePermissionKeepsTheOriginalInTheHistory() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        TestHelper.grant(session, model, USER, SecurityConstants.READ_WRITE);
        respond(true);

        convert(coreFeature.getCoreSession(USER), model, null);

        List<DocumentModel> versions = session.getVersions(model.getRef());
        assertEquals(2, versions.size());
        assertEquals("legacy.doc", ((Blob) versions.get(0).getPropertyValue("file:content")).getFilename());
    }

    @Test
    public void conversionDoesNotVersionTheOriginalTwice() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        session.checkIn(model.getRef(), VersioningOption.MINOR, null);
        session.save();
        respond(true);

        convert(session, model, null);

        assertEquals("the original was already a version", 2, session.getVersions(model.getRef()).size());
    }

    @Test
    public void conversionOfDocumentOpenInOnlyofficeIsRejected() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        lockService.lock(session, model);
        session.save();
        respond(true);

        assertRejected(model, SC_CONFLICT);
        assertEquals("the editing session would lose its changes", "legacy.doc",
                ((Blob) session.getDocument(model.getRef()).getPropertyValue("file:content")).getFilename());
    }

    @Test
    public void conversionOfVersionIsRejected() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        DocumentRef version = session.checkIn(model.getRef(), VersioningOption.MINOR, null);
        session.save();
        respond(true);

        assertRejected(session.getDocument(version), SC_CONFLICT);
    }

    @Test
    public void conversionOfTrashedDocumentIsRejected() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        Framework.getService(TrashService.class).trashDocument(model);
        session.save();
        respond(true);

        assertRejected(session.getDocument(model.getRef()), SC_CONFLICT);
    }

    @Test
    public void unsupportedFormatIsRejected() throws Exception {
        assertTrue(assertBadRequest(createDoc("archive.xyz")).contains("archive.xyz"));
        assertBadRequest(TestHelper.createFile(session, "editable", "report.docx", DOCX_MIME_TYPE, "x"));
        assertBadRequest(TestHelper.createFile(session, "no-extension", "legacy", "application/msword", "x"));
    }

    @Test
    public void contentWithoutFileNameIsRejected() throws Exception {
        DocumentModel model = session.createDocumentModel("/", "unnamed", "File");
        model.setPropertyValue("file:content", (Serializable) Blobs.createBlob("legacy", "application/msword"));
        model = session.createDocument(model);
        session.save();
        assertNull(((Blob) model.getPropertyValue("file:content")).getFilename());

        assertBadRequest(model);
    }

    @Test
    public void documentWithoutContentIsRejected() throws Exception {
        assertBadRequest(TestHelper.createFile(session, "empty", null, null, null));
    }

    @Test
    public void documentThatCannotHoldContentIsRejected() throws Exception {
        DocumentModel folder = session.createDocument(session.createDocumentModel("/", "folder", "Folder"));
        session.save();

        assertBadRequest(folder);
    }

    @Test(expected = DocumentSecurityException.class)
    public void userWithoutWritePermissionCannotConvert() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        TestHelper.grant(session, model, USER, SecurityConstants.READ);

        convert(coreFeature.getCoreSession(USER), model, null);
    }
}
