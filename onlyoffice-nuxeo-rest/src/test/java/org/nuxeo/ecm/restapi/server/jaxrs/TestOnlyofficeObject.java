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

package org.nuxeo.ecm.restapi.server.jaxrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.settings.SettingsConstants;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.RequestContext;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.onlyoffice.sdk.service.settings.SettingsValidationService;
import org.onlyoffice.service.LinkTokenService;
import org.onlyoffice.service.LinkTokenService.Operation;

@RunWith(FeaturesRunner.class)
@Features({AutomationFeature.class, MockitoFeature.class})
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.login")
@Deploy("org.nuxeo.ecm.platform.login.token")
@Deploy("org.onlyoffice.onlyoffice-nuxeo-core")
public class TestOnlyofficeObject {
    private static final String USER = "jdoe";
    private static final String SECRET = "secret";
    private static final String KEY_PROPERTY = "onlyoffice." + SettingsConstants.SECURITY_KEY;
    private static final String URL_PROPERTY = "onlyoffice." + SettingsConstants.URL;
    private static final String EXTRA_PROPERTY = "onlyoffice.extra";
    private static final int SC_CONFLICT = 409;
    private static final long OTHER_REQUEST_TIMEOUT_MS = 10000;

    @Mock
    @RuntimeService
    private DownloadService downloadService;

    @Inject
    private CoreSession session;

    @Inject
    private CoreFeature coreFeature;

    @Inject
    private SettingsManager settingsManager;

    @Inject
    private JwtManager jwtManager;

    @Inject
    private LinkTokenService linkTokenService;

    @Inject
    private UserManager userManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> originalProperties = new HashMap<>();
    private HttpHeaders httpHeaders;
    private RequestContext requestContext;

    @Before
    public void setUp() throws IOException {
        for (String name : new String[] {KEY_PROPERTY, URL_PROPERTY, EXTRA_PROPERTY}) {
            originalProperties.put(name, Framework.getProperties().get(name));
            Framework.getProperties().remove(name);
        }
        Files.deleteIfExists(settingsFile().toPath());
    }

    @After
    public void tearDown() throws IOException {
        if (requestContext != null) {
            requestContext.dispose();
        }
        Properties properties = Framework.getProperties();
        for (Map.Entry<String, Object> original : originalProperties.entrySet()) {
            if (original.getValue() == null) {
                properties.remove(original.getKey());
            } else {
                properties.put(original.getKey(), original.getValue());
            }
        }
        Files.deleteIfExists(settingsFile().toPath());
    }

    private static java.io.File settingsFile() {
        return new java.io.File(Environment.getDefault().getConfig(), "onlyoffice.properties");
    }

    /**
     * Builds the endpoint as WebEngine would for a request made by the owner of {@code userSession}.
     */
    private OnlyofficeObject endpoint(final CoreSession userSession) {
        if (requestContext != null) {
            requestContext.dispose();
        }

        HttpServletRequest request = mock(HttpServletRequest.class);
        WebContext ctx = mock(WebContext.class);
        httpHeaders = mock(HttpHeaders.class);

        when(request.getAttribute(WebContext.class.getName())).thenReturn(ctx);
        when(request.getContextPath()).thenReturn("/nuxeo");
        when(ctx.getRequest()).thenReturn(request);
        when(ctx.getCoreSession()).thenReturn(userSession);
        when(ctx.getPrincipal()).thenReturn(userSession.getPrincipal());
        when(ctx.getServerURL()).thenAnswer(invocation -> new StringBuilder("http://nuxeo.example.com"));
        when(ctx.getHttpHeaders()).thenReturn(httpHeaders);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getMatchedURIs()).thenReturn(Collections.singletonList("api/v1/onlyoffice"));
        when(ctx.getUriInfo()).thenReturn(uriInfo);

        requestContext = new RequestContext(request, mock(HttpServletResponse.class));

