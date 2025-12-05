# Application Base

Reusable foundation for java RDF applications that provide a web UI or web API or both. Not a replacement to Ruby or client side JavaScript for serious UIs but suited to applications with management UIs (e.g. data converters) or with straightforward requirements (e.g. UKGovLD registry).

## Core functions

   * **Configuration.** Support for configuring a set of "components" that make up an application. Supports parameter setting and linking components (c.f. Spring et al).
   * **Data access.**   Provides simple abstraction for accessing RDF data sources to ease switching between memory, TDB and remote sources. Mostly there to support ...
   * **Data wrapper.**  Wrappers to simplify access to RDF data from UI scripts. Supports cached access to labels/descriptions of resources for rendering.
   * **Template UI.**   Easy velocity based rendering. 
   * **Actions.**       Configurable asynchronous tasks

## Changelog

**4.0.0**
   * Appbase is compiled by JDK 21 and is compatible only with Java versions 21 and above.
   * This version upgrades the following dependencies with major impact.
     This may cause backwards compatibility issues for applications using Appbase:
     * Tomcat 9.0 -> 11.0
     * Jena 3.9 -> 5.6
     * Lib 3.1 -> 4.0
     * Jersey 2.25 -> 3.1
   * Replaced all usages of javax servlet infrastructure with jakarta.
   * Maven artifacts for Appbase version 4.0.0 and onwards are published to the Epimorphics public S3 Maven repository.
   * The `DatasetAccessor` interface from Jena 3 is replaced by `com.epimorphics.appbase.data.DatasetAccessor`:
     * It is directly implemented by `RDFConnectionDatasetAccessor` and indirectly by various wrapper classes which are unchanged from the previous version.
     * It has removed the `containsModel` method since no Jena 5 equivalent exists. Instead, use `getModel` to obtain the model, then `isEmpty` on the model.

**3.1.12**
   * Fix log level in output of RunShell to be ERROR level if process returns failed status

**3.1.7**
   * Add support for customizing the tomcat context in tomcat tests, useful to disable noisy jar scanning

**3.1.6**
   * Update lib to 3.1.7 (updates jackson version to mitigate CV£)
   * Updated Apache Tomcat to 9.0.107 - supported version

**3.1.5**
   * Updated commons-lang to commons-text
   * Updated Apache Tomcat from 7.x to 8.5
   * Updated Apache Velocity from 1.17.0 to 2.4.1. Downstream users may need to set the backward compatibility flags described in the [Velocity upgrade notes](https://velocity.apache.org/engine/2.4.1/upgrading.html#upgrading-from-velocity-17-to-velocity-20).
