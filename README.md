# Nuxeo ONLYOFFICE integration plugin

This plugin enables users to edit office documents from [Nuxeo](https://www.nuxeo.com/) using ONLYOFFICE Document Server - [Community or Integration Edition](#onlyoffice-document-server-editions).

## Features

The plugin allows to:

* Create and edit text documents, spreadsheets, and presentations.
* Share documents with other users.
* Co-edit documents in real-time: use two co-editing modes (Fast and Strict), Track Changes, comments, and built-in chat.

Supported formats:

* For viewing and editing: DOCX, XLSX, PPTX.
* For viewing only: PDF, ODT, ODS, ODP, DOC, XLS, PPT.

## Installing ONLYOFFICE Document Server

You will need an instance of ONLYOFFICE Document Server that is resolvable and connectable both from Nuxeo and any end-clients. ONLYOFFICE Document Server must also be able to POST to Nuxeo directly.

You can install free Community version of ONLYOFFICE Document Server or scalable enterprise-level Integration Edition.

To install free Community version, use [Docker](https://github.com/onlyoffice/Docker-DocumentServer) (recommended) or follow [these instructions](https://helpcenter.onlyoffice.com/server/linux/document/linux-installation.aspx) for Debian, Ubuntu, or derivatives.  

To install Integration Edition, follow instructions [here](https://helpcenter.onlyoffice.com/server/integration-edition/index.aspx).

Community Edition vs Integration Edition comparison can be found [here](#onlyoffice-document-server-editions).

## Installing Nuxeo ONLYOFFICE integration plugin

Install it from [marketplace](https://connect.nuxeo.com/nuxeo/site/marketplace).

You can also install it using [nuxeoctl](https://doc.nuxeo.com/nxdoc/installing-a-new-package-on-your-instance/).
```
nuxeoctl mp-install /path/to/onlyoffice-nuxeo-package-x.x.zip
```

## Configuring Nuxeo ONLYOFFICE integration plugin

Edit [nuxeo.conf](https://doc.nuxeo.com/nxdoc/configuration-parameters-index-nuxeoconf/) and add following lines:
```
onlyoffice.docserv.url=http://documentserver/
onlyoffice.jwt.secret=
```
If you used Docker to install ONLYOFFICE Document Server, use information from [this repo](https://github.com/ONLYOFFICE/Docker-DocumentServer/#available-configuration-parameters) to configure JWT.

If you used other installation options, check the [API documentation](https://api.onlyoffice.com/editors/signature/) for configuring JWT on the Document Server side.  

## Compiling Nuxeo ONLYOFFICE plugin

To build Nuxeo plugin, the following steps must be performed for Ubuntu 14.04:

1. The stable Java version is necessary for the successful build. If you do not have it installed, use the following commands to install Open JDK 8:
    ```bash
    sudo apt-get update
    sudo apt-get install openjdk-8-jdk
    ```

2. Install latest Maven:
Installation process is described [here](https://maven.apache.org/install.html)

3. Download the Nuxeo ONLYOFFICE integration plugin source code:
    ```bash
    git clone https://github.com/onlyoffice/onlyoffice-nuxeo.git
    ```

4. Compile Nuxeo ONLYOFFICE integration plugin:
    ```bash
    cd onlyoffice-nuxeo/
    mvn clean install
    ```

5. Built package is located here `./onlyoffice-nuxeo-package/target/onlyoffice-nuxeo-package-x.x.zip`

## How it works

The ONLYOFFICE integration follows the API documented [here](https://api.onlyoffice.com/editors/basic):

## ONLYOFFICE Document Server editions 

ONLYOFFICE offers different versions of its online document editors that can be deployed on your own servers.

**ONLYOFFICE Document Server:**

* Community Edition (`onlyoffice-documentserver` package)
* Integration Edition (`onlyoffice-documentserver-ie` package)

The table below will help you make the right choice.

| Pricing and licensing | Community Edition | Integration Edition |
| ------------- | ------------- | ------------- |
| | [Get it now](https://www.onlyoffice.com/download.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo)  | [Start Free Trial](https://www.onlyoffice.com/connectors-request.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo)  |
| Cost  | FREE  | [Go to the pricing page](https://www.onlyoffice.com/integration-edition-prices.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo)  |
| Simultaneous connections | up to 20 maximum  | As in chosen pricing plan |
| Number of users | up to 20 recommended | As in chosen pricing plan |
| License | GNU AGPL v.3 | Proprietary |
| **Support** | **Community Edition** | **Integration Edition** | 
| Documentation | [Help Center](https://helpcenter.onlyoffice.com/server/docker/opensource/index.aspx) | [Help Center](https://helpcenter.onlyoffice.com/server/integration-edition/index.aspx) |
| Standard support | [GitHub](https://github.com/ONLYOFFICE/DocumentServer/issues) or paid | One year support included |
| Premium support | [Buy Now](https://www.onlyoffice.com/support.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo) | [Buy Now](https://www.onlyoffice.com/support.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo) |
| **Services** | **Community Edition** | **Integration Edition** | 
| Conversion Service                | + | + | 
| Document Builder Service          | + | + | 
| **Interface** | **Community Edition** | **Integration Edition** |
| Tabbed interface                       | + | + |
| White Label                            | - | - |
| Integrated test example (node.js)     | - | + |
| **Plugins & Macros** | **Community Edition** | **Integration Edition** |
| Plugins                           | + | + |
| Macros                            | + | + |
| **Collaborative capabilities** | **Community Edition** | **Integration Edition** |
| Two co-editing modes              | + | + |
| Comments                          | + | + |
| Built-in chat                     | + | + |
| Review and tracking changes       | + | + |
| Display modes of tracking changes | + | + |
| Version history                   | + | + |
| **Document Editor features** | **Community Edition** | **Integration Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Adding Content control          | - | + | 
| Editing Content control         | + | + | 
| Layout tools                    | + | + |
| Table of contents               | + | + |
| Navigation panel                | + | + |
| Comparing Documents             | - | +* |
| **Spreadsheet Editor features** | **Community Edition** | **Integration Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Functions, formulas, equations  | + | + |
| Table templates                 | + | + |
| Pivot tables                    | +** | +** |
| **Presentation Editor features** | **Community Edition** | **Integration Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Animations                      | + | + |
| Presenter mode                  | + | + |
| Notes                           | + | + |
| | [Get it now](https://www.onlyoffice.com/download.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo)  | [Start Free Trial](https://www.onlyoffice.com/connectors-request.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo)  |

\* It's possible to add documents for comparison from your local drive and from URL. Adding files for comparison from storage is not available yet.

\** Changing style and deleting (Full support coming soon)
