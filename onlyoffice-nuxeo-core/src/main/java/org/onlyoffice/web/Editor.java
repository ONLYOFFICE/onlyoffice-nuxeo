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

package org.onlyoffice.web;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.api.ConfigService;
import org.onlyoffice.utils.UrlManager;
import org.onlyoffice.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/onlyedit")
@WebObject(type = "onlyedit")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
public class Editor extends ModuleRoot {

    private static final Logger logger = LoggerFactory.getLogger(Editor.class);

    private UrlManager urlManager;
    private Utils utils;
    private ConfigService configService;

    private TokenAuthenticationService authService;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);

        urlManager = Framework.getService(UrlManager.class);
        utils = Framework.getService(Utils.class);
        configService = Framework.getService(ConfigService.class);
        authService = Framework.getService(TokenAuthenticationService.class);
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public Object getEdit(@PathParam("id") String id, @QueryParam("mode") String mode) {
        WebContext ctx = getContext();
        CoreSession session = ctx.getCoreSession();
        DocumentModel model = session.getDocument(new IdRef(id));

        String docFilename = model.getAdapter(BlobHolder.class).getBlob().getFilename();
        String docExt = utils.getFileExtension(docFilename);
        String docType = utils.getDocumentType(docExt);

        try {
            return getView("index")
                .arg("config", configService.createConfig(ctx, model, mode))
                .arg("docUrl", urlManager.getDocServUrl())
                .arg("docTitle", model.getTitle())
                .arg("docType", docType);
        } catch (Exception e) {
            logger.error("Error while opening editor for " + id, e);
            return Response.serverError().build();
        }
    }
}
