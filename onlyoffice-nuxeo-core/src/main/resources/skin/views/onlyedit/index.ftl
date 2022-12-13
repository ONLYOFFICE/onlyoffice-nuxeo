<!DOCTYPE html>
<html>
<head>
    <meta http-equiv='Content-Type' content='text/html; charset=utf-8'>

    <!--
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
     -->

    <link rel="shortcut icon" href="${skinPath}/images/${docType}.ico"/>
    <title>${docTitle} - ONLYOFFICE</title>
    <style>
        html, body {
            height: 100%;
            width: 100%;
            margin: 0;
            padding: 0;
        }
        body {
            background: #fff;
            color: #333;
            font-family: Arial, Tahoma,sans-serif;
            font-size: 12px;
            font-weight: normal;
            height: 100%;
            margin: 0;
            padding: 0;
            text-decoration: none;
        }
    </style>
    <script id="scriptApi" type="text/javascript" src="${docUrl}web-apps/apps/api/documents/api.js"></script>
</head>
<body>
    <div id="placeholder"></div>
    <script>
        var connectEditor = function () {
            var config = ${config};

            if ((config.document.fileType === "docxf" || config.document.fileType === "oform")
                && DocsAPI.DocEditor.version().split(".")[0] < 7) {
                alert("Please update ONLYOFFICE Docs to version 7.0 to work on fillable forms online.");
                return;
            }

            new DocsAPI.DocEditor("placeholder", config);
        }

        connectEditor();
    </script>
</body>
</html>