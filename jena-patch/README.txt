The RDF writers in this module are auto-injected into the "fcrepo-http-commons/RdfStreamStreamingOutput" framework
via Spring's annotation-based instantiation of RdfWriterHelper.

This module should be removed with the release of Fedora 4.8.0, at which point, standard RDF 1.1 output syntax
should be used.

The only other evidence of this patch that should also be removed is:
1. jena-patch module definition in top-level pom.xml
2. jena-patch dependency declaration in fcrepo-webapp/pom.xml

Most of the classes in this module are exact or near copies of Jena classes. The differences from the original
classes can be found by searching for comments prefixed with:
"// NOTE, Fedora change"