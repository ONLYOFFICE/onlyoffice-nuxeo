package org.onlyoffice.service;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.security.SecurityPolicyService;
import org.nuxeo.ecm.core.security.SecurityPolicyServiceImpl;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.api.PermissionService;
import org.onlyoffice.utils.Utils;

public class PermissionServiceImpl extends DefaultComponent implements PermissionService {

    private SecurityPolicyService securityPolicyService;

    public void activate(ComponentContext context) {
        this.securityPolicyService = new SecurityPolicyServiceImpl();
    }

    private Utils getUtils() {
        return (Utils)Framework.getService(Utils.class);
    }

    private SecurityService getSecurityService() {
        return (SecurityService)Framework.getService(SecurityService.class);
    }

    public Boolean checkPermission(DocumentModel model, NuxeoPrincipal principal, String permission) {
        if (principal.isAdministrator()) {
            return true;
        }

        String[] resolvedPermissions = this.getSecurityService().getPermissionsToCheck(permission);
        String[] additionalPrincipals = this.getSecurityService().getPrincipalsToCheck(principal);

        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        Session session = repositoryService.getSession(model.getRepositoryName());

        Document document = this.getUtils().resolveReference(session, model.getRef());
        ACP acp = model.getACP();

        Access access = this.securityPolicyService.checkPermission(document, acp, principal, permission, resolvedPermissions, additionalPrincipals);
        if (access != null && !Access.UNKNOWN.equals(access)) {
            return access.toBoolean();
        } else if (acp == null) {
            return false;
        } else {
            access = acp.getAccess(additionalPrincipals, resolvedPermissions);
            return access.toBoolean();
        }
    }
}
