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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.api.CallbackService;
import org.onlyoffice.api.ConfigService;
import org.onlyoffice.api.SettingsService;
import org.onlyoffice.utils.ConfigManager;
import org.onlyoffice.utils.JwtManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebObject(type = "onlyoffice")
public class OnlyofficeObject extends DefaultObject {

    private static final Logger logger = LoggerFactory.getLogger(OnlyofficeObject.class);

    private JwtManager jwtManager;
    private ConfigManager configManager;
    private SettingsService settingsService;
    private CallbackService callbackService;
    private ConfigService configService;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);

        jwtManager = Framework.getService(JwtManager.class);
        configManager = Framework.getService(ConfigManager.class);
        settingsService = Framework.getService(SettingsService.class);
        callbackService = Framework.getService(CallbackService.class);
        configService = Framework.getService(ConfigService.class);
    }

    @GET
    @Path("settings")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getSettings() {
        checkAdministrator();

        return Response.status(Status.OK)
                .entity(new JSONObject(settingsService.getSettings()).toString(2))
                .type("application/json")
                .build();
    }

    @POST
    @Path("settings")
    @Produces(MediaType.APPLICATION_JSON)
    public Object setSettings(InputStream input) throws IOException {
        checkAdministrator();

        JSONObject json = new JSONObject(IOUtils.toString(input, Charset.defaultCharset()));

        settingsService.updateSettings(json);

        return Response.status(Status.OK)
                .entity(new JSONObject(settingsService.getSettings()).toString(2))
                .type("application/json")
                .build();
    }

    @POST
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getConfig(InputStream input) throws Exception {

        JSONObject json = new JSONObject(IOUtils.toString(input, Charset.defaultCharset()));

        WebContext ctx = getContext();
        CoreSession session = ctx.getCoreSession();
        DocumentModel model = session.getDocument(new IdRef(json.getString("uid")));

        JSONObject response = new JSONObject();
        response.put("apiUrl", configManager.getDocServUrl());
        response.put("config", configService.createConfig(ctx, model, "edit"));

        return Response.status(Status.OK)
                .entity(response.toString(2))
                .type("application/json")
                .build();
    }

    @POST
    @Path("callback/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object postCallback(@PathParam("id") String id, InputStream input) {
        Status code = Status.OK;
        Exception error = null;

        try {
            JSONObject json = new JSONObject(IOUtils.toString(input, Charset.defaultCharset()));

            if (jwtManager.isEnabled()) {
                String token = json.optString("token");
                Boolean inBody = true;
                
                if (token == null || token == "") {
                    String jwtHeader = jwtManager.getJwtHeader();
                    List<String> values = getContext().getHttpHeaders().getRequestHeader(jwtHeader);
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

            callbackService.processCallback(session, model, json);

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

    private void checkAdministrator() {
        NuxeoPrincipal currentUser = getContext().getCoreSession().getPrincipal();
        if (!currentUser.isAdministrator()) {
            throw new WebSecurityException("You don't have the permission to ONLYOFFICE settings!");
        }
    }
}
