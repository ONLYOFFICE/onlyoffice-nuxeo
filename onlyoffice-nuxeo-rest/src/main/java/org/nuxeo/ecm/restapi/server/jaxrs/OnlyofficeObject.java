/**
 *
 * (c) Copyright Ascensio System SIA 2023
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
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;

import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.common.Format;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.settings.Settings;
import com.onlyoffice.model.settings.SettingsConstants;
import com.onlyoffice.model.settings.security.Security;
import com.onlyoffice.model.settings.validation.ValidationResult;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.onlyoffice.sdk.service.settings.SettingsValidationService;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);

        permissionService = Framework.getService(PermissionService.class);
        settingsValidationService = Framework.getService(SettingsValidationService.class);
        callbackService = Framework.getService(CallbackService.class);
        settingsManager = Framework.getService(SettingsManager.class);
        jwtManager = Framework.getService(JwtManager.class);
        documentManager = Framework.getService(DocumentManager.class);
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

        Map<String, Object> extraSettings = new HashMap<String, Object>(){{
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
    public Object setSettings(InputStream input) throws IOException {
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
    public Object getFilter(@PathParam("id") String id) {
        JSONObject response = new JSONObject();

        CoreSession session = getContext().getCoreSession();
        DocumentModel model = session.getDocument(new IdRef(id));

        String fileName = model.getAdapter(BlobHolder.class).getBlob().getFilename();

        if (documentManager.isViewable(fileName)) {
            response.put("mode", "view");
        }

        Boolean hasWriteProperties = permissionService.checkPermission(model, session.getPrincipal(), SecurityConstants.WRITE_PROPERTIES);

        if (documentManager.isEditable(fileName) && hasWriteProperties) {
            response.put("mode", "edit");
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
    public Object postCallback(@PathParam("id") String id, InputStream input) throws IOException {
        Status code = Status.OK;
        Exception error = null;

        Callback callback = objectMapper.readValue(input, Callback.class);

        try {
            String securityHeader = settingsManager.getSecurityHeader();
            List<String> values = getContext().getHttpHeaders().getRequestHeader(securityHeader);
            String authorizationHeader = values == null || values.isEmpty() ? null : values.get(0);

            callback = callbackService.verifyCallback(callback, authorizationHeader);

            callbackService.processCallback(callback, id);
        } catch (SecurityException ex) {
            code = Status.UNAUTHORIZED;
            error = ex;
            logger.error("Security error while saving document " + id, ex);
        } catch (Exception ex) {
            code = Status.INTERNAL_SERVER_ERROR;
            error = ex;
            logger.error("Error while saving document " + id, ex);
        }

        HashMap<String, Object> response = new HashMap<String, Object>();
        if (error != null) {
            response.put("error", 1);
            response.put("message", error.getMessage());
        } else {
            response.put("error", 0);
        }

        try {
            return Response.status(code).entity(new JSONObject(response).toString(2)).build();
        } catch (Exception e) {
            logger.error("Error while processing callback for " + id, e);
            return Response.status(code).build();
        }
    }

    @GET
    @Path("download/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getDownload(@PathParam("id") String id, @Context HttpServletRequest request,
                              @Context HttpServletResponse response) throws IOException {
        boolean isTransactionActive = false;

        try {
            if (!TransactionHelper.isTransactionActive()) {
                isTransactionActive = TransactionHelper.startTransaction();
            }

            if (settingsManager.isSecurityEnabled()) {
                String jwtHeader = settingsManager.getSecurityHeader();
                List<String> values = getContext().getHttpHeaders().getRequestHeader(jwtHeader);
                String header = values.isEmpty() ? null : values.get(0);
                String authorizationPrefix = settingsManager.getSecurityPrefix();
                String token = (header != null && header.startsWith(authorizationPrefix))
                        ? header.substring(authorizationPrefix.length()) : header;

                if (token == null || token == "") {
                    throw new DocumentSecurityException("Expected JWT");
                }

                try {
                    String payload = jwtManager.verify(token);
                } catch (Exception e) {
                    throw new DocumentSecurityException("JWT verification failed");
                }
            }

            IdRef docRef = new IdRef(id);
            CoreSession session = getContext().getCoreSession();

            if (!session.exists(docRef)) {
                NuxeoPrincipal principal = NuxeoPrincipal.getCurrent();
                if (principal != null && principal.isAnonymous()) {
                    throw new DocumentSecurityException("Authentication is needed for downloading the blob");
                }

                return Response.status(HttpServletResponse.SC_NOT_FOUND).build();
            }

            DocumentModel model = session.getDocument(docRef);
            Blob blob = getBlob(model, "file:content");

            DownloadService.DownloadContext context = DownloadService.DownloadContext.builder(request, response)
                    .doc(model)
                    .xpath("file:content")
                    .filename(blob.getFilename())
                    .reason("download")
                    .build();

            DownloadService downloadService = Framework.getService(DownloadService.class);
            downloadService.downloadBlob(context);

            return Response.ok().build();
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
    public Object getTestFile() throws UnsupportedEncodingException {
        checkAdministrator();

        if (settingsManager.isSecurityEnabled()) {
            String jwtHeader = settingsManager.getSecurityHeader();
            List<String> values = getContext().getHttpHeaders().getRequestHeader(jwtHeader);
            String header = values.isEmpty() ? null : values.get(0);
            String authorizationPrefix = settingsManager.getSecurityPrefix();
            String token = (header != null && header.startsWith(authorizationPrefix))
                    ? header.substring(authorizationPrefix.length()) : header;

            if (token == null || token == "") {
                return Response.status(Status.UNAUTHORIZED).build();
            }

            try {
                String payload = jwtManager.verify(token);
            } catch (Exception e) {
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

    private void checkAdministrator() {
        NuxeoPrincipal currentUser = getContext().getCoreSession().getPrincipal();
        if (!currentUser.isAdministrator()) {
            throw new WebSecurityException("You don't have the permission to ONLYOFFICE settings!");
        }
    }

    private Blob getBlob(DocumentModel model, String xpath) {
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
