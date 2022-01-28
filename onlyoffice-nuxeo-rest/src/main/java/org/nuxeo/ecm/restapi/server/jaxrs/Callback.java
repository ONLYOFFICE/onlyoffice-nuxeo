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

package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.utils.JwtManager;
import org.onlyoffice.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/onlyoffice")
@WebObject(type = "onlyoffice")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
public class Callback extends DefaultObject {

    private static final Logger logger = LoggerFactory.getLogger(Callback.class);

    private JwtManager jwtManager;
    private Utils utils;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);

        jwtManager = Framework.getService(JwtManager.class);
        utils = Framework.getService(Utils.class);
    }

    @POST
    @Path("callback/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object postCallback(@PathParam("id") String id, InputStream input) {
        Status code = Status.OK;
        Exception error = null;

        try {
            JSONObject json = new JSONObject(IOUtils.toString(input, Charset.defaultCharset()));

            if (jwtManager.isEnabled()) {
                String token = json.optString("token");
                Boolean inBody = true;
                
                if (token == null || token == "") {
                    List<String> values = getContext().getHttpHeaders().getRequestHeader("Authorization");
                    String header = values.isEmpty() ? null : values.get(0);
                    token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : header;
                    inBody = false;
                }

                if (token == null || token == "") {
                    throw new SecurityException("Expected JWT");
                }

                if (!jwtManager.verify(token)) {
                    throw new SecurityException("JWT verification failed");
                }

                JSONObject bodyFromToken = new JSONObject(
                        new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"));

                if (inBody) {
                    json = bodyFromToken;
                } else {
                    json = bodyFromToken.getJSONObject("payload");
                }
            }

            CoreSession session = getContext().getCoreSession();
            DocumentModel model = session.getDocument(new IdRef(id));

            processCallback(session, model, json);

        } catch (SecurityException ex) {
            code = Status.UNAUTHORIZED;
            error = ex;
            logger.error("Security error while saving document " + id, ex);
        } catch (Exception ex) {
            code = Status.INTERNAL_SERVER_ERROR;
            error = ex;
            logger.error("Error while saving document " + id, ex);
        }

        HashMap<String, Object> response = new HashMap<String, Object>();
        if (error != null) {
            response.put("error", 1);
            response.put("message", error.getMessage());
        } else {
            response.put("error", 0);
        }

        try {
            return Response.status(code).entity(new JSONObject(response).toString(2)).build();
        } catch (Exception e) {
            logger.error("Error while processing callback for " + id, e);
            return Response.status(code).build();
        }
    }

    private void processCallback(CoreSession session, DocumentModel model, JSONObject json) throws Exception {
        switch (json.getInt("status")) {
        case 0:
            logger.error("ONLYOFFICE has reported that no doc with the specified key can be found");
            model.removeLock();
            break;
        case 1:
            if (!model.isLocked()) {
                logger.info("Document open for editing, locking document");
                model.setLock();
            } else {
                logger.debug("Document already locked, another user has entered/exited");
            }
            break;
        case 2:
            logger.info("Document Updated, changing content");
            model.removeLock();
            updateDocument(session, model, json.getString("key"), json.getString("url"));
            break;
        case 3:
            logger.error("ONLYOFFICE has reported that saving the document has failed");
            model.removeLock();
            break;
        case 4:
            logger.info("No document updates, unlocking node");
            model.removeLock();
            break;
        }
    }

    private void updateDocument(CoreSession session, DocumentModel model, String changeToken, String url) throws Exception {
        Blob original = getBlob(model, "file:content");
        Blob saved = Blobs.createBlob(new URL(url).openStream(), original.getMimeType(), original.getEncoding());
        saved.setFilename(original.getFilename());

        DocumentHelper.addBlob(model.getProperty("file:content"), saved);

        if (model.hasFacet(FacetNames.VERSIONABLE)) {
            VersioningOption vo = VersioningOption.MINOR;
            model.putContextData(VersioningService.VERSIONING_OPTION, vo);
        }

        model.putContextData(CoreSession.CHANGE_TOKEN, utils.getChangeToken(changeToken));

        session.saveDocument(model);
        session.save();
    }

    private Blob getBlob(DocumentModel model, String xpath) {
        Blob blob = (Blob) model.getPropertyValue(xpath);
        if (blob == null) {
            BlobHolder bh = model.getAdapter(BlobHolder.class);
            if (bh != null) {
                blob = bh.getBlob();
            }
        }
        return blob;
    }
}
