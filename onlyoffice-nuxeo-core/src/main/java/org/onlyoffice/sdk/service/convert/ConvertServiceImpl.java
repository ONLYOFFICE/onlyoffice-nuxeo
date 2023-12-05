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

package org.onlyoffice.sdk.service.convert;

import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.service.convert.ConvertService;
import com.onlyoffice.service.convert.DefaultConvertService;
import org.nuxeo.runtime.api.Framework;


public class ConvertServiceImpl extends DefaultConvertService implements ConvertService {
    public ConvertServiceImpl() {
        super(
                Framework.getService(DocumentManager.class),
                Framework.getService(UrlManager.class),
                Framework.getService(RequestManager.class),
                Framework.getService(SettingsManager.class)
        );
    }
}