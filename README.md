# Application Base

Reusable foundation for java RDF applications that provide a web UI or web API or both.
Not a replacement to Ruby or client side JavaScript for serious UIs but suited 
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
