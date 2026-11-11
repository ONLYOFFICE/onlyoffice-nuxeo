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

import java.util.Arrays;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.security.AbstractSecurityPolicy;

/**
 * Test policy standing for any custom or add-on policy (retention, hold...): denies write access to documents named
 * {@value #RESTRICTED_NAME} whatever the ACLs say.
 */
public class DenyRestrictedDocumentPolicy extends AbstractSecurityPolicy {
    public static final String RESTRICTED_NAME = "restricted";

    @Override
    public Access checkPermission(final Document doc, final ACP mergedAcp, final NuxeoPrincipal principal,
                                  final String permission, final String[] resolvedPermissions,
                                  final String[] additionalPrincipals) {
        if (RESTRICTED_NAME.equals(doc.getName())
                && Arrays.asList(resolvedPermissions).contains(SecurityConstants.WRITE_PROPERTIES)) {
            return Access.DENY;
        }
        return Access.UNKNOWN;
    }

    @Override
    public boolean isRestrictingPermission(final String permission) {
        return SecurityConstants.WRITE_PROPERTIES.equals(permission);
    }

    @Override
    public boolean isExpressibleInQuery(final String repositoryName) {
        return true;
    }

    @Override
    public SQLQuery.Transformer getQueryTransformer(final String repositoryName) {
        return SQLQuery.Transformer.IDENTITY;
    }
}
