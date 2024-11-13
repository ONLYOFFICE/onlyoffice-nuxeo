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

package org.onlyoffice.web;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import com.onlyoffice.manager.url.UrlManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/onlyedit")
@WebObject(type = "onlyedit")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
public class Editor extends ModuleRoot {

    private static final Logger logger = LoggerFactory.getLogger(Editor.class);

    private UrlManager urlManager;
    private DocumentManager documentManager;
    private ConfigService configService;


    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        urlManager = Framework.getService(UrlManager.class);
        documentManager = Framework.getService(DocumentManager.class);
        configService = Framework.getService(ConfigService.class);
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public Object getEdit(@PathParam("id") String id) throws JsonProcessingException {
        try {
            WebContext ctx = getContext();
            CoreSession session = ctx.getCoreSession();

            DocumentModel model = session.getDocument(new IdRef(id));

            Config config = configService.createConfig(
                    id,
                    Mode.EDIT,
                    Type.DESKTOP
            );

            config.getEditorConfig().setLang(ctx.getLocale().toLanguageTag());

            String docFilename = model.getAdapter(BlobHolder.class).getBlob().getFilename();
            DocumentType docType = documentManager.getDocumentType(docFilename);

            ObjectMapper objectMapper = new ObjectMapper();

            return getView("index")
                .arg("config", objectMapper.writeValueAsString(config))
                .arg("docUrl", urlManager.getDocumentServerApiUrl())
                .arg("docTitle", model.getTitle())
                .arg("docType", docType.name().toLowerCase());
        } catch (DocumentSecurityException e) {
            return Response.status(403).build();
        } catch (DocumentNotFoundException e) {
            return Response.status(404).build();
        } catch (Exception e) {
            logger.error("Error while opening editor for " + id, e);
            throw new NuxeoException(e);
        }
    }
}
