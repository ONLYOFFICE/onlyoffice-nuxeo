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

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import java.io.IOException;

public class RequestManagerImpl extends DefaultComponent implements RequestManager {

    public static final int REQUEST_TIMEOUT = 60;

    private UrlManager getUrlManager() {
        return Framework.getService(UrlManager.class);
    }

    private JwtManager getJwtManager() {
        return Framework.getService(JwtManager.class);
    }

    @Override
    public <R> R executeRequestToDocumentServer(String url, RequestManager.Callback<R> callback) throws Exception {
        HttpGet request = new HttpGet(getUrlManager().replaceDocEditorURLToInnner(url));

        return executeRequest(request, "Document Server", callback);
    }

    @Override
    public <R> R executeRequestToCommandService(JSONObject body, RequestManager.Callback<R> callback) throws Exception {
        HttpPost request = new HttpPost(getUrlManager().getInnerDocServUrl() + "coauthoring/CommandService.ashx");

        return executeRequestPost(request, body, "Command Service", callback);
    }

    @Override
    public <R> R executeRequestToConversionService(JSONObject body, RequestManager.Callback<R> callback) throws Exception {
        HttpPost request = new HttpPost(getUrlManager().getInnerDocServUrl() + "ConvertService.ashx");
        return executeRequestPost(request, body, "Conversion Service", callback);
    }

    private <R> R executeRequestPost(HttpPost request, JSONObject body, String name, RequestManager.Callback<R> callback) throws Exception {
        if (getJwtManager().isEnabled()) {
            String token = getJwtManager().createToken(body);

            JSONObject payloadBody = new JSONObject();
            payloadBody.put("payload", body);

            String headerToken = getJwtManager().createToken(payloadBody);

            body.put("token", token);
            request.setHeader(getJwtManager().getJwtHeader(), "Bearer " + headerToken);
        }

        StringEntity requestEntity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);

        request.setEntity(requestEntity);
        request.setHeader("Accept", "application/json");
        return executeRequest(request, name, callback);
    }

    private <R> R executeRequest(HttpUriRequest request, String name, RequestManager.Callback<R> callback) throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                StatusLine statusLine = response.getStatusLine();
                if (statusLine == null) {
                    throw new RuntimeException(name + " returned no status " + request.getURI().toString());
                }

                HttpEntity resEntity = response.getEntity();
                if (resEntity == null) {
                    throw new RuntimeException(name + " did not return an entity " + request.getURI().toString());
                }

                int statusCode = statusLine.getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    throw new RuntimeException(name + " returned a " + statusCode + " status " + getErrorMessage(resEntity) + ' ' + request.getURI().toString());
                }

                R result = callback.doWork(resEntity);
                EntityUtils.consume(resEntity);

                return result;
            }
        }
    }

    private CloseableHttpClient getHttpClient() {
        Integer timeout = REQUEST_TIMEOUT * 1000;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).build();

        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

        return httpClient;
    }

    private String getErrorMessage(HttpEntity resEntity) throws IOException {
        String message = "";
        String content = EntityUtils.toString(resEntity);
        int i = content.indexOf("\"message\":\"");
        if (i != -1) {
            int j = content.indexOf("\",\"path\":", i);
            if (j != -1) {
                message = content.substring(i+11, j);
            }
        }
        return message;
    }
}