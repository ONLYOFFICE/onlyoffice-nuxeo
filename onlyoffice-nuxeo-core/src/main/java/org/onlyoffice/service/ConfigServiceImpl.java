/**
 *
 * (c) Copyright Ascensio System SIA 2022
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
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.api.ConfigService;
import org.onlyoffice.utils.ConfigManager;
import org.onlyoffice.utils.JwtManager;
import org.onlyoffice.utils.Utils;

public class ConfigServiceImpl extends DefaultComponent implements ConfigService {

    private ConfigManager configManager;
    private Utils utils;
    private JwtManager jwtManager;
    private TokenAuthenticationService authService;

    protected ConfigManager getConfigManager() {
        if (configManager == null) {
            configManager = Framework.getService(ConfigManager.class);
        }
        return configManager;
    }

    protected Utils getUtils() {
        if (utils == null) {
            utils = Framework.getService(Utils.class);
        }
        return utils;
    }

    protected TokenAuthenticationService getAuthService() {
        if (authService == null) {
            authService = Framework.getService(TokenAuthenticationService.class);
        }
        return authService;
    }

    protected JwtManager getJwtManager() {
        if (jwtManager == null) {
            jwtManager = Framework.getService(JwtManager.class);
        }
        return jwtManager;
    }
    @Override
    public JSONObject createConfig(WebContext ctx, DocumentModel model, String mode) throws Exception {
        ConfigManager configManager = getConfigManager();
        Utils utils = getUtils();
        TokenAuthenticationService authService = getAuthService();
        JwtManager jwtManager = getJwtManager();

        String user = ctx.getPrincipal().getName();
        String token = authService.acquireToken(user, "ONLYOFFICE", "editor", "auth", "rw");
        String baseUrl = configManager.getBaseNuxeoUrl(ctx);
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

        String contentUrl = String.format("%1snuxeo/nxfile/%2s/%3s/file:content/%4s?token=%5s", baseUrl, repoName, docId, docFilename, token);
        String callbackUrl = String.format("%1snuxeo/api/v1/onlyoffice/callback/%2s?token=%3s", baseUrl, docId, token);

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