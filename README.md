# Nuxeo ONLYOFFICE integration plugin

This plugin enables users to edit office documents from [Nuxeo](https://www.nuxeo.com/) using ONLYOFFICE Docs packaged as Document Server - [Community or Enterprise Edition](#onlyoffice-docs-editions).

## Features

The plugin allows to:

* Create and edit text documents, spreadsheets, and presentations.
* Share documents with other users.
* Co-edit documents in real-time: use two co-editing modes (Fast and Strict), Track Changes, comments, and built-in chat.

Supported formats:

* For viewing and editing: DOCX, XLSX, PPTX.
* For viewing only: PDF, ODT, ODS, ODP, DOC, XLS, PPT.

## Installing ONLYOFFICE Docs

You will need an instance of ONLYOFFICE Docs (Document Server) that is resolvable and connectable both from Nuxeo and any end-clients. ONLYOFFICE Document Server must also be able to POST to Nuxeo directly.

You can install free Community version of ONLYOFFICE Docs or scalable Enterprise Edition with pro features.

To install free Community version, use [Docker](https://github.com/onlyoffice/Docker-DocumentServer) (recommended) or follow [these instructions](https://helpcenter.onlyoffice.com/installation/docs-community-install-ubuntu.aspx) for Debian, Ubuntu, or derivatives.  

To install Enterprise Edition, follow instructions [here](https://helpcenter.onlyoffice.com/installation/docs-enterprise-index.aspx).

Community Edition vs Enterprise Edition comparison can be found [here](#onlyoffice-docs-editions).

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

To build Nuxeo plugin, the following steps must be performed for Ubuntu:

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

The ONLYOFFICE integration follows the API documented [here](https://api.onlyoffice.com/editors/basic). 

## ONLYOFFICE Docs editions 

ONLYOFFICE offers different versions of its online document editors that can be deployed on your own servers.

* Community Edition (`onlyoffice-documentserver` package)
* Enterprise Edition (`onlyoffice-documentserver-ee` package)

The table below will help you make the right choice.

| Pricing and licensing | Community Edition | Enterprise Edition |
| ------------- | ------------- | ------------- |
| | [Get it now](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo#docs-enterprise)  |
| Cost  | FREE  | [Go to the pricing page](https://www.onlyoffice.com/docs-enterprise-prices.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo)  |
| Simultaneous connections | up to 20 maximum  | As in chosen pricing plan |
| Number of users | up to 20 recommended | As in chosen pricing plan |
| License | GNU AGPL v.3 | Proprietary |
| **Support** | **Community Edition** | **Enterprise Edition** | 
| Documentation | [Help Center](https://helpcenter.onlyoffice.com/installation/docs-community-index.aspx) | [Help Center](https://helpcenter.onlyoffice.com/installation/docs-enterprise-index.aspx) |
| Standard support | [GitHub](https://github.com/ONLYOFFICE/DocumentServer/issues) or paid | One year support included |
| Premium support | [Buy Now](https://www.onlyoffice.com/support.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo) | [Buy Now](https://www.onlyoffice.com/support.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo) |
| **Services** | **Community Edition** | **Enterprise Edition** | 
| Conversion Service                | + | + | 
| Document Builder Service          | + | + | 
| **Interface** | **Community Edition** | **Enterprise Edition** |
| Tabbed interface                       | + | + |
| Dark theme                             | + | + |
| 150% scaling                           | + | + |
| White Label                            | - | - |
| Integrated test example (node.js)     | - | + |
| **Plugins & Macros** | **Community Edition** | **Enterprise Edition** |
| Plugins                           | + | + |
| Macros                            | + | + |
| **Collaborative capabilities** | **Community Edition** | **Enterprise Edition** |
| Two co-editing modes              | + | + |
| Comments                          | + | + |
| Built-in chat                     | + | + |
| Review and tracking changes       | + | + |
| Display modes of tracking changes | + | + |
| Version history                   | + | + |
| **Document Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Adding Content control          | - | + | 
| Editing Content control         | + | + | 
| Layout tools                    | + | + |
| Table of contents               | + | + |
| Navigation panel                | + | + |
| Comparing Documents             | - | +* |
| **Spreadsheet Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Functions, formulas, equations  | + | + |
| Table templates                 | + | + |
| Pivot tables                    | + | + |
| Data validation                 | + | + |
| Conditional formatting  for viewing | +** | +** |
| Sheet Views                     | - | + |
| **Presentation Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Transitions                     | + | + |
| Presenter mode                  | + | + |
| Notes                           | + | + |
| | [Get it now](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo#docs-enterprise)  |

\* It's possible to add documents for comparison from your local drive and from URL. Adding files for comparison from storage is not available yet.

\*** Support for all conditions and gradient. Adding/Editing capabilities are coming soon
