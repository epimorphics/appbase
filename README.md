# Application Base

Reusable foundation for java RDF applications that provide a web UI or web API or both. Not a replacement to Ruby or client side JavaScript for serious UIs but suited to applications with management UIs (e.g. data converters) or with straightforward requirements (e.g. UKGovLD registry).

## Core functions

   * **Configuration.** Support for configuring a set of "components" that make up an application. Supports parameter setting and linking components (c.f. Spring et al).
   * **Data access.**   Provides simple abstraction for accessing RDF data sources to ease switching between memory, TDB and remote sources. Mostly there to support ...
   * **Data wrapper.**  Wrappers to simplify access to RDF data from UI scripts. Supports cached access to labels/descriptions of resources for rendering.
   * **Template UI.**   Easy velocity based rendering. 
   * **Actions.**       Configurable asynchronous tasks

