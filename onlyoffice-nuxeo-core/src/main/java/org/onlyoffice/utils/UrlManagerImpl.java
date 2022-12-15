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

package org.onlyoffice.utils;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.constants.SettingsConstants;

public class UrlManagerImpl implements UrlManager {
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
        String nuxeoServerInnerUrl = Framework.getProperty(SettingsConstants.NUXEO_SERVER_INNER_URL, null);
        if (nuxeoServerInnerUrl == null || nuxeoServerInnerUrl.isEmpty()) {
            return appendSlash(ctx.getServerURL().toString());
        } else {
            return appendSlash(nuxeoServerInnerUrl);
        }
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
        TokenAuthenticationService tokenAuthenticationService = Framework.getService(TokenAuthenticationService.class);

        return String.format(
                "%1snuxeo/nxfile/%2s/%3s/file:content/%4s?token=%5s",
                getBaseNuxeoUrl(ctx),
                ctx.getCoreSession().getRepositoryName(),
                model.getId(),
                model.getAdapter(BlobHolder.class).getBlob().getFilename(),
                tokenAuthenticationService.acquireToken(ctx.getPrincipal().getName(), "ONLYOFFICE", "editor", "auth", "rw")
        );
    }

    @Override
    public String getCallbackUrl(WebContext ctx, DocumentModel model) {
        TokenAuthenticationService tokenAuthenticationService = Framework.getService(TokenAuthenticationService.class);

        return String.format(
                "%1snuxeo/api/v1/onlyoffice/callback/%2s?token=%3s",
                getBaseNuxeoUrl(ctx),
                model.getId(),
                tokenAuthenticationService.acquireToken(ctx.getPrincipal().getName(), "ONLYOFFICE", "editor", "auth", "rw")
        );
    }

    private String appendSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}
