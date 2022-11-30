/**
 *
 * (c) Copyright Ascensio System SIA 2022
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

package org.onlyoffice.operation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.model.DocumentType;
import org.onlyoffice.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Locale;

@Operation(
        id = CreateOperation.ID,
        category = Constants.CAT_DOCUMENT,
        label = "ONLYOFFICECreate"
)
public class CreateOperation {
    private static final Logger logger = LoggerFactory.getLogger(CreateOperation.class);

    public static final String ID = "ONLYOFFICE.Create";

    @Context
    protected CoreSession session;

    @Param(name = "path")
    protected String path;

    @Param(name = "title")
    protected String title;

    @Param(name = "type")
    protected String type;

    @Param(name = "language", required = false)
    protected String language;

    @OperationMethod
    public String run() throws IOException {
        Utils utils = Framework.getService(Utils.class);

        Locale locale = Locale.ENGLISH;

        if (language != null && !language.isEmpty()) {
            locale = Locale.forLanguageTag(language);
        }

        String pathLocale = utils.getPathLocale(locale.toLanguageTag());

        if (pathLocale == null) {
            pathLocale = utils.getPathLocale(locale.getLanguage());

            if (pathLocale == null) pathLocale = utils.getPathLocale("en");
        }

        DocumentType documentType = DocumentType.valueOf(type.toUpperCase());
        String extension = utils.getDefaultExtensionByType(documentType);

        try (InputStream inputStream = getClass().getResourceAsStream("/app_data/" + pathLocale  + "/new." + extension)){
            DocumentModel newDoc = session.createDocumentModel(path, title, "File");

            Blob blob = Blobs.createBlob(inputStream);
            blob.setFilename(title + "." + extension);
            blob.setMimeType(getMimeType(extension));

            newDoc.setPropertyValue("file:content", (Serializable) blob);

            DocumentModel result = session.createDocument(newDoc);

            return result.getId();
        }
    }

    private String getMimeType(String extension) {
        try {
            return Framework.getService(MimetypeRegistry.class).getMimetypeFromExtension(extension);
        } catch (MimetypeNotFoundException | MimetypeDetectionException e) {
            return "application/octet-stream";
        }
    }

}
