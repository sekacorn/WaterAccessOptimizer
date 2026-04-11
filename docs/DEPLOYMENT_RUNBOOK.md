# Deployment Runbook

This runbook has been simplified so it does not overstate unverified infrastructure.

## Current Release Path

Use [../DEPLOYMENT.md](../DEPLOYMENT.md) as the deployment source of truth.

## Operational Guidance

- Docker Compose is the primary documented release path.
- Validate health endpoints after startup.
- Confirm secrets are set in `.env` before launch.
- Treat Kubernetes and advanced rollout strategies as separate validation work unless you have tested them in your own environment.
