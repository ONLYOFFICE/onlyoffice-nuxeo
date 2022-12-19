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

package org.onlyoffice.utils;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.onlyoffice.model.DocumentType;

public interface Utils {
    public String getPathLocale(String language);
    public String getDocumentKey(DocumentModel model);
    public String getTitleWithoutExtension(String filename);
    public String getFileExtension(String filename);
    public String getDocumentType(String ext);
    public String getChangeToken(String key);
    public String getDefaultExtensionByType(DocumentType documentType);
    public String getMimeType(String extension);
}
