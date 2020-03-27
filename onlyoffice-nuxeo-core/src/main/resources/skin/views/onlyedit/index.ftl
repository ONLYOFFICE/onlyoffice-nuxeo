<!DOCTYPE html>
<html>
<head>
    <meta http-equiv='Content-Type' content='text/html; charset=utf-8'>
    <link rel="icon" sizes="32x32" href="images/touch/favicon-32x32.png" type="image/png">
    <link rel="icon" sizes="16x16" href="images/touch/favicon-16x16.png" type="image/png">
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
    <script id="scriptApi" type="text/javascript" src="${docUrl}OfficeWeb/apps/api/documents/api.js"></script>
</head>
<body>
    <div id="placeholder"></div>
    <script>
        var config = ${config};
        new DocsAPI.DocEditor("placeholder", config);
    </script>
</body>
</html>