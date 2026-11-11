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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.client.DocumentServerClient;
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
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

    @Mock
    @RuntimeService
    private ConvertService convertService;

    @Mock
    @RuntimeService
    private DocumentServerClient documentServerClient;

    @Inject
    private AutomationService automationService;

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

    @Test(expected = OperationException.class)
    public void unsupportedFormatIsRejected() throws Exception {
        convert(session, createDoc("archive.xyz"), null);
    }

    @Test(expected = DocumentSecurityException.class)
    public void userWithoutWritePermissionCannotConvert() throws Exception {
        DocumentModel model = createDoc("legacy.doc");
        TestHelper.grant(session, model, USER, SecurityConstants.READ);

        convert(coreFeature.getCoreSession(USER), model, null);
    }
}
