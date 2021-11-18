# ORCAP

ORCAP is an acronym for

**O**auth \
**R**estricted \
**C**ontent server \
**A**nd \
**P**roxy cache

It allows serving any files from disk and restricting acces to these files as an OAuth resource server. 
Using the `cache` profile it also acts as a proxy cache.

## Why not use Nginx or similar for serving files?
The point of ORCAP is to  allow downloads only for people providing a valid OAuth 2 / OIDC token with a predefined scope.
These features are not available in free versions of common web servers. Additional features from other file servers are not required.

Spring Webflux should allow sufficient performance as it also provides zero-copy file transfer.

## And other proxy cache solutions?
Again, the demand for OAuth protected resources does not work together with regular proxy caches.
Each proxy node needs enforce the same OAuth checks as the root node does.

## File path assumptions

* In production: We assume you run this app in a Docker container and mount all files for download in the `/content` directory.
* In development: All files are served from your project folder under `./content`

Other directories can be configured using environment variables. 