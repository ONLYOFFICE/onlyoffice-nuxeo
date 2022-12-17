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

import java.util.List;

public class Format {
    public String name;
    public DocumentType type;
    public boolean edit;
    public List<String> convertTo;

    public Format(String name, DocumentType type, List<String> convertTo) {
        this.name = name;
        this.type = type;
        this.edit = false;
        this.convertTo = convertTo;
    }

    public Format(String name, DocumentType type, boolean edit, List<String> convertTo) {
        this.name = name;
        this.type = type;
        this.edit = edit;
        this.convertTo = convertTo;
    }

    public String getName() {
        return name;
    }

    public DocumentType getType() { return type; }

    public boolean isEdit() { return edit; }

    public List<String> getConvertTo() {
        return convertTo;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(DocumentType type) { this.type = type; }

    public void setConvertTo(List<String> convertTo) {
        this.convertTo = convertTo;
    }

    public void setEdit(boolean edit) { this.edit = edit; }
}