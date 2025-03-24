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

package org.onlyoffice.utils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
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


    @Override
    public String getChangeToken(String key) {
        try {
            String decoded = new String(Base64.getDecoder().decode(key), "UTF-8");
            return decoded.split("__")[1];
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Document resolveReference(Session session, DocumentRef docRef) {
        if (docRef == null) {
            throw new IllegalArgumentException("null docRref");
        } else {
            Object ref = docRef.reference();
            if (ref == null) {
                throw new IllegalArgumentException("null reference");
            } else {
                int type = docRef.type();
                switch(type) {
                    case 1:
                        return session.getDocumentByUUID((String)ref);
                    case 2:
                        return session.resolvePath((String)ref);
                    case 3:
                        return session.getDocumentByUUID(((DocumentModel)ref).getId());
                    default:
                        throw new IllegalArgumentException("Invalid type: " + type);
                }
            }
        }
    }

    @Override
    public String getMimeType(String extension) {
        try {
            return Framework.getService(MimetypeRegistry.class).getMimetypeFromExtension(extension);
        } catch (MimetypeNotFoundException | MimetypeDetectionException e) {
            return "application/octet-stream";
        }
    }
}
