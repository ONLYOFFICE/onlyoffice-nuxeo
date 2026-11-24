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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilsImpl extends DefaultComponent implements Utils {
    private static final Logger logger = LoggerFactory.getLogger(UtilsImpl.class);
    // Document Server accepts keys of up to 128 characters, and the digest of a blob can be a long SHA-256
    private static final int CONTENT_HASH_LENGTH = 16;
    private static final int CONTENT_HASH_PART = 2;

    @Override
    public String getChangeToken(final String key) {
        try {
            String decoded = new String(Base64.getDecoder().decode(key), "UTF-8");
            return decoded.split("__")[1];
        } catch (Exception e) {
            return null;
        }
    }

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

    @Override
    public String getContentHash(final String key) {
        try {
            String[] parts = new String(Base64.getDecoder().decode(key), StandardCharsets.UTF_8).split("__");
            if (parts.length <= CONTENT_HASH_PART) {
                return null;
            }
            return parts[CONTENT_HASH_PART].replace("_embedded", "");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getContentHash(final DocumentModel model) {
        BlobHolder holder = model.getAdapter(BlobHolder.class);
        Blob blob = holder == null ? null : holder.getBlob();
        if (blob == null || blob.getDigest() == null) {
            return null;
        }

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(blob.getDigest().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, CONTENT_HASH_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
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
