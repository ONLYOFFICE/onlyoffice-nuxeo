/**
 *
 * (c) Copyright Ascensio System SIA 2023
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

package org.onlyoffice.sdk.service.config;

import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.documenteditor.config.document.Permissions;
import com.onlyoffice.model.common.User;
import com.onlyoffice.service.documenteditor.config.DefaultConfigService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.service.PermissionService;

public class ConfigServiceImpl extends DefaultConfigService {
    private PermissionService permissionService;

    public ConfigServiceImpl() {
        super(
                Framework.getService(DocumentManager.class),
                Framework.getService(UrlManager.class),
                Framework.getService(JwtManager.class),
                Framework.getService(SettingsManager.class)
        );

        this.permissionService = Framework.getService(PermissionService.class);
    }

    @Override
    public Permissions getPermissions(final String fileId) {
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = ctx.getCoreSession();
        DocumentModel model = session.getDocument(new IdRef(fileId));
        String fileName = getDocumentManager().getDocumentName(fileId);

        Boolean editPermission = permissionService.checkPermission(model, ctx.getPrincipal(), SecurityConstants.WRITE_PROPERTIES);
        Boolean isEditable = getDocumentManager().isEditable(fileName) || getDocumentManager().isFillable(fileName);

        return Permissions.builder()
                .edit(editPermission && isEditable)
                .build();
    }

    @Override
    public User getUser() {
        WebContext ctx = WebEngine.getActiveContext();
        NuxeoPrincipal principal = ctx.getPrincipal();

        if (principal != null) {
            return User.builder()
                    .id(principal.getName())
                    .name(principal.getFirstName() + " " + principal.getLastName())
                    .build();
        } else {
            return super.getUser();
        }
    }
}
