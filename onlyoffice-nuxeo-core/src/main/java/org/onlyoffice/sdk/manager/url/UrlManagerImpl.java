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

package org.onlyoffice.sdk.manager.url;

import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.settings.SettingsConstants;
import com.onlyoffice.manager.url.DefaultUrlManager;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.service.LinkTokenService;
import org.onlyoffice.service.LinkTokenService.Operation;

import jakarta.servlet.http.HttpServletRequest;

public class UrlManagerImpl extends DefaultUrlManager {
    private static final String NUXEO_URL_PROPERTY = "nuxeo.url";
    private static final String CONTEXT_PATH_PROPERTY = "org.nuxeo.ecm.contextPath";
    private static final String DEFAULT_CONTEXT_PATH = "/nuxeo";

    public UrlManagerImpl() {
        super(Framework.getService(SettingsManager.class));
    }

    // ONLYOFFICE.Convert may run outside a web request, in an automation chain of a listener or a workflow
    private static CoreSession getSession(final WebContext ctx) {
        return ctx != null ? ctx.getCoreSession() : CoreInstance.getCoreSession(null);
    }

    @Override
    public String getFileUrl(final String fileId) {
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = getSession(ctx);

        DocumentModel model = session.getDocument(new IdRef(fileId));

        return String.format(
                "%1s/api/v1/onlyoffice/download/%2s?%3s=%4s",
                getBaseNuxeoUrl(ctx, true),
                model.getId(),
                LinkTokenService.PARAMETER,
                createToken(Operation.DOWNLOAD, model.getId(), session, null)
        );
    }

    @Override
    public String getCallbackUrl(final String fileId) {
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = getSession(ctx);

        DocumentModel model = session.getDocument(new IdRef(fileId));

        return String.format(
                "%1s/api/v1/onlyoffice/callback/%2s?%3s=%4s",
                getBaseNuxeoUrl(ctx, true),
                model.getId(),
                LinkTokenService.PARAMETER,
                createToken(Operation.CALLBACK, model.getId(), session,
                        Framework.getService(DocumentManager.class).getDocumentKey(model.getId(), false))
        );
    }

    @Override
    public String getGobackUrl(final String fileId) {
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = getSession(ctx);

        DocumentModel model = session.getDocument(new IdRef(fileId));

        // a version whose live document was deleted has no path
        if (model.getPath() == null) {
            return getBaseNuxeoUrl(ctx, false) + "/ui/";
        }

        return String.format(
                "%1s/ui/#!/browse%2s",
                getBaseNuxeoUrl(ctx, false),
                model.getPath().removeLastSegments(1).toString()
        );
    }

    @Override
    public String getTestConvertUrl(final String productInnerUrl) {
        WebContext ctx = WebEngine.getActiveContext();

        return String.format(
                "%1s/api/v1/onlyoffice/test-txt?%2s=%3s",
                getBaseNuxeoUrl(ctx, true),
                LinkTokenService.PARAMETER,
                createToken(Operation.TEST_FILE, null, getSession(ctx), null)
        );
    }

    private String getBaseNuxeoUrl(final WebContext ctx, final Boolean inner) {
        String productInnerUrl = getSettingsManager().getSetting(SettingsConstants.PRODUCT_INNER_URL);

        // outside a request, the address of Nuxeo comes from its settings, as for its own links (mails, invitations)
        if (ctx == null) {
            if (inner && StringUtils.isNotBlank(productInnerUrl)) {
                return sanitizeUrl(productInnerUrl)
                        + Framework.getProperty(CONTEXT_PATH_PROPERTY, DEFAULT_CONTEXT_PATH);
            }
            String nuxeoUrl = Framework.getProperty(NUXEO_URL_PROPERTY);
            if (StringUtils.isBlank(nuxeoUrl)) {
                throw new NuxeoException("The address of Nuxeo is not set (" + NUXEO_URL_PROPERTY + ")");
            }
            return sanitizeUrl(nuxeoUrl);
        }

        HttpServletRequest request = ctx.getRequest();
        String webAppName = VirtualHostHelper.getWebAppName(request);
        String serverUrl = null;

        if (inner && productInnerUrl != null && !productInnerUrl.isEmpty()) {
            serverUrl = productInnerUrl;
        } else {
            serverUrl = ctx.getServerURL().toString();
        }

        return StringUtils.isNotBlank(webAppName) ? sanitizeUrl(serverUrl) + "/" + webAppName : sanitizeUrl(serverUrl);
    }

    private static String createToken(final Operation operation, final String documentId, final CoreSession session,
                                      final String documentKey) {
        return Framework.getService(LinkTokenService.class)
                .createToken(operation, documentId, session.getPrincipal().getName(), documentKey);
    }
}
