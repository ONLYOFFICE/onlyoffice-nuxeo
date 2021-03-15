/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

package org.onlyoffice.web;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.utils.ConfigManager;
import org.onlyoffice.utils.JwtManager;
import org.onlyoffice.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/onlyedit")
@WebObject(type = "onlyedit")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
public class Editor extends ModuleRoot {

    private static final Logger logger = LoggerFactory.getLogger(Editor.class);

    private JwtManager jwtManager;
    private ConfigManager config;
    private Utils utils;

    private TokenAuthenticationService authService;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);

        jwtManager = Framework.getService(JwtManager.class);
        config = Framework.getService(ConfigManager.class);
        utils = Framework.getService(Utils.class);
        authService = Framework.getService(TokenAuthenticationService.class);
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public Object getEdit(@PathParam("id") String id, @QueryParam("mode") String mode) {
        WebContext ctx = getContext();
        CoreSession session = ctx.getCoreSession();
        DocumentModel model = session.getDocument(new IdRef(id));

        try {
            return getView("index")
                .arg("config", getConfig(ctx, model, mode))
                .arg("docUrl", config.getDocServUrl())
                .arg("docTitle", model.getTitle());
        } catch (Exception e) {
            logger.error("Error while opening editor for " + id, e);
            return Response.serverError().build();
        }
    }

    private JSONObject getConfig(WebContext ctx, DocumentModel model, String mode) throws Exception {
        String user = ctx.getPrincipal().getName();
        String token = authService.acquireToken(user, "ONLYOFFICE", "editor", "auth", "rw");
        String baseUrl = ctx.getBaseURL();
        String repoName = ctx.getCoreSession().getRepositoryName();
        String locale = ctx.getLocale().toLanguageTag();

        JSONObject responseJson = new JSONObject();
        JSONObject documentObject = new JSONObject();
        JSONObject editorConfigObject = new JSONObject();
        JSONObject userObject = new JSONObject();
        JSONObject permObject = new JSONObject();

        String docTitle = model.getTitle();
        String docFilename = model.getAdapter(BlobHolder.class).getBlob().getFilename();
        String docExt = utils.getFileExtension(docFilename);
        String docId = model.getId();

        String contentUrl = String.format("%1s/nuxeo/nxfile/%2s/%3s/file:content/%4s?token=%5s", baseUrl, repoName, docId, docFilename, token);
        String callbackUrl = String.format("%1s/nuxeo/api/v1/onlyoffice/callback/%2s?token=%3s", baseUrl, docId, token);

        Boolean toEdit = mode != null && mode.equals("edit");

        responseJson.put("type", "desktop");
        responseJson.put("width", "100%");
        responseJson.put("height", "100%");
        responseJson.put("documentType", utils.getDocumentType(docExt));

        responseJson.put("document", documentObject);
        documentObject.put("title", docTitle);
        documentObject.put("url", contentUrl);
        documentObject.put("fileType", docExt);
        documentObject.put("key", utils.getDocumentKey(model));
        documentObject.put("permissions", permObject);
        permObject.put("edit", toEdit);

        responseJson.put("editorConfig", editorConfigObject);
        editorConfigObject.put("lang", locale);
        editorConfigObject.put("mode", toEdit ? "edit" : "view");
        editorConfigObject.put("callbackUrl", callbackUrl);
        editorConfigObject.put("user", userObject);
        userObject.put("id", user);
        userObject.put("name", user);

        if (jwtManager.isEnabled()) {
            responseJson.put("token", jwtManager.createToken(responseJson));
        }

        return responseJson;
    }
}
