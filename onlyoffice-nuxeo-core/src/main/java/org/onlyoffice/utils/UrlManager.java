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

package org.onlyoffice.utils;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.model.WebContext;

public interface UrlManager {
    public String getDocServUrl();
    public String getInnerDocServUrl();
    public String getBaseNuxeoUrl(WebContext ctx);
    public String getBaseNuxeoUrl(OperationContext ctx);
    public String replaceDocEditorURLToInnner(String url);
    public String getContentUrl(WebContext ctx, DocumentModel model);
    public String getContentUrl(OperationContext ctx, DocumentModel model);
    public String getCallbackUrl(WebContext ctx, DocumentModel model);
    public String getGobackUrl(WebContext ctx, DocumentModel model);
    public String getTestTxtUrl(WebContext ctx);
}
