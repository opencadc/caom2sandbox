# caom2sandbox
CAOM service deployment for the CADC data engineering sandbox system

You are free to fork this code and use it as a starting point for implementing services; the current
implementations are not really deployable and re-usable as they contain some hard-coded configuration.

We will be providing a Dockerfile and documentation to configure the container at runtime. Known issues
that make the images not re-usable have been captured and will be addressed in the near future

## sc2repo
This is an implementatuion of the CAOM metadata repository service. Requires: PostgreSQL server with citext
and pg_sphere extensions.

## sc2tap
This is an implementation of TAP-1.1 for CAOM. Requires: PostgreSQL server with citext
and pg_sphere extensions.

## sc2links
This is an implementation of DataLink-1.0 for CAOM. Requires: a CAOM TAP service.

## sc2meta
This is a CAOM metadata service. Requires: a CAOM TAP service.

## sc2soda
This is an implementation of SODA-1.0 for CAOM. Requires: a CAOM TAP service and a
storage back end that supports some cutout-related features.
