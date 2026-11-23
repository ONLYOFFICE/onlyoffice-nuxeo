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

package org.onlyoffice.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import com.onlyoffice.manager.url.UrlManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.ActiveWebContext;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper;

/**
 * The links handed to Document Server bypass the Nuxeo authentication (link-token-service.xml): exactly those, and
 * nothing else.
 */
@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
@Deploy("org.nuxeo.ecm.platform.web.common")
public class TestLinkTokenOpenUrls {
    @Inject
    private UrlManager urlManager;

    @Inject
    private CoreSession session;

    private static boolean bypassesAuthentication(final String method, final String requestUri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(requestUri);

        PluggableAuthenticationService authenticationService = (PluggableAuthenticationService) Framework.getRuntime()
                .getComponent(PluggableAuthenticationService.NAME);
        return authenticationService.getOpenUrls().stream().anyMatch(openUrl -> openUrl.allowByPassAuth(request));
    }

    /** The request as the authentication filter sees it: the REST API servlet forwards /api/* to /site/api/*. */
    private static String forwarded(final String requestUri) {
        return requestUri.replace("/api/", "/site/api/");
    }

    private static String pathOf(final String url) {
        return URI.create(url).getRawPath();
    }

    @Test
    public void linksHandedToDocumentServerBypassAuthenticationForTheirMethodOnly() {
        DocumentModel model = TestHelper.createFile(session, "doc", "report.docx", "application/octet-stream", "x");
        String download;
        String callback;
        String testFile;
        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            download = pathOf(urlManager.getFileUrl(model.getId()));
            callback = pathOf(urlManager.getCallbackUrl(model.getId()));
            testFile = pathOf(urlManager.getTestConvertUrl(null));
        }

        for (String uri : new String[] {download, forwarded(download)}) {
            assertTrue(uri, bypassesAuthentication("GET", uri));
        }
        for (String uri : new String[] {callback, forwarded(callback)}) {
            assertTrue(uri, bypassesAuthentication("POST", uri));
        }
        for (String uri : new String[] {testFile, forwarded(testFile)}) {
            assertTrue(uri, bypassesAuthentication("GET", uri));
        }
        assertFalse(bypassesAuthentication("POST", download));
        assertFalse(bypassesAuthentication("GET", callback));
        assertFalse(bypassesAuthentication("POST", testFile));
    }

    @Test
    public void otherUrlsStayAuthenticated() {
        String[] uris = {
            "/nuxeo/api/v1/onlyoffice/settings",
            "/nuxeo/api/v1/onlyoffice/filter/8f1c5c2e",
            "/nuxeo/api/v1/onlyoffice/download/8f1c5c2e/other",
            "/nuxeo/api/v1/me",
            "/nuxeo/nxfile/default/8f1c5c2e/api/v1/onlyoffice/download/8f1c5c2e",
            "/nuxeo/site/automation/api/v1/onlyoffice/callback/8f1c5c2e",
        };

        for (String uri : uris) {
            assertFalse(uri, bypassesAuthentication("GET", uri));
            assertFalse(uri, bypassesAuthentication("POST", uri));
        }
    }
}
