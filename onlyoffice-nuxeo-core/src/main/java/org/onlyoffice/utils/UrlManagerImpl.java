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

package org.onlyoffice.utils;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.constants.SettingsConstants;
import javax.servlet.http.HttpServletRequest;

public class UrlManagerImpl implements UrlManager {

    private static final String APPLICATION_NAME = "onlyoffice-nuxeo";
    private static final String DEVICE_ID = "document-server";

    @Override
    public String getDocServUrl() {
        String docServUrl = Framework.getProperty(SettingsConstants.DOC_SERVER_URL, "http://127.0.0.1/");

        return appendSlash(docServUrl);
    }

    @Override
    public String getInnerDocServUrl() {
        String innerDocServUrl = Framework.getProperty(SettingsConstants.DOC_SERVER_INNER_URL, null);
        if (innerDocServUrl == null || innerDocServUrl.isEmpty()) {
            return getDocServUrl();
        } else {
            return appendSlash(innerDocServUrl);
        }
    }

    @Override
    public String getBaseNuxeoUrl(WebContext ctx) {
        HttpServletRequest request = ctx.getRequest();
        String webAppName = VirtualHostHelper.getWebAppName(request);
        String serverUrl = null;

        String nuxeoServerInnerUrl = Framework.getProperty(SettingsConstants.NUXEO_SERVER_INNER_URL, null);
        if (nuxeoServerInnerUrl == null || nuxeoServerInnerUrl.isEmpty()) {
            serverUrl = appendSlash(ctx.getServerURL().toString());
        } else {
            serverUrl = appendSlash(nuxeoServerInnerUrl);
        }

        return StringUtils.isNotBlank(webAppName) ? serverUrl + webAppName + '/' : serverUrl;
    }

    @Override
    public String getBaseNuxeoUrl(OperationContext ctx) {
        HttpServletRequest request = (HttpServletRequest) ctx.get("request");
        String webAppName = VirtualHostHelper.getWebAppName(request);
        String serverUrl = null;

        String nuxeoServerInnerUrl = Framework.getProperty(SettingsConstants.NUXEO_SERVER_INNER_URL, null);
        if (nuxeoServerInnerUrl == null || nuxeoServerInnerUrl.isEmpty()) {
            serverUrl = appendSlash(VirtualHostHelper.getServerURL(request));
        } else {
            serverUrl = appendSlash(nuxeoServerInnerUrl);
        }

        return StringUtils.isNotBlank(webAppName) ? serverUrl + webAppName + '/' : serverUrl;
    }

    @Override
    public String replaceDocEditorURLToInnner(String url) {
        String innerDocEditorUrl = getInnerDocServUrl();
        String publicDocEditorUrl = getDocServUrl();

        if (!publicDocEditorUrl.equals(innerDocEditorUrl)) {
            url = url.replace(publicDocEditorUrl, innerDocEditorUrl);
        }

        return url;
    }

    @Override
    public String getContentUrl(WebContext ctx, DocumentModel model) {
        return String.format(
                "%1sapi/v1/onlyoffice/download/%2s?token=%3s",
                getBaseNuxeoUrl(ctx),
                model.getId(),
                generateToken(ctx.getPrincipal().getName())
        );
    }

    @Override
    public String getContentUrl(OperationContext ctx, DocumentModel model) {
        return String.format(
                "%1sapi/v1/onlyoffice/download/%2s?token=%3s",
                getBaseNuxeoUrl(ctx),
                model.getId(),
                generateToken(ctx.getPrincipal().getName())
        );
    }

    @Override
    public String getCallbackUrl(WebContext ctx, DocumentModel model) {
        return String.format(
                "%1sapi/v1/onlyoffice/callback/%2s?token=%3s",
                getBaseNuxeoUrl(ctx),
                model.getId(),
                generateToken(ctx.getPrincipal().getName())
        );
    }

    @Override
    public String getGobackUrl(WebContext ctx, DocumentModel model) {
        return String.format(
                "%1sui/#!/browse%2s",
                getBaseNuxeoUrl(ctx),
                model.getPath().removeLastSegments(1).toString()
        );
    }

    @Override
    public String getTestTxtUrl(WebContext ctx) {
        return String.format(
                "%1sapi/v1/onlyoffice/test-txt?token=%2s",
                getBaseNuxeoUrl(ctx),
                generateToken(ctx.getPrincipal().getName())
        );
    }

    private String appendSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    private String generateToken(String userName) {
        TokenAuthenticationService tokenAuthenticationService = Framework.getService(TokenAuthenticationService.class);

        return  tokenAuthenticationService.acquireToken(
                userName,
                APPLICATION_NAME,
                DEVICE_ID,
                "",
                "rw"
        );
    }
}
