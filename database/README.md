# Database Documentation

This directory contains historical schema material plus PostgreSQL artifacts used by the project during different phases of development.

## Current Release Guidance

For the release currently documented in this repository:

- PostgreSQL is the primary persistent store.
- The active runtime paths are described in [../README.md](../README.md) and [../docs/ARCHITECTURE_OVERVIEW.md](../docs/ARCHITECTURE_OVERVIEW.md).
- Docker Compose is the primary documented deployment path.

## Important Note

Some schema and migration files in this directory reflect older or broader platform ideas that are not the primary source of truth for the current packaged release. Treat them as reference material unless they are explicitly linked from current deployment documentation.
