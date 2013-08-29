# Application Base

Reusable foundation for java RDF applications that provide a web UI or web API or both.
Not a replacement to Ruby or client side JavaScript for serious UIs but suited 
to applications with management UIs (e.g. data converters) or with straightforward
requirements (e.g. UKGovLD registry).

Based on approach and code used for Modal, Pearson, SE, Registry, BG.

## Core functions

   * **Configuration.** Support for configurating a set of "components" that make up and application. Supports parameter setting and linking components (c.f. Spring et al).
   * **Data access.**   Provides simple abstraction for accessing RDF data sources to ease switching between memory, TDB and remote sources. Mostly there to support ...
   * **Data wrapper.**  Wrappers to simplify access to RDF data from UI scripts. Supports cached access to labels/descriptions of resources for rendering.
   * **Template UI.**   Easy velocity based rendering. 

### TBD

   * Data (cube) API
   * Support for generic JSON APIs

Full documentation: https://epimorphics.codebasehq.com/projects/data-fabric/notebook/AppBase/index