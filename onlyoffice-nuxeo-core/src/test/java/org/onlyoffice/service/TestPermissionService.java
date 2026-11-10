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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.inject.Inject;

import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Status;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.ActiveWebContext;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestPermissionService {
    private static final String USER = "jdoe";
    private static final String OTHER_USER = "bob";

    @Inject
    private PermissionService permissionService;

    @Inject
    private CoreSession session;

    @Inject
    private CoreFeature coreFeature;

    @Inject
    private CallbackService callbackService;

    private DocumentModel createDoc() {
        return TestHelper.createFile(session, "doc", "report.docx", "application/octet-stream", "x");
    }

    private boolean canWrite(final CoreSession userSession, final DocumentModel model) {
        DocumentModel userModel = userSession.getDocument(model.getRef());
        return permissionService.checkPermission(userModel, userSession.getPrincipal(),
                SecurityConstants.WRITE_PROPERTIES);
    }

    @Test
    public void administratorAlwaysHasPermission() {
        DocumentModel model = createDoc();

        assertTrue(permissionService.checkPermission(model, session.getPrincipal(),
                SecurityConstants.WRITE_PROPERTIES));
    }

    @Test
    public void userWithReadWriteCanWrite() {
        DocumentModel model = createDoc();
        TestHelper.grant(session, model, USER, SecurityConstants.READ_WRITE);

        assertTrue(canWrite(coreFeature.getCoreSession(USER), model));
    }

    @Test
    public void userWithReadOnlyCannotWrite() {
        DocumentModel model = createDoc();
        TestHelper.grant(session, model, USER, SecurityConstants.READ);

        assertFalse(canWrite(coreFeature.getCoreSession(USER), model));
    }

    @Test
    public void lockOwnerCanStillWrite() {
        DocumentModel model = createDoc();
        TestHelper.grant(session, model, USER, SecurityConstants.READ_WRITE);

        CoreSession userSession = coreFeature.getCoreSession(USER);
        userSession.setLock(model.getRef());

        assertTrue(canWrite(userSession, model));
    }

    /**
     * Co-editing: the document is locked by the ONLYOFFICE session of the first editor (EDITING callback), other
     * users with write access must still be able to join the same editing session.
     */
    @Test
    public void coEditorCanWriteDocumentLockedByOnlyofficeSession() throws Exception {
        DocumentModel model = createDoc();
        TestHelper.grant(session, model, USER, SecurityConstants.READ_WRITE);
        TestHelper.grant(session, model, OTHER_USER, SecurityConstants.READ_WRITE);

        Callback editing = new Callback();
        editing.setStatus(Status.EDITING);
        try (ActiveWebContext ignored = ActiveWebContext.activate(coreFeature.getCoreSession(OTHER_USER))) {
            callbackService.processCallback(editing, model.getId());
        }

        assertTrue(canWrite(coreFeature.getCoreSession(USER), model));
    }

    /**
     * BUG: Nuxeo's default lock security policy denies write access to everyone but the lock owner, but the plugin
     * evaluates policies with an empty {@code new SecurityPolicyServiceImpl()} and ignores every registered policy.
     */
    @Test
    public void documentLockedByAnotherUserIsNotWritable() {
        DocumentModel model = createDoc();
        TestHelper.grant(session, model, USER, SecurityConstants.READ_WRITE);
        TestHelper.grant(session, model, OTHER_USER, SecurityConstants.READ_WRITE);

        coreFeature.getCoreSession(OTHER_USER).setLock(model.getRef());

        CoreSession userSession = coreFeature.getCoreSession(USER);
        boolean nuxeoDecision = userSession.hasPermission(model.getRef(), SecurityConstants.WRITE_PROPERTIES);

        assertFalse("Nuxeo itself denies write on a document locked by someone else", nuxeoDecision);
        assertEquals(nuxeoDecision, canWrite(userSession, model));
    }
}
