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

package org.onlyoffice.sdk.manager.document;

import com.onlyoffice.manager.document.DefaultDocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.utils.Utils;

public class DocumentManagerImpl extends DefaultDocumentManager {
    private final Utils utils;

    public DocumentManagerImpl() {
        super(Framework.getService(SettingsManager.class));

        this.utils = Framework.getService(Utils.class);
    }

    // ONLYOFFICE.Convert may run outside a web request, in an automation chain of a listener or a workflow
    private static CoreSession getSession() {
        WebContext ctx = WebEngine.getActiveContext();
        return ctx != null ? ctx.getCoreSession() : CoreInstance.getCoreSession(null);
    }

    @Override
    public String getDocumentKey(final String fileId, final boolean embedded) {
        return utils.getDocumentKey(getSession().getDocument(new IdRef(fileId)), embedded);
    }

    @Override
    public String getDocumentName(final String fileId) {
        DocumentModel model = getSession().getDocument(new IdRef(fileId));
        BlobHolder holder = model.getAdapter(BlobHolder.class);
        Blob blob = holder != null ? holder.getBlob() : null;
        return blob != null ? blob.getFilename() : null;
    }
}
