## How to contribute

#### **IDE Setup**
* **Eclipse:** It is helpful to enable the JPA facet when working in eclipse.  This performs extra validation o the JPA components.  After enabling it, you typically see the error "No persistence.xml file found in project".  To fix this:
   * Add datastore/derby as a **source directory** in the project settings.  Be sure to add the derby directory **not** *derby/META-INF*
   * Project menu ->  clean.

#### **Did you find a bug?**

* **Ensure the bug was not already reported** by searching on GitHub under [Issues](https://github.com/sqrlserverjava/sqrl-server-base/issues).

* If you're unable to find an open issue addressing the problem, [open a new one](https://github.com/sqrlserverjava/sqrl-server-base/issues/new). Be sure to include a **title and clear description**, as much relevant information as possible, and a **code sample** or an **executable test case** demonstrating the expected behavior that is not occurring.


#### **Did you write a patch that fixes a bug?**

* Open a new GitHub pull request with the patch.

* Ensure the PR description clearly describes the problem and solution. Include the relevant issue number if applicable.



Thanks!
