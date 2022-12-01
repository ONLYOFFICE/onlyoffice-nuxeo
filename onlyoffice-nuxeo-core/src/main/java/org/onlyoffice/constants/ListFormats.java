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

package org.onlyoffice.constants;

import org.onlyoffice.model.DocumentType;

import java.util.ArrayList;
import java.util.List;

public class ListFormats {
    public static final List<Format> formats = new ArrayList<Format>() {{
        add(new Format("djvu", DocumentType.WORD));
        add(new Format("doc", DocumentType.WORD));
        add(new Format("docm", DocumentType.WORD));
        add(new Format("docx", DocumentType.WORD, true));
        add(new Format("docxf", DocumentType.WORD, true));
        add(new Format("oform", DocumentType.WORD, true));
        add(new Format("dot", DocumentType.WORD));
        add(new Format("dotm", DocumentType.WORD));
        add(new Format("dotx", DocumentType.WORD));
        add(new Format("epub", DocumentType.WORD));
        add(new Format("fb2", DocumentType.WORD));
        add(new Format("fodt", DocumentType.WORD));
        add(new Format("html", DocumentType.WORD));
        add(new Format("mht", DocumentType.WORD));
        add(new Format("odt", DocumentType.WORD));
        add(new Format("ott", DocumentType.WORD));
        add(new Format("pdf", DocumentType.WORD));
        add(new Format("rtf", DocumentType.WORD));
        add(new Format("txt", DocumentType.WORD));
        add(new Format("xps", DocumentType.WORD));
        add(new Format("oxps", DocumentType.WORD));
        add(new Format("xml", DocumentType.WORD));

        add(new Format("csv", DocumentType.CELL));
        add(new Format("fods", DocumentType.CELL));
        add(new Format("ods", DocumentType.CELL));
        add(new Format("ots", DocumentType.CELL));
        add(new Format("xls", DocumentType.CELL));
        add(new Format("xlsm", DocumentType.CELL));
        add(new Format("xlsx", DocumentType.CELL, true));
        add(new Format("xlt", DocumentType.CELL));
        add(new Format("xltm", DocumentType.CELL));
        add(new Format("xltx", DocumentType.CELL));

        add(new Format("fodp", DocumentType.SLIDE));
        add(new Format("odp", DocumentType.SLIDE));
        add(new Format("otp", DocumentType.SLIDE));
        add(new Format("pot", DocumentType.SLIDE));
        add(new Format("potm", DocumentType.SLIDE));
        add(new Format("potx", DocumentType.SLIDE));
        add(new Format("pps", DocumentType.SLIDE));
        add(new Format("ppsm", DocumentType.SLIDE));
        add(new Format("ppsx", DocumentType.SLIDE));
        add(new Format("ppt", DocumentType.SLIDE));
        add(new Format("pptm", DocumentType.SLIDE));
        add(new Format("pptx", DocumentType.SLIDE, true));
    }};

    public static List<Format> getSupportedFormats() {
        return formats;
    }
}
