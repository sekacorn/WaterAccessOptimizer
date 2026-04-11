# Contributing to WaterAccessOptimizer

Thanks for contributing. The most helpful changes are the ones that improve the current release surface and keep the documentation honest.

## Start Here

Read these files before making changes:

- [README.md](./README.md)
- [GETTING_STARTED.md](./GETTING_STARTED.md)
- [DEPLOYMENT.md](./DEPLOYMENT.md)
- [docs/ARCHITECTURE_OVERVIEW.md](./docs/ARCHITECTURE_OVERVIEW.md)

## Good Contribution Areas

- frontend bug fixes and usability improvements
- accessibility improvements
- documentation corrections
- tests for current routed features
- backend hardening for the active auth and data-service paths

## Issues

When reporting a bug, include:

- steps to reproduce
- expected and actual behavior
- screenshots or logs if relevant
- environment details
- whether you used demo mode or the Docker stack

## Pull Requests

Keep pull requests focused and release-friendly:

- explain the user-facing problem
- list the verification you ran
- update docs when behavior, ports, paths, or setup steps change
- avoid presenting planned or legacy features as shipped functionality

## Verification

For frontend work, run:

```bash
cd frontend
npm install
npm run lint
npm run test -- --run
npm run build
```

If you change deployment, security, or architecture behavior, update the corresponding Markdown files in the repo root or `docs/`.

## Style

- prefer small, reviewable changes
- keep product claims accurate
- document limitations and assumptions
- preserve the current release path unless you are intentionally updating it

## License

By contributing, you agree that your contributions are provided under the repository license.
