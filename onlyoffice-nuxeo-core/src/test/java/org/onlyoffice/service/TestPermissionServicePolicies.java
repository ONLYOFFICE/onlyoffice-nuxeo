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
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.ActiveWebContext;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper;

/**
 * Registered security policies other than the lock one must be respected, also during an editing session.
 */
@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
@Deploy("org.onlyoffice.onlyoffice-nuxeo-core:OSGI-INF/test-deny-restricted-policy.xml")
public class TestPermissionServicePolicies {
    private static final String USER = "jdoe";
    private static final String OTHER_USER = "bob";

    @Inject
    private PermissionService permissionService;

    @Inject
    private CallbackService callbackService;

    @Inject
    private CoreSession session;

    @Inject
    private CoreFeature coreFeature;

    private DocumentModel createDoc(final String name) {
        DocumentModel model = TestHelper.createFile(session, name, "report.docx", "application/octet-stream", name);
        TestHelper.grant(session, model, USER, SecurityConstants.READ_WRITE);
        TestHelper.grant(session, model, OTHER_USER, SecurityConstants.READ_WRITE);
        return model;
    }

    private boolean canWrite(final String user, final DocumentModel model) {
        CoreSession userSession = coreFeature.getCoreSession(user);
        return permissionService.checkPermission(userSession.getDocument(model.getRef()), userSession.getPrincipal(),
                SecurityConstants.WRITE_PROPERTIES);
    }

    @Test
    public void customPolicyIsRespected() {
        assertFalse(canWrite(USER, createDoc(DenyRestrictedDocumentPolicy.RESTRICTED_NAME)));
        assertTrue(canWrite(USER, createDoc("regular")));
    }

    @Test
    public void customPolicyIsRespectedDuringEditingSession() throws Exception {
        DocumentModel model = createDoc(DenyRestrictedDocumentPolicy.RESTRICTED_NAME);

        Callback editing = new Callback();
        editing.setStatus(Status.EDITING);
        try (ActiveWebContext ignored = ActiveWebContext.activate(session)) {
            callbackService.processCallback(editing, model.getId());
        }

        assertFalse(canWrite(USER, model));
    }
}
