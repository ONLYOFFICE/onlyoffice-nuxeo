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

package org.onlyoffice.utils;

import java.util.Arrays;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.service.LinkTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilsImpl extends DefaultComponent implements Utils {
    private static final Logger logger = LoggerFactory.getLogger(UtilsImpl.class);

    // Nuxeo's change token is "<system change token>-<user change token>" (just a timestamp when change tokens are
    // disabled); only the user part is what Nuxeo checks for concurrent updates, the system part also moves on
    // background updates (thumbnails, fulltext, ...)
    @Override
    public String getUserChangeToken(final String changeToken) {
        if (changeToken == null) {
            return null;
        }
        return changeToken.substring(changeToken.lastIndexOf('-') + 1);
    }

    // - only the user part of the change token: background updates of the document (thumbnails, fulltext, ...) must
    //   not change the key, users opening it in between would end up in separate editing sessions, and all but the
    //   first one to save would lose their changes;
    // - the digest of the file: the user change token misses most replacements of the file (Blob.AttachOnDocument,
    //   WebDAV, Drive, ...) and repeats after restoring a version, and Document Server would serve a cached file;
    // - a hash with a secret: without JWT, Document Server opens the editing session of whatever key it is given
    @Override
    public String getDocumentKey(final DocumentModel model, final boolean embedded) {
        BlobHolder holder = model.getAdapter(BlobHolder.class);
        Blob blob = holder == null ? null : holder.getBlob();

        return Framework.getService(LinkTokenService.class).hash(Arrays.asList(
                model.getId(),
                getUserChangeToken(model.getChangeToken()),
                blob == null ? null : blob.getDigest(),
                embedded ? "embedded" : null));
    }

    @Override
    public Document resolveReference(final Session session, final DocumentRef docRef) {
        if (docRef == null) {
            throw new IllegalArgumentException("null docRref");
        } else {
            Object ref = docRef.reference();
            if (ref == null) {
                throw new IllegalArgumentException("null reference");
            } else {
                int type = docRef.type();
                switch (type) {
                    case DocumentRef.ID:
                        return session.getDocumentByUUID((String) ref);
                    case DocumentRef.PATH:
                        return session.resolvePath((String) ref);
                    case DocumentRef.INSTANCE:
                        return session.getDocumentByUUID(((DocumentModel) ref).getId());
                    default:
                        throw new IllegalArgumentException("Invalid type: " + type);
                }
            }
        }
    }

    @Override
    public String getMimeType(final String extension) {
        try {
            return Framework.getService(MimetypeRegistry.class).getMimetypeFromExtension(extension);
        } catch (MimetypeNotFoundException | MimetypeDetectionException e) {
            return "application/octet-stream";
        }
    }
}
