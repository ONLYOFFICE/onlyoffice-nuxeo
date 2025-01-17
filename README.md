# ONLYOFFICE addon package for Nuxeo

This package enables users to edit office documents from [Nuxeo](https://www.nuxeo.com/) using ONLYOFFICE Docs packaged as Document Server - [Community or Enterprise Edition](#onlyoffice-docs-editions).

## Features

The package allows to:

* Create and edit text documents, spreadsheets, and presentations.
* Share documents with other users.
* Co-edit documents in real-time: use two co-editing modes (Fast and Strict), Track Changes, comments, and built-in chat.

Supported formats:

**For viewing:**
* **WORD:** DJVU, DOC, DOCM, DOCX, DOCXF, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, MHT, ODT, OFORM, OTT, OXPS, PDF, RTF, TXT, XML, XPS
* **CELL:** CSV, FODS, ODS, OTS, XLS, XLSM, XLSX, XLT, XLTM, XLTX
* **SLIDE:** FODP, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX

**For editing:**

* **WORD:** DOCM, DOCX, DOCXF, DOTM, DOTX, HTM, XML
* **CELL:** XLSM, XLSX, XLTM, XLTX
* **SLIDE:** POTM, POTX, PPSM, PPSX, PPTM, PPTX

**For filling:**

* **WORD:** OFORM

**For converting to Office Open XML formats:**

* **WORD:** DOC, DOCM, DOCXF, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, MHT, ODT, OTT, OXPS, PDF, RTF, XML, XPS
* **CELL:** FODS, ODS, OTS, XLS, XLSM, XLT, XLTM, XLTX
* **SLIDE:** FODP, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM

## Installing ONLYOFFICE Docs

You will need an instance of ONLYOFFICE Docs (Document Server) that is resolvable and connectable both from Nuxeo and any end-clients. ONLYOFFICE Document Server must also be able to POST to Nuxeo directly.

You can install free Community version of ONLYOFFICE Docs or scalable Enterprise Edition with pro features.

To install free Community version, use [Docker](https://github.com/onlyoffice/Docker-DocumentServer) (recommended) or follow [these instructions](https://helpcenter.onlyoffice.com/installation/docs-community-install-ubuntu.aspx) for Debian, Ubuntu, or derivatives.  

To install Enterprise Edition, follow instructions [here](https://helpcenter.onlyoffice.com/installation/docs-enterprise-index.aspx).

Community Edition vs Enterprise Edition comparison can be found [here](#onlyoffice-docs-editions).

## Installing ONLYOFFICE addon package for Nuxeo

Install it from [marketplace](https://connect.nuxeo.com/nuxeo/site/marketplace).

You can also install it using [nuxeoctl](https://doc.nuxeo.com/nxdoc/installing-a-new-package-on-your-instance/).
```
nuxeoctl mp-install /path/to/onlyoffice-nuxeo-package-x.x.zip
```

## Configuring ONLYOFFICE addon package for Nuxeo

Open the [nuxeo.conf](https://doc.nuxeo.com/nxdoc/configuration-parameters-index-nuxeoconf/) file and enter the name of the server with ONLYOFFICE Docs installed:

```
onlyoffice.docserv.url=http://documentserver/
```
where the **documentserver** is the name of the server with **ONLYOFFICE Docs** installed. 
The address must be accessible from the user browser and from the Nuxeo server. 
The Nuxeo server address must also be accessible from **ONLYOFFICE Docs** for correct work.

Starting from version 7.2, JWT is enabled by default and the secret key is generated automatically to restrict the access to ONLYOFFICE Docs and for security reasons and data integrity. 
Specify your own secret key by adding the `onlyoffice.jwt.secret=yoursecret` line to the **nuxeo.conf** file. 
In the ONLYOFFICE Docs [config file](https://api.onlyoffice.com/editors/signature/), specify the same secret key and enable the validation.

## Compiling ONLYOFFICE addon package for Nuxeo

To build Nuxeo package, the following steps must be performed for Ubuntu:

1. The stable Java version is necessary for the successful build. If you do not have it installed, use the following commands to install Open JDK 8:
    ```bash
    sudo apt-get update
    sudo apt-get install openjdk-8-jdk
    ```

2. Install latest Maven:
Installation process is described [here](https://maven.apache.org/install.html)

3. Download the ONLYOFFICE addon package for Nuxeo source code:
    ```bash
    git clone https://github.com/onlyoffice/onlyoffice-nuxeo.git
    ```

4. Compile ONLYOFFICE addon package for Nuxeo:
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
| Premium support | [Contact us](mailto:sales@onlyoffice.com) | [Contact us](mailto:sales@onlyoffice.com) |
| **Services** | **Community Edition** | **Enterprise Edition** |
| Conversion Service                | + | + |
| Document Builder Service          | + | + |
| **Interface** | **Community Edition** | **Enterprise Edition** |
| Tabbed interface                       | + | + |
| Dark theme                             | + | + |
| 125%, 150%, 175%, 200% scaling         | + | + |
| White Label                            | - | - |
| Integrated test example (node.js)      | + | + |
| Mobile web editors                     | - | +* |
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
| Adding Content control          | + | + | 
| Editing Content control         | + | + | 
| Layout tools                    | + | + |
| Table of contents               | + | + |
| Navigation panel                | + | + |
| Mail Merge                      | + | + |
| Comparing Documents             | + | + |
| **Spreadsheet Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Functions, formulas, equations  | + | + |
| Table templates                 | + | + |
| Pivot tables                    | + | + |
| Data validation           | + | + |
| Conditional formatting          | + | + |
| Sparklines                   | + | + |
| Sheet Views                     | + | + |
| **Presentation Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Transitions                     | + | + |
| Animations                      | + | + |
| Presenter mode                  | + | + |
| Notes                           | + | + |
| **Form creator features** | **Community Edition** | **Enterprise Edition** |
| Adding form fields           | + | + |
| Form preview                    | + | + |
| Saving as PDF                   | + | + |
| **Working with PDF**      | **Community Edition** | **Enterprise Edition** |
| Text annotations (highlight, underline, cross out) | + | + |
| Comments                        | + | + |
| Freehand drawings               | + | + |
| Form filling                    | + | + |
| | [Get it now](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo#docs-enterprise)  |

\* If supported by DMS.