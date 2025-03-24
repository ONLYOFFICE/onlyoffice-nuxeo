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

package org.onlyoffice.sdk.manager.document;

import com.onlyoffice.manager.document.DefaultDocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.api.Framework;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class DocumentMangerImpl extends DefaultDocumentManager {
    public DocumentMangerImpl() {
        super(Framework.getService(SettingsManager.class));
    }

    @Override
    public String getDocumentKey(final String fileId, final boolean embedded) {
        CoreSession session = WebEngine.getActiveContext().getCoreSession();

        DocumentModel model = session.getDocument(new IdRef(fileId));
        try {

            String key = model.getId() + "__" + model.getChangeToken();
            key = embedded ? key + "_embedded" : key;
            return Base64.getEncoder().encodeToString(key.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDocumentName(final String fileId) {
        CoreSession session = WebEngine.getActiveContext().getCoreSession();

        DocumentModel model = session.getDocument(new IdRef(fileId));
        return model.getAdapter(BlobHolder.class).getBlob().getFilename();
    }
}
