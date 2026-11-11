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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.security.LockSecurityPolicy;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.utils.Utils;

/**
 * Checks permissions exactly like {@code CoreSession.hasPermission} does (ACLs and every registered security policy),
 * except that the lock of an ONLYOFFICE editing session does not prevent co-editors from joining it.
 */
public class PermissionServiceImpl extends DefaultComponent implements PermissionService {

    public Boolean checkPermission(final DocumentModel model, final NuxeoPrincipal principal, final String permission) {
        Session session = Framework.getService(RepositoryService.class).getSession(model.getRepositoryName());
        Document document = Framework.getService(Utils.class).resolveReference(session, model.getRef());
        SecurityService securityService = Framework.getService(SecurityService.class);

        boolean editingSessionLock = Framework.getService(LockService.class)
                .isEditingSessionLock(document.getUUID(), document.getLock());
        if (!editingSessionLock) {
            return securityService.checkPermission(document, principal, permission);
        }

        boolean ignoreLockPolicy = LockSecurityPolicy.isIgnorePolicy();
        LockSecurityPolicy.setIgnorePolicy(true);
        try {
            return securityService.checkPermission(document, principal, permission);
        } finally {
            LockSecurityPolicy.setIgnorePolicy(ignoreLockPolicy);
        }
    }
}
