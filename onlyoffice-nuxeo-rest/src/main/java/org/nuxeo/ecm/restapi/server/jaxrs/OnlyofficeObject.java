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

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;

import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.common.Format;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.security.JwtOperation;
import com.onlyoffice.model.settings.Settings;
import com.onlyoffice.model.settings.SettingsConstants;
import com.onlyoffice.model.settings.security.Security;
import com.onlyoffice.model.settings.validation.ValidationResult;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.onlyoffice.sdk.service.settings.SettingsValidationService;
import org.onlyoffice.service.LinkTokenService;
import org.onlyoffice.service.LinkTokenService.LinkToken;
import org.onlyoffice.service.LinkTokenService.Operation;
import org.onlyoffice.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebObject(type = "onlyoffice")
public class OnlyofficeObject extends DefaultObject {

    private static final Logger logger = LoggerFactory.getLogger(OnlyofficeObject.class);

    private PermissionService permissionService;
    private SettingsValidationService settingsValidationService;
    private CallbackService callbackService;
    private SettingsManager settingsManager;
    private JwtManager jwtManager;
    private DocumentManager documentManager;
    private LinkTokenService linkTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void initialize(final Object... args) {
        super.initialize(args);

        permissionService = Framework.getService(PermissionService.class);
        settingsValidationService = Framework.getService(SettingsValidationService.class);
        callbackService = Framework.getService(CallbackService.class);
        settingsManager = Framework.getService(SettingsManager.class);
        jwtManager = Framework.getService(JwtManager.class);
        documentManager = Framework.getService(DocumentManager.class);
        linkTokenService = Framework.getService(LinkTokenService.class);
    }

    @GET
    @Path("settings")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getSettings() throws JsonProcessingException {
        checkAdministrator();

        String url = StringUtils.defaultIfEmpty(settingsManager.getSetting(SettingsConstants.URL), "");
        String securityKey = StringUtils.defaultIfEmpty(
                settingsManager.getSetting(SettingsConstants.SECURITY_KEY),
                ""
        );
        String securityHeader = StringUtils.defaultIfEmpty(
                settingsManager.getSetting(SettingsConstants.SECURITY_HEADER),
                ""
        );
        String innerUrl = StringUtils.defaultIfEmpty(
                settingsManager.getSetting(SettingsConstants.INNER_URL),
                ""
        );
        String productInnerUrl = StringUtils.defaultIfEmpty(
                settingsManager.getSetting(SettingsConstants.PRODUCT_INNER_URL),
                ""
        );

        Map<String, Object> extraSettings = new HashMap<String, Object>() {{
            put("pathApiUrl", settingsManager.getDocsIntegrationSdkProperties().getDocumentServer().getApiUrl());
        }};

        Settings settings = Settings.builder()
                .url(url)
                .innerUrl(innerUrl)
                .productInnerUrl(productInnerUrl)
                .security(Security.builder()
                        .key(securityKey)
                        .header(securityHeader)
                        .build())
                .extra(extraSettings)
                .build();


        return Response.status(Status.OK)
                    .entity(objectMapper.writeValueAsString(settings))
                    .type("application/json")
                    .build();

    }

