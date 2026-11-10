/**
 *
 * (c) Copyright Ascensio System SIA 2026
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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Remembers the owner and creation time of every lock set by an ONLYOFFICE editing session in the Nuxeo key/value
 * store (persistent and shared by the cluster nodes in the SQL and MongoDB configurations). A lock is recognized as
 * an editing session lock only if it still matches what was recorded, so a stale record never covers a lock set
 * later by a user.
 */
public class LockServiceImpl extends DefaultComponent implements LockService {
    private static final String STORE_NAME = "onlyoffice";
    private static final String KEY_PREFIX = "onlyoffice:lock:";
    private static final String SEPARATOR = "|";

    /**
     * Tolerance on the lock creation time, which may lose precision once persisted by the repository.
     */
    private static final long CREATED_TOLERANCE_MS = 1000;

    @Override
    public boolean lock(final CoreSession session, final DocumentModel model) {
        if (session.getLockInfo(model.getRef()) != null) {
            return false;
        }

        Lock lock = session.setLock(model.getRef());
        getStore().put(key(model.getId()), lock.getOwner() + SEPARATOR + lock.getCreated().getTimeInMillis());
        return true;
    }

    @Override
    public boolean isEditingSessionLock(final String documentId, final Lock lock) {
        if (lock == null || lock.getOwner() == null || lock.getCreated() == null) {
            return false;
        }

        String recorded = getStore().getString(key(documentId));
        if (recorded == null) {
            return false;
        }

        int separator = recorded.lastIndexOf(SEPARATOR);
        if (separator < 0) {
            return false;
        }

        try {
            String owner = recorded.substring(0, separator);
            long created = Long.parseLong(recorded.substring(separator + 1));

            return owner.equals(lock.getOwner())
                    && Math.abs(created - lock.getCreated().getTimeInMillis()) <= CREATED_TOLERANCE_MS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void forget(final String documentId) {
        getStore().put(key(documentId), (String) null);
    }

    private static String key(final String documentId) {
        return KEY_PREFIX + documentId;
    }

    private KeyValueStore getStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(STORE_NAME);
    }
}
