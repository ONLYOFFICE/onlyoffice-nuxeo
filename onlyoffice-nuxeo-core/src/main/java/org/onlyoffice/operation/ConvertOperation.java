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

package org.onlyoffice.operation;

import org.apache.http.HttpEntity;
import org.json.JSONObject;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.api.ConvertService;
import org.onlyoffice.model.DocumentType;
import org.onlyoffice.utils.RequestManager;
import org.onlyoffice.utils.UrlManager;
import org.onlyoffice.utils.Utils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

@Operation(
        id = ConvertOperation.ID,
        category = Constants.CAT_DOCUMENT,
        label = "ONLYOFFICEConvert"
)
public class ConvertOperation {
    public static final String ID = "ONLYOFFICE.Convert";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext context;

    @Param(name = "id")
    protected String id;

    @Param(name = "language", required = false)
    protected String language;

    @OperationMethod
    public String run() throws OperationException {
        ConvertService convertService = Framework.getService(ConvertService.class);
        Utils utils = Framework.getService(Utils.class);
        UrlManager urlManager = Framework.getService(UrlManager.class);
        RequestManager requestManager = Framework.getService(RequestManager.class);

        DocumentModel model = session.getDocument(new IdRef(id));

        if (!session.hasPermission(model.getRef(), SecurityConstants.WRITE_PROPERTIES)) {
            throw new DocumentSecurityException(String.format("Privilege '%s' is not granted to '%s'",
                    SecurityConstants.WRITE_PROPERTIES, session.getPrincipal().getName()));
        }

        String key = utils.getDocumentKey(model);
        String fileName = model.getAdapter(BlobHolder.class).getBlob().getFilename();
        String title = utils.getTitleWithoutExtension(fileName);
        String currentExtension = utils.getFileExtension(fileName);

        if (!convertService.isConvertible(currentExtension)) {
            throw new OperationException("Document type is not supported!");
        }

        String documentType = utils.getDocumentType(currentExtension);
        String targetExtension = utils.getDefaultExtensionByType(DocumentType.valueOf(documentType.toUpperCase()));
        String contentUrl = urlManager.getContentUrl(context, model);

        Locale locale = Locale.ENGLISH;

        if (language != null && !language.isEmpty()) {
            locale = Locale.forLanguageTag(language);
        }

        JSONObject response;

        response = convertService.convert(key, currentExtension, targetExtension, contentUrl, locale.toLanguageTag(), true);

        if (response.has("endConvert") && response.getBoolean("endConvert")) {
            requestManager.executeRequestToDocumentServer(response.getString("fileUrl"), new RequestManager.Callback<Void>() {
                public Void doWork(HttpEntity httpEntity) throws IOException {
                    Blob blob = Blobs.createBlob(httpEntity.getContent());
                    blob.setFilename(title + "." + targetExtension);
                    blob.setMimeType(utils.getMimeType(targetExtension));

                    model.setPropertyValue("file:content", (Serializable) blob);

                    session.saveDocument(model);
                    session.save();

                    return null;
                }
            });
        }

        return response.toString(2);
    }
}