    @POST
    @Path("settings")
    @Produces(MediaType.APPLICATION_JSON)
    public Object setSettings(final InputStream input) throws IOException {
        checkAdministrator();

        Settings settings = objectMapper.readValue(input, Settings.class);

        try {
            settingsManager.setSettings(settings);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        Map<String, ValidationResult> validationResults = settingsValidationService.validateSettings();

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("validationResults", validationResults);

        return Response.status(Status.OK)
                .entity(objectMapper.writeValueAsString(responseMap))
                .type("application/json")
                .build();
    }

    @GET
    @Path("formats")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getFormats() throws JsonProcessingException {
        List<Format> formats = documentManager.getFormats();
        ObjectMapper mapper = new ObjectMapper();

        return Response.status(Status.OK)
                .entity(mapper.writeValueAsString(formats))
                .type("application/json")
                .build();
    }

    @GET
    @Path("filter/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getFilter(final @PathParam("id") String id) {
        JSONObject response = new JSONObject();

        CoreSession session = getContext().getCoreSession();
        DocumentModel model = session.getDocument(new IdRef(id));

        BlobHolder blobHolder = model.getAdapter(BlobHolder.class);
        Blob blob = blobHolder != null ? blobHolder.getBlob() : null;
        String fileName = blob != null ? blob.getFilename() : null;

        // documents without content (e.g. a File without attachment) cannot be opened: no mode
        if (fileName != null) {
            if (documentManager.isViewable(fileName)) {
                response.put("mode", "view");
            }

            if (documentManager.isEditable(fileName) && permissionService.canEdit(model, session.getPrincipal())) {
                response.put("mode", "edit");
            }
        }

        return Response.status(Status.OK)
                .entity(response.toString(2))
                .type("application/json")
                .build();
    }

    @POST
    @Path("callback/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object postCallback(final @PathParam("id") String id,
                               final @QueryParam(LinkTokenService.PARAMETER) String linkToken,
                               final InputStream input) {
        Status code = Status.OK;

        try {
            LinkToken link = linkTokenService.verifyToken(linkToken, Operation.CALLBACK, id);

            Callback callback = objectMapper.readValue(input, Callback.class);

            String securityHeader = settingsManager.getSecurityHeader();
            List<String> values = getContext().getHttpHeaders().getRequestHeader(securityHeader);
            String authorizationHeader = values == null || values.isEmpty() ? null : values.get(0);

            callback = callbackService.verifyCallback(callback, authorizationHeader);
            validateCallback(callback);

            if (link.getDocumentKey() != null && !link.getDocumentKey().equals(callback.getKey())) {
                throw new SecurityException("Link token issued for another editing session");
            }

            try (NuxeoLoginContext ignored = loginAs(link.getUserName())) {
                callbackService.processCallback(callback, id);
            }
        } catch (SecurityException | JWTVerificationException ex) {
            code = Status.UNAUTHORIZED;
            logger.warn("Callback for document {} refused: {}", id, ex.toString());
        } catch (JsonProcessingException ex) {
            code = Status.BAD_REQUEST;
            logger.warn("Callback for document {} refused, not a valid callback: {}", id, ex.getOriginalMessage());
        } catch (NuxeoException ex) {
            // Permanent errors (not found, forbidden, lock conflict) carry a 4xx status and are not retried by
            // Document Server; anything else stays 5xx so that transient failures are retried.
            Status status = Status.fromStatusCode(ex.getStatusCode());
            code = status != null ? status : Status.INTERNAL_SERVER_ERROR;
            if (code.getFamily() == Status.Family.CLIENT_ERROR) {
                logger.warn("Callback for document {} refused: {}", id, ex.toString());
            } else {
                logger.error("Error while saving document " + id, ex);
            }
        } catch (Exception ex) {
            code = Status.INTERNAL_SERVER_ERROR;
            logger.error("Error while saving document " + id, ex);
        }

        // Document Server reads only "error"; the reason stays in the log, not in the answer to the caller
        HashMap<String, Object> response = new HashMap<String, Object>();
        response.put("error", code == Status.OK ? 0 : 1);

        try {
            return Response.status(code).entity(new JSONObject(response).toString(2)).build();
        } catch (Exception e) {
            logger.error("Error while processing callback for " + id, e);
            return Response.status(code).build();
        }
    }

    // A status the SDK does not know is deserialized as null, as is the callback from a header token without
    // "payload"
    private static void validateCallback(final Callback callback) {
        if (callback == null) {
            throw new NuxeoException("Callback is empty", HttpServletResponse.SC_BAD_REQUEST);
        }

        com.onlyoffice.model.documenteditor.callback.Status status = callback.getStatus();
        if (status == null) {
            throw new NuxeoException("Callback has no known status", HttpServletResponse.SC_BAD_REQUEST);
        }

        if ((status == com.onlyoffice.model.documenteditor.callback.Status.SAVE
                || status == com.onlyoffice.model.documenteditor.callback.Status.SAVE_CORRUPTED)
                && StringUtils.isBlank(callback.getUrl())) {
            throw new NuxeoException("Callback to save has no file url", HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @GET
    @Path("download/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getDownload(final @PathParam("id") String id,
                              final @QueryParam(LinkTokenService.PARAMETER) String linkToken,
                              final @Context HttpServletRequest request,
                              final @Context HttpServletResponse response) throws IOException {
        LinkToken link;
        try {
            link = linkTokenService.verifyToken(linkToken, Operation.DOWNLOAD, id);
        } catch (SecurityException e) {
            logger.warn("Download of document {} refused: {}", id, e.getMessage());
            return Response.status(Status.UNAUTHORIZED).build();
        }

        boolean isTransactionActive = false;

        try (NuxeoLoginContext ignored = loginAs(link.getUserName())) {
            if (!TransactionHelper.isTransactionActive()) {
                isTransactionActive = TransactionHelper.startTransaction();
            }

            if (settingsManager.isSecurityEnabled()) {
                String jwtHeader = settingsManager.getSecurityHeader();
                List<String> values = getContext().getHttpHeaders().getRequestHeader(jwtHeader);
                String header = values == null || values.isEmpty() ? null : values.get(0);
                String authorizationPrefix = settingsManager.getSecurityPrefix();
                String token = (header != null && header.startsWith(authorizationPrefix))
                        ? header.substring(authorizationPrefix.length()) : header;

                if (token == null || token.isEmpty()) {
                    throw new DocumentSecurityException("Expected JWT");
                }

                try {
                    jwtManager.verify(token, JwtOperation.DOWNLOAD);
                } catch (Exception e) {
                    logger.warn("JWT verification failed while downloading document {}: {}", id, e.toString());
                    throw new DocumentSecurityException("JWT verification failed");
                }
            }

            IdRef docRef = new IdRef(id);
            CoreSession session = CoreInstance.getCoreSession(null);

            if (!session.exists(docRef)) {
                return Response.status(HttpServletResponse.SC_NOT_FOUND).build();
            }

            DocumentModel model = session.getDocument(docRef);
            Blob blob = getBlob(model, "file:content");

            if (blob == null) {
                return Response.status(HttpServletResponse.SC_NOT_FOUND).build();
            }

            if (!isKnownFormat(blob.getFilename())) {
                return Response.status(Status.FORBIDDEN).build();
            }

            // The link is open and served from the Nuxeo origin: a browser following it must never render the file
            // (an html or svg file would run its scripts with the session of whoever opens the link), whatever
            // "inline" parameter is added to it
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Content-Security-Policy", "sandbox");

            DownloadService.DownloadContext context = DownloadService.DownloadContext.builder(request, response)
                    .doc(model)
                    .xpath("file:content")
                    .filename(blob.getFilename())
                    .reason("download")
                    .inline(false)
                    .build();

            DownloadService downloadService = Framework.getService(DownloadService.class);
            downloadService.downloadBlob(context);

            return Response.ok().build();
        } catch (SecurityException e) {
            logger.warn("Download of document {} refused: {}", id, e.getMessage());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (NuxeoException e) {
            if (isTransactionActive) {
                TransactionHelper.setTransactionRollbackOnly();
            }
            throw new IOException(e);
        } finally {
            if (isTransactionActive) {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    @GET
    @Path("test-txt")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getTestFile(final @QueryParam(LinkTokenService.PARAMETER) String linkToken)
            throws UnsupportedEncodingException {
        try {
            linkTokenService.verifyToken(linkToken, Operation.TEST_FILE, null);
        } catch (SecurityException e) {
            logger.warn("Download of the test file refused: {}", e.getMessage());
            return Response.status(Status.UNAUTHORIZED).build();
        }

        if (settingsManager.isSecurityEnabled()) {
            String jwtHeader = settingsManager.getSecurityHeader();
            List<String> values = getContext().getHttpHeaders().getRequestHeader(jwtHeader);
            String header = values == null || values.isEmpty() ? null : values.get(0);
            String authorizationPrefix = settingsManager.getSecurityPrefix();
            String token = (header != null && header.startsWith(authorizationPrefix))
                    ? header.substring(authorizationPrefix.length()) : header;

            if (token == null || token.isEmpty()) {
                return Response.status(Status.UNAUTHORIZED).build();
            }

            try {
                jwtManager.verify(token, JwtOperation.DOWNLOAD);
            } catch (Exception e) {
                logger.warn("JWT verification failed while downloading test file: {}", e.toString());
                return Response.status(Status.UNAUTHORIZED).build();
            }
        }

        String message = "Test file for conversion";

        return Response.status(Status.OK)
                .header("Content-Disposition", "attachment; filename=test.txt")
                .header("Content-Length", message.getBytes("UTF-8").length)
                .type("text/plain")
                .entity(message).build();
    }

    // The links handed to Document Server are not authenticated by Nuxeo (see link-token-service.xml): the work is
    // done as the user the link was issued to, with the permissions that user has now
    private static NuxeoLoginContext loginAs(final String userName) {
        // the request has no principal yet, and the user directory is readable only by an authenticated one
        NuxeoPrincipal principal = Framework.doPrivileged(
                () -> Framework.getService(UserManager.class).getPrincipal(userName));
        if (principal == null) {
            throw new SecurityException("Link token issued to an unknown user");
        }

        NuxeoLoginContext loginContext = NuxeoLoginContext.create(principal);
        loginContext.login();
        return loginContext;
    }

    private void checkAdministrator() {
        NuxeoPrincipal currentUser = getContext().getCoreSession().getPrincipal();
        if (!currentUser.isAdministrator()) {
            throw new WebSecurityException("You don't have the permission to ONLYOFFICE settings!");
        }
    }

    private boolean isKnownFormat(final String fileName) {
        String extension = documentManager.getExtension(fileName);
        if (extension == null) {
            return false;
        }

        for (Format format : documentManager.getFormats()) {
            if (extension.equals(format.getName())) {
                return true;
            }
        }
        return false;
    }

    private Blob getBlob(final DocumentModel model, final String xpath) {
        Blob blob = (Blob) model.getPropertyValue(xpath);
        if (blob == null) {
            BlobHolder bh = model.getAdapter(BlobHolder.class);
            if (bh != null) {
                blob = bh.getBlob();
            }
        }
        return blob;
    }
}
