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

/**
 * Keeps track of the locks set by ONLYOFFICE editing sessions, so that they can be told apart from locks set by
 * users in Nuxeo: an editing session lock must not prevent co-editors from joining, any other lock must be respected.
 */
public interface LockService {

    /**
     * Locks the document for an ONLYOFFICE editing session if it is not locked yet.
     *
     * @return {@code true} if the document has been locked by this call
     */
    boolean lock(CoreSession session, DocumentModel model);

    /**
     * Tells whether the given lock of the document was set by an ONLYOFFICE editing session.
     */
    boolean isEditingSessionLock(String documentId, Lock lock);

    /**
     * Forgets the editing session lock of the document once it has been removed.
     */
    void forget(String documentId);
}
