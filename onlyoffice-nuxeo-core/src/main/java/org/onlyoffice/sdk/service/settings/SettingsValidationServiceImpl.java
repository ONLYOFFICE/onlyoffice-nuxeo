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

package org.onlyoffice.sdk.service.settings;

import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;

import com.onlyoffice.model.common.CommonResponse;
import com.onlyoffice.model.settings.validation.ValidationResult;
import com.onlyoffice.model.settings.validation.status.Status;
import com.onlyoffice.service.settings.DefaultSettingsValidationService;
import org.nuxeo.runtime.api.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SettingsValidationServiceImpl extends DefaultSettingsValidationService implements SettingsValidationService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsValidationServiceImpl.class);
    public SettingsValidationServiceImpl() {
        super(
                Framework.getService(RequestManager.class),
                Framework.getService(UrlManager.class),
                Framework.getService(SettingsManager.class)
        );
    }

    public Map<String, ValidationResult> validateSettings() {
        Map<String, ValidationResult> result = new HashMap<>();

        try {
            result.put(
                    "documentServer",
                    checkDocumentServer()
            );
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            result.put(
                    "documentServer",
                    ValidationResult.builder()
                            .status(Status.FAILED)
                            .error(CommonResponse.Error.CONNECTION)
                            .build()
            );
        }

        try {
            result.put(
                    "commandService",
                    checkCommandService()
            );
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            result.put(
                    "commandService",
                    ValidationResult.builder()
                            .status(Status.FAILED)
                            .error(CommonResponse.Error.CONNECTION)
                            .build()
            );
        }

        try {
            result.put(
                    "convertService",
                    checkConvertService()
            );
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            result.put(
                    "convertService",
                    ValidationResult.builder()
                            .status(Status.FAILED)
                            .error(CommonResponse.Error.CONNECTION)
                            .build()
            );
        }

        return result;
    }
}
