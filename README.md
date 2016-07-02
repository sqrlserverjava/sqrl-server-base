<!--- http://dillinger.io/ --->

[![Build Status](https://travis-ci.org/dbadia/sqrl-server-base.svg?branch=master)](https://travis-ci.org/dbadia/sqrl-server-base)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/204/badge)](https://bestpractices.coreinfrastructure.org/projects/204)
# sqrl-server-base

This java library that implements the server side portions of the [SQRL](https://www.grc.com/sqrl/sqrl.htm) protocol.  It can be integrated into existing JEE apps in order to add SQRL as an authentication option to existing methods (username/password, etc).

The intent is that additional libraries will be built on top of this for popular authentication frameworks such as Spring, Apache Shiro, etc.  It can also be used directly by an application as is.

## General


#### Interoperability
 * This library is fully functional for basic authentication.  Advanced operations such as SQRL identity replacement are not yet implemented
 * As of June 2016, the SQRL protocol has been declared ["done with caveats"](https://www.grc.com/sn/sn-562.txt) by it's creator.  SQRL clients built prior to this may still be using an older version of the protocol and may not be compatible
* There is a sample application using this library at the url below.  You <b>must</b> install a SQRL client before authenticating:
 https://sqrljava.tech:20000/sqrlexample

#### Design Goals
 * Make integration as easy as possible - There is only 
 * Secure - see [Security Considerations](#security-considerations) 

#### Dependencies
 * Java 1.8
 * [Zxing](https://github.com/zxing/zxing) for QR code generation
 * [slf4j](http://www.slf4j.org//) for logging
 * [ed25519-java](https://github.com/str4d/ed25519-java) for fast   Ed25519 EC crypto

#### Integration Requirements
The SQRL protocol requires 2 interaction points with the user: 1) the web browser and 2) the SQRL client.  Even if both of these are running on the same device, they are distinct processes which communicate to different server endpoints.

A persistence layer (typically a database) is required for the 2 endpoints to communicate state and to store various information about the SQRL users.  One of the SQRL database tables will include a foreign key reference to existing user data table.  Used SQRL nonces ("nuts") are stored in another table.  These nonces can be purged as soon as they expire.

#### Integration Overview
* Create a persistence class that implements `com.github.dbadia.sqrl.server.SqrlIdentityPersistance`
* Create a `com.github.dbadia.sqrl.server.SQRLConfig` bean and set the required fields accordingly (see javadoc).  This can be done via Spring, JAXB, etc.
* In your applciation code, you can now create a `com.github.dbadia.sqrl.server.SqrlServerOperations` object using the 2 objects above
* Create a servlet to handle SQRL client requests.  The `doPost` method of this servlet should invoke `SqrlServerOperations.handleSqrlClientRequest(ServletRequest, ServletResponse)`
* Login Page impact
	* Decide on one of the two approaches explained in [Browser to SQRL Client Correlation](#browser-to-sqrl-client-correlation) 
	* When you are ready to display the SQRL QR code, invoke `SqrlServerOperations.buildQrCodeForAuthPage()`.  Use the result to display and anchor tag with the SQRL url which wraps the QR code image.  The expected result in a QR code that can be scanned, clicked, or touched, as seen in  https://sqrljava.tech:20000/sqrlexample
	* Once the SQRL QR code is displayed, the authentication page must periodically poll the server (using ajay long polling, etc) to see if SQRL authentication is in progress or has completed.   Once SQRL authentication completes, the server will redirect the user to whatever page is typically displayed after autheticating

#### License
http://www.apache.org/licenses/LICENSE-2.0.html

## General Considerations

###### Authentication page impact
Traditionally, displaying an authentication page is an inexpensive operation.  Display some static html, username/password fields and some images from a CDN.  

However to display the SQRL QR code, it is required the user have a JEE session created and a unique QR code be generated.  Once generated, the authentication page must poll the server to understand when a SQRL authentication is in progress and complete.  This polling is required once the SQRL QR code is displayed, even it the user is using a non-SQRL authentication method.  The polling is required to have the page auto-refresh when the SQRL authentication is complete.

Both of these requirements result in a more process/resource intesive authentication page.  Here are 2 approaches that will  minimize such impact:
* Do not display the SQRL QR code on the login page; instead, display the [SQRL logo](https://www.grc.com/sqrl/logo.htm)  or some other indicator of SQRL support.  When/if the user clicks/taps the logo, generate the SQRL QR code and display
* Host the SQRL server logic on it's own infrastructure and load the SQRL QR code in the background as the page is loaded.  This isolates the more intensive SQRL support logic. 

###### Browser to SQRL Client Correlation
SQRL has 2 interaction points with the user: 1) the web browser and 2) the SQRL client.  Even if both of these are running on the same device, they are distinct processes which communicate to different server endpoints.
 
The browser requests the authentication page, then polls for status updates.  The SQRL QR code is then be clicked or scanned by the user which opens the SQRL client.  The SQRL client then makes a SQRL HTTP request to the SQRL backchannel which maps to a different server URI endpoint.  

There is a correlation ID embedded in the SQRL URL that allows the server to understand which user is not authenticated so the authentication page can be updated to allow the user into the application.  This correlation currently occurs via the persistence layer.  It is important to understand that the browser polling mechanism will result in additional load on the site and the persistence layer.


## Security Considerations
NOTE: This section applies to this library only,it is not a security assessment of the SQRL protocol in general


###### Nut Collisions
The SQRL protocol defines the size of SQRL token ("Nut").  Each authentication page should get a unique nut, but due to size limitations, this is not guaranteed in a mutli server environment.  To help avoid the likelihood of 2 clients receiving the same nut, additional entropy is added to the Secure Random instance with a UUID, which, by definition, should be unique to the JVM instance.

Assume that, by great chance, a SQRL Nut collision does occur with users foo and bar and assume that foo's SQRL client contacts the server first.  Foo will be authenticated and allowed in as usual.  When bar's SQRL client contacts the server, the SQRL library will reject the token as already used.  Bar should be shown a new authentication page with a new Nut.