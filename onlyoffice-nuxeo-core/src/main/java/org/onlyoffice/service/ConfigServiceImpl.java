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

package org.onlyoffice.service;

import org.json.JSONObject;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.api.ConfigService;
import org.onlyoffice.api.PermissionService;
import org.onlyoffice.utils.JwtManager;
import org.onlyoffice.utils.UrlManager;
import org.onlyoffice.utils.Utils;

public class ConfigServiceImpl extends DefaultComponent implements ConfigService {

    private UrlManager urlManager;
    private Utils utils;
    private JwtManager jwtManager;
    private PermissionService permissionService;

    protected UrlManager getUrlManager() {
        if (urlManager == null) {
            urlManager = Framework.getService(UrlManager.class);
        }
        return urlManager;
    }

    protected Utils getUtils() {
        if (utils == null) {
            utils = Framework.getService(Utils.class);
        }
        return utils;
    }

    protected JwtManager getJwtManager() {
        if (jwtManager == null) {
            jwtManager = Framework.getService(JwtManager.class);
        }
        return jwtManager;
    }

    protected PermissionService getPermissionService() {
        if (permissionService == null) {
            permissionService = Framework.getService(PermissionService.class);
        }
        return permissionService;
    }

    @Override
    public JSONObject createConfig(WebContext ctx, DocumentModel model) {
        UrlManager urlManager = getUrlManager();
        Utils utils = getUtils();
        JwtManager jwtManager = getJwtManager();
        PermissionService permissionService = getPermissionService();

        String user = ctx.getPrincipal().getName();
        String locale = ctx.getLocale().toLanguageTag();

        JSONObject responseJson = new JSONObject();
        JSONObject documentObject = new JSONObject();
        JSONObject editorConfigObject = new JSONObject();
        JSONObject userObject = new JSONObject();
        JSONObject permObject = new JSONObject();
        JSONObject customizationObject = new JSONObject();

        String docTitle = model.getTitle();
        String docFilename = model.getAdapter(BlobHolder.class).getBlob().getFilename();
        String docExt = utils.getFileExtension(docFilename);

        String contentUrl = urlManager.getContentUrl(ctx, model);
        String callbackUrl = urlManager.getCallbackUrl(ctx, model);

        Boolean hasWriteProperties = permissionService.checkPermission(model, ctx.getPrincipal(), SecurityConstants.WRITE_PROPERTIES);
        Boolean isEditable = utils.isEditable(docExt);
        Boolean isFillForm = utils.isFillForm(docExt);

        String mode = hasWriteProperties && (isEditable || isFillForm) ? "edit" : "view";

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
        permObject.put("edit", hasWriteProperties);

        responseJson.put("editorConfig", editorConfigObject);
        editorConfigObject.put("lang", locale);
        editorConfigObject.put("mode", mode);
        if (mode.equals("edit")) {
            editorConfigObject.put("callbackUrl", callbackUrl);
        }
        editorConfigObject.put("user", userObject);
        userObject.put("id", user);
        userObject.put("name", user);

        editorConfigObject.put("customization", customizationObject);
        JSONObject goBack = new JSONObject();
        goBack.put("url", urlManager.getGobackUrl(ctx, model));
        customizationObject.put("goback", goBack);

        if (jwtManager.isEnabled()) {
            responseJson.put("token", jwtManager.createToken(responseJson));
        }

        return responseJson;
    }
}
