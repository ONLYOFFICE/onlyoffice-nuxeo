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

package org.onlyoffice.listener;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Marks every save that replaces the file of a document as a user change.
 * <p>
 * The document key handed to Document Server carries only the user part of the change token, so that background
 * updates (thumbnails, fulltext, ...) do not split an editing session. Most ways of replacing the file
 * (Blob.AttachOnDocument, WebDAV, Drive, the conversion of this addon, ...) do not flag the save as a user change:
 * the key would stay the same, Document Server would keep serving the previous file from its cache, and the save
 * of an editing session would silently overwrite the new file.
 */
public class ContentChangeListener implements EventListener {

    private static final String CONTENT = "file:content";

    @Override
    public void handleEvent(final Event event) {
        EventContext context = event.getContext();
        if (!(context instanceof DocumentEventContext)) {
            return;
        }

        DocumentModel model = ((DocumentEventContext) context).getSourceDocument();
        if (model != null && model.hasSchema("file") && model.getProperty(CONTENT).isDirty()) {
            model.putContextData(CoreSession.USER_CHANGE, Boolean.TRUE);
        }
    }
}
