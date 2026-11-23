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

/**
 * Signs the links handed to Document Server. A token is valid only for the operation and the document it was issued
 * for, so a leaked link gives access to nothing else.
 */
public interface LinkTokenService {
    String PARAMETER = "token";

    enum Operation {
        DOWNLOAD,
        CALLBACK,
        TEST_FILE
    }

    /**
     * @param documentId  the document the link is for, null for the test file
     * @param documentKey the key of the editing session, for a callback link only
     */
    String createToken(Operation operation, String documentId, String userName, String documentKey);

    /**
     * @throws SecurityException if the token is not one issued for this operation and this document, or has expired
     */
    LinkToken verifyToken(String token, Operation operation, String documentId);

    final class LinkToken {
        private final String userName;
        private final String documentKey;

        public LinkToken(final String userName, final String documentKey) {
            this.userName = userName;
            this.documentKey = documentKey;
        }

        public String getUserName() {
            return userName;
        }

        public String getDocumentKey() {
            return documentKey;
        }
    }
}
