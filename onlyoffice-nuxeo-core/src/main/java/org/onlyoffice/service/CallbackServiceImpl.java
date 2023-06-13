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

package org.onlyoffice.service;

import org.json.JSONObject;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.api.CallbackService;
import org.onlyoffice.utils.UrlManager;
import org.onlyoffice.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CallbackServiceImpl extends DefaultComponent implements CallbackService {
    private static final Logger logger = LoggerFactory.getLogger(CallbackServiceImpl.class);

    private UrlManager urlManager;
    private Utils utils;

    protected UrlManager getUrlManager() {
        if (urlManager == null) {
            urlManager = Framework.getService(UrlManager.class);
        }
        return urlManager;
    }

    protected Utils getUtils() {
        if (utils == null) {
            utils = Framework.getService(Utils.class);
        }
        return utils;
    }

    @Override
    public void processCallback(CoreSession session, DocumentModel model, JSONObject json) throws Exception {
        switch (json.getInt("status")) {
            case 0:
                logger.error("ONLYOFFICE has reported that no doc with the specified key can be found");
                this.removeLock(session, model);
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
                this.removeLock(session, model);

                urlManager = getUrlManager();
                String documentUrl = urlManager.replaceDocEditorURLToInnner(json.getString("url"));

                updateDocument(session, model, json.getString("key"), documentUrl);
                break;
            case 3:
                logger.error("ONLYOFFICE has reported that saving the document has failed");
                this.removeLock(session, model);
                break;
            case 4:
                logger.info("No document updates, unlocking node");
                this.removeLock(session, model);
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

        utils = getUtils();
        model.putContextData(CoreSession.CHANGE_TOKEN, utils.getChangeToken(changeToken));

        session.saveDocument(model);
        session.save();
    }

    private void removeLock(CoreSession session, DocumentModel model) throws Exception {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        Session repoSession = repositoryService.getSession(model.getRepositoryName());

        Document doc = this.getUtils().resolveReference(repoSession, model.getRef());
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
