# Vocabulary Service

Provides webservices for the ALA vocabulary service.
It works in conjunction with the [vocabulary-plugin](../vocabulary-plugin), which can be added to
applications to provide semantic information via this service, 
and the [vocabulary-server-plugin](../vocabulary-server-plugin), which can be used to display and link 
vocabulary elements.

## Installation

The vocabulary service is backed by an [RDF4J](http://rdf4j.org/) server, version 2.4.0 at present.
To use the server:

* Install Tomcat (or the servlet container of your choice).
  * If you are deploying the vocabulary service as a WAR, you can deploy it to the same tomcat server.
* Install `rdf4j-server.war`
* (Optionally) Install `rdf4j-workbench.war` 
  This will make your life considerably easier, since youcan use it for the next steps.
  If you don't want to, you can use the RDF4J CLI.
* Create a repository with an id of `ala` at http://$server/rdf4j-workbench/repositories/NONE/create
  This repository needs the following characteristics: *RDFS Inferencing* and *Lucene*.
  There isn't a pre-built configiuration for this in the workbench, so simply create a repoistory and then:
  * Stop the server
  * Copy this [config.ttl](data/config.ttl) to `$RDF4J/Server/repositories/ala/config.ttl` where $RDF4J
    is the data directory for RDF4J, eg. `/Library/Application Support/RDF4J` on OS X.
  * Start the server.
    As long as you do this *before* importing any triples into the repository, the inferencer will do its magic.
* Import the vocabularies into the repository at http://$server/rdf4j-workbench/repositories/ala/add
  * Import `format.ttl` and `ala.ttl` in that order before importing any other vocabulary elements.
    These contain the ontologies and additional triples needed to make the vocabulary service
    provide proper inferencing and formatting information.
  * After that, import vocabularies as you see fit.
    There is a pre-built vocabularies.ttl for common ALA terms.
    You probably also want to import the iso-639.ttl and dwc.ttl vocabularies for languages and
    Darwin Core terms. See the [administration](#adminstration) section for how to (re-)generate these.
* Add the vocabulary-service WAR to tomcat.

## Configuration

The configuration comes with sensible defaults for a co-located service and RDF4J server.
If you want to change configuration, use:

* **security.*** Standard ALA [CAS](https://github.com/AtlasOfLivingAustralia/ala-auth-plugin) security configuration
* **skin.*** Standard ALA [bootstrap](https://github.com/AtlasOfLivingAustralia/ala-bootstrap3) skin configuration
* **repository.servicd** The root of the RDF4J server. Defaults to `http://localhost:8080/rdf4j-server/`
* **repository.id** The repository identifier. Defaults to `ala`
* **cacheManager.config** The URL for the [JCS](https://commons.apache.org/proper/commons-jcs/) cache manager confiuration.
  The cache manager caches resource descriptions once retrieved from the triple store.
  Defaults to the following [cache.ccf](src/main/resources/cache.ccf) which caches 20000 items by default.
* **format.defaultCategory** The IRI of the default category when [categorising](#categorisation)
  Defaults to `http://www.ala.org.au/format/1.0/categories/other`
* **tags.cacheFor** The time in seconds to cache the [tag](#tags) CSS and Javascript. Defaults to 3600.
* **import.languages.default** A comma-separated list ISO 639-3 codes that should be explicitly imported and tagged.
* **import.biocache.index.fields** The URL of the biocache service that will be queried for additional DwC vocabulary fields.
  Defaults to `https://biocache.ala.org.au/ws/index/fields`

## Services

Services are documented by [openapi.yml](src/main/resources/public/openapi.yml).
In general, these services return mildly-enhanced [JSON-LD](https://json-ld.org/) with
and additional `@count` attribute for the total size of paged queries and
a `@categorisation` described below.

### Categorisation

A resource or list of resources is also returned with a `@categorisation` element.
This contrains an object with keys of category IRIs, ordered by priority, each with lists of
predicates, ordered by display order.
When displaying a resource, you can work through the categorisation to provide a consistent
rendering of the resource.

Categorisation comes from the `format:` ontology `http://www.ala.org.au/format/1.0/`
The `format:category` predicate annotates a predicate with a `format:Category` for grouping.
Categories are ordered by `format:priority`.
A predicate can also be ordered in a category by the `format:comesBefore` and `format:comesAfter`
predicates, which provide a partial order for ordering predicates within categories.
(The partial order avoids having to have some sort of global priority order for predicates.)

### Tags

Resources with a type of `format:Concept`  can be displayed as *tags*, basically small
bits of visual CSS or icons.
The vocabulary plugin has a `<voc:tag .../>` GSP tag tht can be used to mark a vocabulary element.

The tag system works by generating large, single CSS and Javascript documents that can be used to
fill out place-holder tags on HTML pages.
These documents can be locally cached on a browser and reused between pages.
This may or may not be a good idea.
    
## Adminstration

The administration page provides the following services:

* **Clear caches**, to clear the service caches if the triple store has been updated.
* **Import and translate ISO 639-3 codes** Generates a language vocabulary from the ISO 639-3 defintions.
  This is almost 8000 obscure languages, so by default a limited number are used.
  Languages that have two letter codes automatically have a resource generated and marked as a
  concept, as do a specific list of other langiages.
* **Import and translate Darwin Core (DwC) terms** Generates a vocabulaty from the DwC term defintions
  and from the fields listed in the biocache.
  The DwC term defintions contain a complete history of term version, which are not included by default.
  