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


package org.onlyoffice.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.web.common.RequestContext;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * Makes a mocked {@link WebContext} visible through {@code WebEngine.getActiveContext()} for the current thread,
 * the same way WebEngine does it for a real HTTP request.
 */
public final class ActiveWebContext implements AutoCloseable {
    public static final String SERVER_URL = "http://nuxeo.example.com:8080";
    public static final String CONTEXT_PATH = "/nuxeo";

    private final RequestContext requestContext;
    private final WebContext webContext;
    private final HttpServletRequest request;

    private ActiveWebContext(final CoreSession session) {
        request = mock(HttpServletRequest.class);
        webContext = mock(WebContext.class);

        when(request.getAttribute(WebContext.class.getName())).thenReturn(webContext);
        when(request.getContextPath()).thenReturn(CONTEXT_PATH);

        when(webContext.getRequest()).thenReturn(request);
        when(webContext.getCoreSession()).thenReturn(session);
        when(webContext.getPrincipal()).thenReturn(session.getPrincipal());
        when(webContext.getServerURL()).thenAnswer(invocation -> new StringBuilder(SERVER_URL));
        when(webContext.getLocale()).thenReturn(Locale.ENGLISH);

        requestContext = new RequestContext(request, mock(HttpServletResponse.class));
    }

    public static ActiveWebContext activate(final CoreSession session) {
        return new ActiveWebContext(session);
    }

    public WebContext getWebContext() {
        return webContext;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public void close() {
        requestContext.dispose();
    }
}
