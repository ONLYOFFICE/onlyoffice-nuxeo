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

package org.onlyoffice.sdk.manager.url;

import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.settings.SettingsConstants;
import com.onlyoffice.manager.url.DefaultUrlManager;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;

import javax.servlet.http.HttpServletRequest;

public class UrlManagerImpl extends DefaultUrlManager {
    private static final String APPLICATION_NAME = "onlyoffice-nuxeo";
    private static final String DEVICE_ID = "document-server";

    public UrlManagerImpl() {
        super(Framework.getService(SettingsManager.class));
    }

    @Override
    public String getFileUrl(final String fileId) {
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = ctx.getCoreSession();

        DocumentModel model = session.getDocument(new IdRef(fileId));

        return String.format(
                "%1s/api/v1/onlyoffice/download/%2s?token=%3s",
                getBaseNuxeoUrl(ctx, true),
                model.getId(),
                generateToken(ctx.getPrincipal().getName())
        );
    }

    @Override
    public String getCallbackUrl(final String fileId) {
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = ctx.getCoreSession();

        DocumentModel model = session.getDocument(new IdRef(fileId));

        return String.format(
                "%1s/api/v1/onlyoffice/callback/%2s?token=%3s",
                getBaseNuxeoUrl(ctx, true),
                model.getId(),
                generateToken(ctx.getPrincipal().getName())
        );
    }

    @Override
    public String getGobackUrl(final String fileId) {
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = ctx.getCoreSession();

        DocumentModel model = session.getDocument(new IdRef(fileId));

        return String.format(
                "%1s/ui/#!/browse%2s",
                getBaseNuxeoUrl(ctx, false),
                model.getPath().removeLastSegments(1).toString()
        );
    }

    @Override
    public String getTestConvertUrl(String productInnerUrl) {
        WebContext ctx = WebEngine.getActiveContext();

        return String.format(
                "%1s/api/v1/onlyoffice/test-txt?token=%2s",
                getBaseNuxeoUrl(ctx, true),
                generateToken(ctx.getPrincipal().getName())
        );
    }

    private String getBaseNuxeoUrl(WebContext ctx, Boolean inner) {
        HttpServletRequest request = ctx.getRequest();
        String webAppName = VirtualHostHelper.getWebAppName(request);
        String serverUrl = null;

        String productInnerUrl = getSettingsManager().getSetting(SettingsConstants.PRODUCT_INNER_URL);
        if (inner && productInnerUrl != null && !productInnerUrl.isEmpty()) {
            serverUrl = productInnerUrl;
        } else {
            serverUrl = ctx.getServerURL().toString();
        }

        return StringUtils.isNotBlank(webAppName) ? sanitizeUrl(serverUrl) + "/" + webAppName : sanitizeUrl(serverUrl);
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
