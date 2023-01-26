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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.api.ConvertService;
import org.onlyoffice.constants.Format;
import org.onlyoffice.constants.ListFormats;
import org.onlyoffice.utils.RequestManager;

import java.io.IOException;
import java.util.List;

public class ConvertServiceImpl extends DefaultComponent implements ConvertService {
    private RequestManager getRequestManager() {
        return Framework.getService(RequestManager.class);
    }

    @Override
    public JSONObject convert(String key, String currentExtension, String targetExtension, String url, String region, boolean async) throws Exception {
        JSONObject body = new JSONObject();
        body.put("async", async);
        body.put("embeddedfonts", true);
        body.put("filetype", currentExtension);
        body.put("outputtype", targetExtension);
        body.put("key", key);
        body.put("url", url);
        body.put("region", region);

        return getRequestManager().executeRequestToConversionService(body, new RequestManager.Callback<JSONObject>() {
            public JSONObject doWork(HttpEntity httpEntity) throws IOException {
                String content = IOUtils.toString(httpEntity.getContent(), "utf-8");

                JSONObject convertResponse = null;
                try {
                    convertResponse = new JSONObject(content);
                } catch (JSONException e) {
                    throw new JSONException("Couldn't convert JSON from Conversion Service: " + e.getMessage());
                }

                return convertResponse;
            }
        });
    }

    @Override
    public boolean isConvertible(String extension) {
        List<Format> supportedFormats = ListFormats.getSupportedFormats();

        for (Format format : supportedFormats) {
            if (format.getName().equals(extension)) {
                switch (format.type) {
                    case WORD:
                        return format.convertTo.contains("docx");
                    case CELL:
                        return format.convertTo.contains("xlsx");
                    case SLIDE:
                        return format.convertTo.contains("pptx");
                }
            }
        }

        return false;
    }
}
