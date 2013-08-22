# Application Base

Reusable foundation for java-based RDF applications that include a basic UI element. 
Not a replacement to Ruby or client side applications for serious UIs but suited 
to applications with management UIs (e.g. data converters) or with straightforward
requirements (e.g. UKGovLD registry).

Based on approach and code used for Modal, Pearson, SE, Registry, BG.

## Core functions

### Configuration

Configure application as a set of components.

Simple, flexible configuration specification (poor man's Spring).

### Data access abstraction

Data access based entirely on SPARQL (with convenience wrappers) so can use remote (e.g. Fuseki)
endpoints or local data transparently. Support for caching of remote resources.

### Web UI

Easy configuration of a velocity-based rendering.

Convenience wrappers for data sources and local data cases for usability from velocity.

### TBD

Data access API support?

JSON API support?