        OnlyofficeObject endpoint = new OnlyofficeObject();
        ResourceType type = mock(ResourceType.class);
        doReturn(OnlyofficeObject.class).when(type).getResourceClass();
        when(type.getGuard()).thenReturn(Guard.DEFAULT);
        endpoint.initialize(ctx, type);
        return endpoint;
    }

    private void header(final String name, final String value) {
        when(httpHeaders.getRequestHeader(name)).thenReturn(Collections.singletonList(value));
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private DocumentModel createFile(final String fileName) {
        DocumentModel model = session.createDocumentModel("/", "doc", "File");
        if (fileName != null) {
            Blob blob = Blobs.createBlob("content " + UUID.randomUUID(), "application/octet-stream",
                    StandardCharsets.UTF_8.name(), fileName);
            model.setPropertyValue("file:content", (Serializable) blob);
        }
        model = session.createDocument(model);
        session.save();
        return model;
    }

    private void grant(final DocumentModel model, final String permission) {
        ACP acp = model.getACP();
        acp.getOrCreateACL(ACL.LOCAL_ACL).add(new ACE(USER, permission, true));
        session.setACP(model.getRef(), acp, true);
        session.save();
    }

    private JsonNode json(final Response response) throws IOException {
        return objectMapper.readTree((String) response.getEntity());
    }

    private static InputStream body(final String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    private String link(final Operation operation, final String documentId, final String userName,
                        final String documentKey) {
        return linkTokenService.createToken(operation, documentId, userName, documentKey);
    }

    /** The link token Document Server gets in the callback url of an editing session with the key "key". */
    private String callbackLink(final DocumentModel model) {
        return link(Operation.CALLBACK, model.getId(), session.getPrincipal().getName(), "key");
    }

    private String downloadLink(final String documentId) {
        return link(Operation.DOWNLOAD, documentId, session.getPrincipal().getName(), null);
    }

    private String testFileLink() {
        return link(Operation.TEST_FILE, null, session.getPrincipal().getName(), null);
    }

    private String downloadToken() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("url", "http://nuxeo.example.com/download");
        return jwtManager.createToken(payload, SECRET);
    }

    // ------------------------------------------------------------------ settings

    @Test
    public void getSettingsReturnsCurrentSettings() throws Exception {
        Framework.getProperties().put(URL_PROPERTY, "http://docs.example.com/");
        Framework.getProperties().put(KEY_PROPERTY, SECRET);

        JsonNode settings = json((Response) endpoint(session).getSettings());

        assertEquals("http://docs.example.com/", settings.get("url").asText());
        assertEquals(SECRET, settings.get("security").get("key").asText());
        assertTrue(settings.get("extra").has("pathApiUrl"));
    }

    @Test(expected = WebSecurityException.class)
    public void getSettingsRequiresAdministrator() throws Exception {
        endpoint(coreFeature.getCoreSession(USER)).getSettings();
    }

    @Test
    public void postSettingsSavesAndValidates() throws Exception {
        OnlyofficeObject endpoint = endpoint(session);
        SettingsValidationService validation = mock(SettingsValidationService.class);
        when(validation.validateSettings()).thenReturn(Collections.emptyMap());
        setField(endpoint, "settingsValidationService", validation);

        Response response = (Response) endpoint.setSettings(
                body("{\"url\":\"http://docs.example.com/\",\"security\":{\"key\":\"secret\"}}"));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(json(response).has("validationResults"));
        assertEquals("http://docs.example.com/", settingsManager.getSetting(SettingsConstants.URL));
        assertEquals(SECRET, settingsManager.getSetting(SettingsConstants.SECURITY_KEY));
        verify(validation).validateSettings();
    }

    @Test
    public void postSettingsDoesNotPersistExtra() throws Exception {
        OnlyofficeObject endpoint = endpoint(session);
        SettingsValidationService validation = mock(SettingsValidationService.class);
        when(validation.validateSettings()).thenReturn(Collections.emptyMap());
        setField(endpoint, "settingsValidationService", validation);

        // the settings page sends back the extra data it got with the settings
        endpoint.setSettings(body("{\"url\":\"http://docs.example.com/\","
                + "\"extra\":{\"pathApiUrl\":\"/web-apps/apps/api/documents/api.js\"},\"unknownField\":\"value\"}"));

        Properties persisted = new Properties();
        try (Reader reader = Files.newBufferedReader(settingsFile().toPath())) {
            persisted.load(reader);
        }
        assertEquals("http://docs.example.com/", persisted.getProperty(URL_PROPERTY));
        assertFalse(persisted.containsKey(EXTRA_PROPERTY));
        assertNull(Framework.getProperty(EXTRA_PROPERTY));
    }

    @Test(expected = WebSecurityException.class)
    public void postSettingsRequiresAdministrator() throws Exception {
        endpoint(coreFeature.getCoreSession(USER)).setSettings(body("{\"url\":\"http://evil/\"}"));
    }

    // ------------------------------------------------------------------ formats & filter

    @Test
    public void formatsContainOfficeFormats() throws Exception {
        JsonNode formats = objectMapper.readTree((String) ((Response) endpoint(session).getFormats()).getEntity());

        assertTrue(formats.isArray());
        boolean hasDocx = false;
        for (JsonNode format : formats) {
            hasDocx |= "docx".equals(format.get("name").asText());
        }
        assertTrue(hasDocx);
    }

    @Test
    public void filterReturnsEditForEditableFormat() throws Exception {
        DocumentModel model = createFile("report.docx");

        assertEquals("edit", json((Response) endpoint(session).getFilter(model.getId())).get("mode").asText());
    }

    @Test
    public void filterReturnsViewWithoutWritePermission() throws Exception {
        DocumentModel model = createFile("report.docx");
        grant(model, SecurityConstants.READ);

        JsonNode filter = json((Response) endpoint(coreFeature.getCoreSession(USER)).getFilter(model.getId()));

        assertEquals("view", filter.get("mode").asText());
    }

    @Test
    public void filterReturnsViewForDocumentLockedByAnotherUser() throws Exception {
        DocumentModel model = createFile("report.docx");
        grant(model, SecurityConstants.READ_WRITE);
        session.setLock(model.getRef());

        JsonNode filter = json((Response) endpoint(coreFeature.getCoreSession(USER)).getFilter(model.getId()));

        assertEquals("view", filter.get("mode").asText());
    }

    @Test
    public void filterReturnsViewForViewOnlyFormat() throws Exception {
        DocumentModel model = createFile("scan.djvu");

        assertEquals("view", json((Response) endpoint(session).getFilter(model.getId())).get("mode").asText());
    }

    @Test
    public void filterReturnsNoModeForUnsupportedFormat() throws Exception {
        DocumentModel model = createFile("archive.xyz");

        assertFalse(json((Response) endpoint(session).getFilter(model.getId())).has("mode"));
    }

    /**
     * The button label must match what the editor actually opens: PDF forms are editable (fillable) in the editor.
     */
    @Test
    public void filterIsConsistentWithEditorForFillableForms() throws Exception {
        DocumentModel model = createFile("form.pdf");

        assertEquals("edit", json((Response) endpoint(session).getFilter(model.getId())).get("mode").asText());
    }

    /**
     * BUG: documents without content (e.g. a File with no attachment) make the filter fail with a 500 error.
     */
    @Test
    public void filterReturnsNoModeForDocumentWithoutContent() throws Exception {
        DocumentModel model = createFile(null);

        assertFalse(json((Response) endpoint(session).getFilter(model.getId())).has("mode"));
    }

    // ------------------------------------------------------------------ callback

    @Test
    public void callbackSucceeds() throws Exception {
        DocumentModel model = createFile("report.docx");

        Response response = (Response) endpoint(session).postCallback(model.getId(), callbackLink(model),
                body("{\"status\":1,\"key\":\"key\"}"));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(0, objectMapper.readTree((String) response.getEntity()).get("error").asInt());
        assertTrue((session.getLockInfo(model.getRef()) != null));
    }

    @Test
    public void callbackWithoutTokenIsUnauthorizedWhenSecurityEnabled() throws Exception {
        Framework.getProperties().put(KEY_PROPERTY, SECRET);
        DocumentModel model = createFile("report.docx");

        Response response = (Response) endpoint(session).postCallback(model.getId(), callbackLink(model),
                body("{\"status\":1,\"key\":\"key\"}"));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(1, objectMapper.readTree((String) response.getEntity()).get("error").asInt());
        assertFalse((session.getLockInfo(model.getRef()) != null));
    }

    @Test
    public void callbackWithTokenInHeaderIsAccepted() throws Exception {
        Framework.getProperties().put(KEY_PROPERTY, SECRET);
        DocumentModel model = createFile("report.docx");

        Map<String, Object> callback = new HashMap<>();
        callback.put("status", 1);
        callback.put("key", "key");
        String token = jwtManager.createToken(Collections.singletonMap("payload", callback), SECRET);

        OnlyofficeObject endpoint = endpoint(session);
        header(settingsManager.getSecurityHeader(), settingsManager.getSecurityPrefix() + token);
        Response response = (Response) endpoint.postCallback(model.getId(), callbackLink(model),
                body("{\"status\":4}"));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue("status from the signed payload is used", (session.getLockInfo(model.getRef()) != null));
    }

    @Test
    public void permanentNuxeoErrorKeepsItsStatus() throws Exception {
        DocumentModel model = createFile(null);

        Response response = (Response) endpoint(session).postCallback(model.getId(), callbackLink(model),
                body("{\"status\":2,\"key\":\"key\",\"url\":\"http://docs.example.com/file\"}"));

        assertEquals(SC_CONFLICT, response.getStatus());
        assertEquals(1, objectMapper.readTree((String) response.getEntity()).get("error").asInt());
    }

    @Test
    public void unexpectedErrorIsInternalServerError() throws Exception {
        DocumentModel model = createFile("report.docx");
        OnlyofficeObject endpoint = endpoint(session);
        CallbackService failing = mock(CallbackService.class);
        when(failing.verifyCallback(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("boom")).when(failing).processCallback(any(), any());
        setField(endpoint, "callbackService", failing);

        Response response = (Response) endpoint.postCallback(model.getId(), callbackLink(model),
                body("{\"status\":2,\"key\":\"key\",\"url\":\"http://docs.example.com/file\"}"));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(1, json(response).get("error").asInt());
        assertFalse("the exception text is not sent back", json(response).has("message"));
    }

    private void assertBadRequest(final OnlyofficeObject endpoint, final DocumentModel model, final String body)
            throws IOException {
        Response response = (Response) endpoint.postCallback(model.getId(), callbackLink(model), body(body));

        assertEquals(body, Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(body, 1, json(response).get("error").asInt());
        assertFalse(body, json(response).has("message"));
        assertNull(body, session.getLockInfo(model.getRef()));
    }

    @Test
    public void callbackThatIsNotJsonObjectIsBadRequest() throws Exception {
        DocumentModel model = createFile("report.docx");

        for (String body : new String[] {"42", "[]", "\"x\"", "", "not json"}) {
            assertBadRequest(endpoint(session), model, body);
        }
    }

    @Test
    public void callbackWithoutKnownStatusIsBadRequest() throws Exception {
        DocumentModel model = createFile("report.docx");

        for (String body : new String[] {"{\"key\":\"key\"}", "{\"status\":42,\"key\":\"key\"}",
                "{\"status\":\"x\",\"key\":\"key\"}", "{\"status\":{},\"key\":\"key\"}"}) {
            assertBadRequest(endpoint(session), model, body);
        }
    }

    @Test
    public void callbackToSaveWithoutUrlIsBadRequest() throws Exception {
        DocumentModel model = createFile("report.docx");

        assertBadRequest(endpoint(session), model, "{\"status\":2,\"key\":\"key\"}");
        assertBadRequest(endpoint(session), model, "{\"status\":3,\"key\":\"key\",\"url\":\"\"}");
    }

    @Test
    public void callbackWithHeaderTokenWithoutPayloadIsBadRequest() throws Exception {
        Framework.getProperties().put(KEY_PROPERTY, SECRET);
        DocumentModel model = createFile("report.docx");

        Map<String, Object> callback = new HashMap<>();
        callback.put("status", 1);
        callback.put("key", "key");

        OnlyofficeObject endpoint = endpoint(session);
        header(settingsManager.getSecurityHeader(),
                settingsManager.getSecurityPrefix() + jwtManager.createToken(callback, SECRET));
        assertBadRequest(endpoint, model, "{\"status\":1}");

        endpoint = endpoint(session);
        header(settingsManager.getSecurityHeader(), settingsManager.getSecurityPrefix()
                + jwtManager.createToken(Collections.singletonMap("payload", "not a callback"), SECRET));
        assertBadRequest(endpoint, model, "{\"status\":1}");
    }

    private void assertUnauthorized(final DocumentModel model, final String linkToken) throws IOException {
        Response response = (Response) endpoint(session).postCallback(model.getId(), linkToken,
                body("{\"status\":1,\"key\":\"key\"}"));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(1, json(response).get("error").asInt());
        assertNull(session.getLockInfo(model.getRef()));
    }

    @Test
    public void callbackRequiresItsLinkToken() throws Exception {
        DocumentModel model = createFile("report.docx");
        String user = session.getPrincipal().getName();

        assertUnauthorized(model, null);
        assertUnauthorized(model, downloadLink(model.getId()));
        assertUnauthorized(model, testFileLink());
        assertUnauthorized(model, link(Operation.CALLBACK, createFile("other.docx").getId(), user, "key"));
        assertUnauthorized(model, link(Operation.CALLBACK, model.getId(), "nobody", "key"));
    }

    @Test
    public void callbackOfAnotherEditingSessionIsUnauthorized() throws Exception {
        DocumentModel model = createFile("report.docx");

        assertUnauthorized(model, link(Operation.CALLBACK, model.getId(), session.getPrincipal().getName(), "old"));
    }

    private void createUser() {
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", USER);
        userManager.createUser(user);
    }

    @Test
    public void callbackIsProcessedAsTheUserTheLinkWasIssuedTo() throws Exception {
        createUser();
        DocumentModel model = createFile("report.docx");
        grant(model, SecurityConstants.READ_WRITE);

        Response response = (Response) endpoint(session).postCallback(model.getId(),
                link(Operation.CALLBACK, model.getId(), USER, "key"), body("{\"status\":1,\"key\":\"key\"}"));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(USER, session.getLockInfo(model.getRef()).getOwner());
    }

    // ------------------------------------------------------------------ download

    /**
     * BUG: Document Server's requests have no principal, and the user directory is not readable without one, so the
     * user of the link was found only while in the principal cache, e.g. not after a restart.
     */
    @Test
    public void linkUserIsFoundWhileNobodyIsLoggedIn() throws Exception {
        createUser();
        DocumentModel model = createFile("report.docx");
        grant(model, SecurityConstants.READ);
        String linkToken = link(Operation.DOWNLOAD, model.getId(), USER, null);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        AtomicReference<Object> status = new AtomicReference<>();
        Thread documentServerRequest = new Thread(() -> {
            try {
                status.set(((Response) endpoint(session).getDownload(model.getId(), linkToken,
                        mock(HttpServletRequest.class), mock(HttpServletResponse.class))).getStatus());
            } catch (Exception e) {
                status.set(e);
            }
        });
        documentServerRequest.start();
        documentServerRequest.join(OTHER_REQUEST_TIMEOUT_MS);

        assertEquals(Response.Status.OK.getStatusCode(), status.get());
    }

    @Test
    public void downloadRequiresItsLinkToken() throws Exception {
        DocumentModel model = createFile("report.docx");

        String otherDocument = downloadLink(createFile("other.docx").getId());
        String unknownUser = link(Operation.DOWNLOAD, model.getId(), "nobody", null);

        for (String linkToken : new String[] {null, callbackLink(model), testFileLink(), otherDocument, unknownUser}) {
            Response response = (Response) endpoint(session).getDownload(model.getId(), linkToken,
                    mock(HttpServletRequest.class), mock(HttpServletResponse.class));

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        }
        verify(downloadService, never()).downloadBlob(any(DownloadService.DownloadContext.class));
    }

    @Test
    public void downloadStreamsDocumentContent() throws Exception {
        DocumentModel model = createFile("report.docx");

        Response response = (Response) endpoint(session).getDownload(model.getId(), downloadLink(model.getId()),
                mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(downloadService).downloadBlob(any(DownloadService.DownloadContext.class));
    }

    @Test
    public void downloadOfMissingDocumentIsNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        Response response = (Response) endpoint(session).getDownload(id, downloadLink(id),
                mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
    }

    @Test
    public void downloadRequiresJwtWhenSecurityEnabled() throws Exception {
        Framework.getProperties().put(KEY_PROPERTY, SECRET);
        DocumentModel model = createFile("report.docx");

        try {
            endpoint(session).getDownload(model.getId(), downloadLink(model.getId()), mock(HttpServletRequest.class),
                    mock(HttpServletResponse.class));
            fail("Download without JWT must be rejected");
        } catch (IOException e) {
            verify(downloadService, never()).downloadBlob(any(DownloadService.DownloadContext.class));
        }
    }

    @Test
    public void downloadAcceptsValidJwt() throws Exception {
        Framework.getProperties().put(KEY_PROPERTY, SECRET);
        DocumentModel model = createFile("report.docx");

        OnlyofficeObject endpoint = endpoint(session);
        header(settingsManager.getSecurityHeader(), settingsManager.getSecurityPrefix() + downloadToken());
        Response response = (Response) endpoint.getDownload(model.getId(), downloadLink(model.getId()),
                mock(HttpServletRequest.class),
                mock(HttpServletResponse.class));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(downloadService).downloadBlob(any(DownloadService.DownloadContext.class));
    }

    @Test
    public void downloadRejectsJwtSignedWithAnotherKey() throws Exception {
        Framework.getProperties().put(KEY_PROPERTY, "another-secret");
        DocumentModel model = createFile("report.docx");

        OnlyofficeObject endpoint = endpoint(session);
        header(settingsManager.getSecurityHeader(), settingsManager.getSecurityPrefix() + downloadToken());
        try {
            endpoint.getDownload(model.getId(), downloadLink(model.getId()), mock(HttpServletRequest.class),
                    mock(HttpServletResponse.class));
            fail("Download with invalid JWT must be rejected");
        } catch (IOException e) {
            verify(downloadService, never()).downloadBlob(any(DownloadService.DownloadContext.class));
        }
    }

    // ------------------------------------------------------------------ test-txt

    @Test
    public void testFileIsServed() throws Exception {
        Response response = (Response) endpoint(session).getTestFile(testFileLink());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Test file for conversion", response.getEntity());
    }

    @Test
    public void testFileRequiresJwtWhenSecurityEnabled() throws Exception {
        Framework.getProperties().put(KEY_PROPERTY, SECRET);

        Response response = (Response) endpoint(session).getTestFile(testFileLink());

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testFileAcceptsValidJwt() throws Exception {
        Framework.getProperties().put(KEY_PROPERTY, SECRET);

        OnlyofficeObject endpoint = endpoint(session);
        header(settingsManager.getSecurityHeader(), settingsManager.getSecurityPrefix() + downloadToken());

        assertEquals(Response.Status.OK.getStatusCode(), ((Response) endpoint.getTestFile(testFileLink())).getStatus());
    }

    @Test
    public void testFileRequiresItsLinkToken() throws Exception {
        String download = downloadLink(createFile("report.docx").getId());

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                ((Response) endpoint(session).getTestFile(null)).getStatus());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                ((Response) endpoint(session).getTestFile(download)).getStatus());
    }
}
