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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.settings.SettingsConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.ActiveWebContext;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper.FrameworkProperties;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestUrlManager {
    private static final String PRODUCT_INNER_URL_PROPERTY = "onlyoffice." + SettingsConstants.PRODUCT_INNER_URL;
    private static final String INNER_URL = "http://nuxeo-internal:8080";
    private static final long OTHER_REQUEST_TIMEOUT_MS = 10000;

    @Inject
    private UrlManager urlManager;

    @Inject
    private CoreSession session;

    @Inject
    private TokenAuthenticationService tokenAuthenticationService;

    private FrameworkProperties properties;
    private ActiveWebContext webContext;
    private DocumentModel model;

    @Before
    public void setUp() {
        properties = new FrameworkProperties().set(PRODUCT_INNER_URL_PROPERTY, null);

        DocumentModel folder = session.createDocument(session.createDocumentModel("/", "folder", "Folder"));
        model = session.createDocumentModel(folder.getPathAsString(), "doc", "File");
        model = session.createDocument(model);
        session.save();

        webContext = ActiveWebContext.activate(session);
    }

    @After
    public void tearDown() {
        webContext.close();
        properties.close();
    }

    private String baseUrl(final String serverUrl) {
        return serverUrl + "/" + VirtualHostHelper.getWebAppName(webContext.getRequest());
    }

    private String tokenOf(final String url) {
        return url.substring(url.indexOf("?token=") + "?token=".length());
    }

    @Test
    public void fileUrlPointsToDownloadEndpointWithUserToken() {
        String url = urlManager.getFileUrl(model.getId());

        assertTrue(url, url.startsWith(baseUrl(ActiveWebContext.SERVER_URL)
                + "/api/v1/onlyoffice/download/" + model.getId() + "?token="));
        assertEquals(session.getPrincipal().getName(), tokenAuthenticationService.getUserName(tokenOf(url)));
    }

    @Test
    public void callbackUrlPointsToCallbackEndpointWithUserToken() {
        String url = urlManager.getCallbackUrl(model.getId());

        assertTrue(url, url.startsWith(baseUrl(ActiveWebContext.SERVER_URL)
                + "/api/v1/onlyoffice/callback/" + model.getId() + "?token="));
        assertEquals(session.getPrincipal().getName(), tokenAuthenticationService.getUserName(tokenOf(url)));
    }

    /**
     * BUG: the token was acquired in the transaction of the current request, so Document Server, which calls Nuxeo
     * back while that request is still running (e.g. the settings validation), got a 401 for a new token.
     */
    @Test
    public void tokenIsUsableBeforeCurrentTransactionCommits() throws Exception {
        String token = tokenOf(urlManager.getTestConvertUrl(null));

        // what Document Server sees: another request, in another thread and transaction
        AtomicReference<String> userName = new AtomicReference<>();
        Thread documentServerRequest = new Thread(() -> userName.set(
                TransactionHelper.runInTransaction(() -> tokenAuthenticationService.getUserName(token))));
        documentServerRequest.start();
        documentServerRequest.join(OTHER_REQUEST_TIMEOUT_MS);

        assertEquals(session.getPrincipal().getName(), userName.get());
    }

    @Test
    public void innerUrlsUseProductInnerUrlWhenConfigured() {
        properties.set(PRODUCT_INNER_URL_PROPERTY, INNER_URL + "/");

        assertTrue(urlManager.getFileUrl(model.getId()).startsWith(baseUrl(INNER_URL) + "/api/"));
        assertTrue(urlManager.getCallbackUrl(model.getId()).startsWith(baseUrl(INNER_URL) + "/api/"));
        assertTrue(urlManager.getTestConvertUrl(null).startsWith(baseUrl(INNER_URL) + "/api/v1/onlyoffice/test-txt"));
    }

    @Test
    public void gobackUrlUsesPublicUrlAndParentFolder() {
        properties.set(PRODUCT_INNER_URL_PROPERTY, INNER_URL);

        assertEquals(baseUrl(ActiveWebContext.SERVER_URL) + "/ui/#!/browse/folder",
                urlManager.getGobackUrl(model.getId()));
    }
}
