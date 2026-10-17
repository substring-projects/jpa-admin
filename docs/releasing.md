# Release and documentation publication

[Documentation index](README.md)

This is a maintainer procedure, not a record of a published release. The current source version is `1.0.0-SNAPSHOT`.

## Prepare a release

1. Select the release commit and resolve outstanding changes. Update the parent version and module parent references together when changing the Maven version.
2. Review the feature status, configuration, API reference, and compatibility statements against that commit. Record significant changes and migration requirements in release notes.
3. Run the checks below from a clean checkout with the documented toolchain.
4. Start the packaged sample. Verify login/logout, record browsing, create/update, relations, and intended custom actions on disposable data. Test the starter in a separate consumer application.
5. Review the packaged UI assets. The JitPack configuration skips the npm build and packages committed `dist` files; regenerate and include them when frontend source changes.
6. Have the project owner choose and add a project license and establish a private vulnerability-reporting contact. Do not imply a license or security-support commitment that has not been adopted.
7. Publish the selected release using the project's chosen distribution process. Verify the actual artifact coordinates from a clean consumer environment before adding installation instructions or a release badge.

```bash
npm ci --prefix jpa-admin-ui
npm test --prefix jpa-admin-ui
./mvnw clean verify
```

The Maven lifecycle builds the frontend but does not invoke `npm test`; both commands are required for these checks. The repository does not currently contain a release automation workflow.

## Publish documentation with the repository

The Markdown files are designed to render directly on GitHub. Include `README.md`, `CONTRIBUTING.md`, and the complete `docs/` directory in the same reviewed commit. Relative links preserve navigation in repository views and release tags.

Review the diff, commit the documentation on your chosen branch, and push through your normal repository workflow. No separate documentation server is required. Link users to the documentation index at the relevant release tag so snapshot changes do not silently alter their instructions.

The repository includes a MkDocs Material website and GitHub Pages workflow. Follow [documentation website setup](publishing-docs.md) to enable Pages and publish it.

## Release evidence

Record the release tag or commit, Java/Node/Maven versions, test results, packaged sample smoke-test outcome, consumer dependency-resolution result, and exact distribution coordinates in the release notes or release review. Mark unverified combinations explicitly.
