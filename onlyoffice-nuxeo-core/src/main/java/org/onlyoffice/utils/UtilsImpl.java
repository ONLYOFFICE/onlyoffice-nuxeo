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

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.model.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;

public class UtilsImpl extends DefaultComponent implements Utils {
    private static final Logger logger = LoggerFactory.getLogger(UtilsImpl.class);
    private final String formatsPath = "app_data/document-formats/onlyoffice-docs-formats.json";
    private List<Format> supportedFormats;

    public UtilsImpl() {

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(formatsPath);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            supportedFormats = objectMapper.readValue(inputStream, new TypeReference<List<Format>>() { });
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static final Map<String, String> PathLocale = new HashMap<String, String>(){{
        put("az", "az-Latn-AZ");
        put("bg", "bg-BG");
        put("cs", "cs-CZ");
        put("de", "de-DE");
        put("el", "el-GR");
        put("en-GB", "en-GB");
        put("en", "en-US");
        put("es", "es-ES");
        put("fr", "fr-FR");
        put("it", "it-IT");
        put("ja", "ja-JP");
        put("ko", "ko-KR");
        put("lv", "lv-LV");
        put("nl", "nl-NL");
        put("pl", "pl-PL");
        put("pt-BR", "pt-BR");
        put("pt", "pt-PT");
        put("ru", "ru-RU");
        put("sk", "sk-SK");
        put("sv", "sv-SE");
        put("uk", "uk-UA");
        put("vi", "vi-VN");
        put("zh", "zh-CN");
    }};

    @Override
    public String getPathLocale(String language) {
        return PathLocale.get(language);
    }

    @Override
    public String getDocumentKey(DocumentModel model) {
        try {
            String key = model.getId() + "__" + model.getChangeToken();
            return Base64.getEncoder().encodeToString(key.getBytes("UTF-8"));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getDocumentType(String extension) {
        for (Format format : this.supportedFormats) {
            if (format.getName().equals(extension)) {

                String type = format.getType().name().toLowerCase();

                return type;
            }
        }

        return null;
    }

    public Boolean isViewable(String extension) {
        for (Format format : this.supportedFormats) {
            if (format.getName().equals(extension) && format.getActions().contains("view")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Boolean isEditable(String extension) {
        for (Format format : this.supportedFormats) {
            if (format.getName().equals(extension) && format.getActions().contains("edit")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Boolean isFillForm(String extension) {
        for (Format format : this.supportedFormats) {
            if (format.getName().equals(extension) && format.getActions().contains("fill")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getTitleWithoutExtension(String filename) {
        if (filename != null) {
            int index = filename.lastIndexOf('.');
            if (index > -1) {
                return filename.substring(0, filename.lastIndexOf("."));
            }
        }

        return filename;
    }

    @Override
    public String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1).trim().toLowerCase();
    }

    @Override
    public String getChangeToken(String key) {
        try {
            String decoded = new String(Base64.getDecoder().decode(key), "UTF-8");
            return decoded.split("__")[1];
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getMimeType(String extension) {
        try {
            return Framework.getService(MimetypeRegistry.class).getMimetypeFromExtension(extension);
        } catch (MimetypeNotFoundException | MimetypeDetectionException e) {
            return "application/octet-stream";
        }
    }

    @Override
    public Document resolveReference(Session session, DocumentRef docRef) {
        if (docRef == null) {
            throw new IllegalArgumentException("null docRref");
        } else {
            Object ref = docRef.reference();
            if (ref == null) {
                throw new IllegalArgumentException("null reference");
            } else {
                int type = docRef.type();
                switch(type) {
                    case 1:
                        return session.getDocumentByUUID((String)ref);
                    case 2:
                        return session.resolvePath((String)ref);
                    case 3:
                        return session.getDocumentByUUID(((DocumentModel)ref).getId());
                    default:
                        throw new IllegalArgumentException("Invalid type: " + type);
                }
            }
        }
    }

    public List<Format> getSupportedFormats() {
        return this.supportedFormats;
    }

    public JSONArray getSupportedFormatsAsJson() {
        return new JSONArray(this.supportedFormats);
    }
}
