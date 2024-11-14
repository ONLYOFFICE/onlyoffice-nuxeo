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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.service.convert.ConvertService;
import org.apache.hc.core5.http.HttpEntity;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;
import com.onlyoffice.manager.request.RequestManager;
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

    @Param(name = "id")
    protected String id;

    @Param(name = "language", required = false)
    protected String language;

    @OperationMethod
    public String run() throws Exception {
        ConvertService convertService = Framework.getService(ConvertService.class);
        DocumentManager documentManager = Framework.getService(DocumentManager.class);
        RequestManager requestManager = Framework.getService(RequestManager.class);
        Utils utils = Framework.getService(Utils.class);

        DocumentModel model = session.getDocument(new IdRef(id));

        if (!session.hasPermission(model.getRef(), SecurityConstants.WRITE_PROPERTIES)) {
            throw new DocumentSecurityException(String.format("Privilege '%s' is not granted to '%s'",
                    SecurityConstants.WRITE_PROPERTIES, session.getPrincipal().getName()));
        }

        String fileName = model.getAdapter(BlobHolder.class).getBlob().getFilename();
        String title = documentManager.getBaseName(fileName);

        if (documentManager.getDefaultConvertExtension(fileName) == null) {
            throw new OperationException("Document type is not supported!");
        }

        String targetExtension = documentManager.getDefaultConvertExtension(fileName);

        Locale locale = Locale.ENGLISH;

        if (language != null && !language.isEmpty()) {
            locale = Locale.forLanguageTag(language);
        }

        ConvertRequest convertRequest = ConvertRequest.builder()
                .region(locale.toLanguageTag())
                .outputtype(targetExtension)
                .async(true)
                .build();

        ConvertResponse convertResponse = convertService.processConvert(convertRequest, id);

        if (convertResponse.getEndConvert() != null && convertResponse.getEndConvert()) {
            requestManager.executeGetRequest(convertResponse.getFileUrl(), new RequestManager.Callback<Void>() {
                public Void doWork(Object response) throws IOException {
                    Blob blob = Blobs.createBlob(((HttpEntity)response).getContent());
                    blob.setFilename(title + "." + targetExtension);
                    blob.setMimeType(utils.getMimeType(title + "." + targetExtension));

                    model.setPropertyValue("file:content", (Serializable) blob);

                    session.saveDocument(model);
                    session.save();

                    return null;
                }
            });
        }

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writeValueAsString(convertResponse);
    }
}
