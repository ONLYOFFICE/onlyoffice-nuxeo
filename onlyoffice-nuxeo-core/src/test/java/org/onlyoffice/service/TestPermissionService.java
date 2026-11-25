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
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestPermissionService {
    private static final String USER = "jdoe";
    private static final String OTHER_USER = "bob";
    private static final long LOCK_CREATION_GAP_MS = 1500;

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

    private boolean canEdit(final CoreSession userSession, final DocumentRef ref) {
        return permissionService.canEdit(userSession.getDocument(ref), userSession.getPrincipal());
    }

    private DocumentModel createEditableDoc() {
        DocumentModel model = createDoc();
        TestHelper.grant(session, model, USER, SecurityConstants.READ_WRITE);
        return model;
    }

    @Test
    public void liveDocumentCanBeEdited() {
        DocumentModel model = createEditableDoc();

        assertTrue(canEdit(coreFeature.getCoreSession(USER), model.getRef()));
    }

    @Test
    public void documentCannotBeEditedWithoutWritePermission() {
        DocumentModel model = createDoc();
        TestHelper.grant(session, model, USER, SecurityConstants.READ);

        assertFalse(canEdit(coreFeature.getCoreSession(USER), model.getRef()));
    }

    @Test
    public void versionCannotBeEdited() {
        DocumentModel model = createEditableDoc();
        DocumentRef version = session.checkIn(model.getRef(), VersioningOption.MINOR, null);
        session.save();

        assertFalse(canEdit(coreFeature.getCoreSession(USER), version));
    }

    @Test
    public void proxyCannotBeEdited() {
        DocumentModel model = createEditableDoc();
        DocumentRef version = session.checkIn(model.getRef(), VersioningOption.MINOR, null);
        DocumentModel versionProxy = session.createProxy(version, session.getRootDocument().getRef());
        DocumentModel liveProxy = session.createProxy(model.getRef(), session.getRootDocument().getRef());
        TestHelper.grant(session, versionProxy, USER, SecurityConstants.READ_WRITE);
        TestHelper.grant(session, liveProxy, USER, SecurityConstants.READ_WRITE);

        CoreSession userSession = coreFeature.getCoreSession(USER);
        assertFalse(canEdit(userSession, versionProxy.getRef()));
        assertFalse(canEdit(userSession, liveProxy.getRef()));
    }

    @Test
    public void trashedDocumentCannotBeEdited() {
        DocumentModel model = createEditableDoc();
        Framework.getService(TrashService.class).trashDocument(model);
        session.save();

        assertFalse(canEdit(coreFeature.getCoreSession(USER), model.getRef()));
    }

    @Test
    public void documentUnderLegalHoldCannotBeEdited() {
        DocumentModel model = createEditableDoc();
        session.makeRecord(model.getRef());
        session.setLegalHold(model.getRef(), true, null);
        session.save();

        assertFalse(canEdit(coreFeature.getCoreSession(USER), model.getRef()));
    }

    /**
     * What the callback of an editing session opened by the owner of {@code userSession} does: it runs as that user.
     */
    private void startEditingSession(final CoreSession userSession, final DocumentModel model) throws Exception {
        Callback editing = new Callback();
        editing.setStatus(Status.EDITING);
        try (NuxeoLoginContext ignored = TestHelper.loginAs(userSession)) {
            callbackService.processCallback(editing, model.getId());
        }
        assertEquals("the editing session lock is the user's", userSession.getPrincipal().getName(),
                session.getLockInfo(model.getRef()).getOwner());
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

        startEditingSession(coreFeature.getCoreSession(OTHER_USER), model);

        assertTrue(canWrite(coreFeature.getCoreSession(USER), model));
    }

    /**
     * A record of a former editing session must not make a lock set later by a user look like an editing session
     * lock.
     */
    @Test
    public void staleEditingSessionRecordDoesNotCoverManualLock() throws Exception {
        DocumentModel model = createDoc();
        TestHelper.grant(session, model, USER, SecurityConstants.READ_WRITE);
        TestHelper.grant(session, model, OTHER_USER, SecurityConstants.READ_WRITE);

        CoreSession otherSession = coreFeature.getCoreSession(OTHER_USER);
        startEditingSession(otherSession, model);

        // the editing session lock is removed outside of ONLYOFFICE, then the same user locks the document manually:
        // only its creation time tells it apart from the recorded editing session lock
        session.removeLock(model.getRef());
        Thread.sleep(LOCK_CREATION_GAP_MS);
        otherSession.setLock(model.getRef());

        assertFalse(canWrite(coreFeature.getCoreSession(USER), model));
    }

    @Test
    public void administratorCanWriteDocumentLockedByAnotherUser() {
        DocumentModel model = createDoc();
        TestHelper.grant(session, model, OTHER_USER, SecurityConstants.READ_WRITE);
        coreFeature.getCoreSession(OTHER_USER).setLock(model.getRef());

        assertEquals(session.hasPermission(model.getRef(), SecurityConstants.WRITE_PROPERTIES),
                permissionService.checkPermission(model, session.getPrincipal(), SecurityConstants.WRITE_PROPERTIES));
    }

    /**
     * Nuxeo's default lock security policy denies write access to everyone but the lock owner; the plugin used to
     * evaluate policies with an empty {@code new SecurityPolicyServiceImpl()} and ignore every registered policy.
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
