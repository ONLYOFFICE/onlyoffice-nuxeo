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

package org.onlyoffice.sdk.service.callback;

import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.service.documenteditor.callback.DefaultCallbackService;
import org.apache.http.HttpEntity;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.LockException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.onlyoffice.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CallbackServiceImpl extends DefaultCallbackService {
    private static final Logger logger = LoggerFactory.getLogger(CallbackServiceImpl.class);

    private RequestManager requestManager;
    private Utils utils;

    public CallbackServiceImpl() {
        super(
                Framework.getService(JwtManager.class),
                Framework.getService(SettingsManager.class)
        );

        this.requestManager = Framework.getService(RequestManager.class);
        this.utils = Framework.getService(Utils.class);
    }

    @Override
    public void handlerEditing(final Callback callback, final String fileId) throws Exception {
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = ctx.getCoreSession();

        DocumentModel model = session.getDocument(new IdRef(fileId));

        if (!model.isLocked()) {
            logger.info("Document open for editing, locking document");
            model.setLock();
        } else {
            logger.debug("Document already locked, another user has entered/exited");
        }
    }

    @Override
    public void handlerSave(Callback callback, String fileId) throws Exception {
        logger.info("Document Updated, changing content");
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = ctx.getCoreSession();

        DocumentModel model = session.getDocument(new IdRef(fileId));

        this.removeLock(session, model);

        updateDocument(session, model, callback.getKey(), callback.getUrl());
    }

    @Override
    public void handlerClosed(Callback callback, String fileId) throws Exception {
        logger.info("No document updates, unlocking node");
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = ctx.getCoreSession();

        DocumentModel model = session.getDocument(new IdRef(fileId));

        this.removeLock(session, model);
    }

    private void updateDocument(CoreSession session, DocumentModel model, String changeToken, String url) throws Exception {
        Blob original = getBlob(model, "file:content");

        requestManager.executeGetRequest(url, new RequestManager.Callback<Void>() {
            @Override
            public Void doWork(final Object response) throws Exception {
                Blob saved = Blobs.createBlob(((HttpEntity)response).getContent(), original.getMimeType(), original.getEncoding());
                saved.setFilename(original.getFilename());

                DocumentHelper.addBlob(model.getProperty("file:content"), saved);

                if (model.hasFacet(FacetNames.VERSIONABLE)) {
                    VersioningOption vo = VersioningOption.MINOR;
                    model.putContextData(VersioningService.VERSIONING_OPTION, vo);
                }

                model.putContextData(CoreSession.CHANGE_TOKEN, utils.getChangeToken(changeToken));

                session.saveDocument(model);
                session.save();

                return null;
            }
        });
    }

    private void removeLock(CoreSession session, DocumentModel model) throws Exception {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        Session repoSession = repositoryService.getSession(model.getRepositoryName());

        Document doc = utils.resolveReference(repoSession, model.getRef());
        String owner = model.getLockInfo().getOwner();

        Lock lock = doc.removeLock(owner);
        if (lock == null) {

        } else if (lock.getFailed()) {
            throw new LockException("Document already locked by " + lock.getOwner() + ": " + model.getRef(), 409);
        } else {
            Map<String, Serializable> options = new HashMap();
            options.put("lock", lock);
            this.notifyEvent("documentUnlocked", model, options, (String)null, (String)null, true, false, session);
        }
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

    private void notifyEvent(String eventId, DocumentModel source, Map<String, Serializable> options, String category,
                             String comment, boolean withLifeCycle, boolean inline, CoreSession session) {
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), source);
        if (options != null) {
            ctx.setProperties(options);
        }

        ctx.setProperty("repositoryName", source.getRepositoryName());
        if (source != null && withLifeCycle) {
            String currentLifeCycleState = source.getCurrentLifeCycleState();
            ctx.setProperty("documentLifeCycle", currentLifeCycleState);
        }

        if (comment != null) {
            ctx.setProperty("comment", comment);
        }

        ctx.setProperty("category", category == null ? "eventDocumentCategory" : category);
        Event event = ctx.newEvent(eventId);
        if ("sessionSaved".equals(eventId)) {
            event.setIsCommitEvent(true);
        }

        if (inline) {
            event.setInline(true);
        }

        ((EventService)Framework.getService(EventService.class)).fireEvent(event);
    }
}
