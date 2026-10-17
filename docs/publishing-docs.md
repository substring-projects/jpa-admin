# Documentation website

[Documentation index](README.md)

The documentation website uses MkDocs Material. Markdown in `docs/` remains the source of truth. Search, sidebar navigation, code copying, and light/dark themes are included.

## Preview locally

Use Python 3.12 (the CI version) or a compatible newer Python installation:

```bash
python3 -m venv .venv-docs
source .venv-docs/bin/activate
python -m pip install -r requirements-docs.txt
python -m mkdocs serve
```

On Windows, create the environment with `py -m venv .venv-docs` and activate it in PowerShell with `.venv-docs\Scripts\Activate.ps1`.

Open **http://127.0.0.1:8000/jpa-admin/**. Changes to documentation reload automatically. Stop the preview with Ctrl+C.

To validate and generate the static website:

```bash
python -m mkdocs build --strict
```

The output is `site/`, which is ignored by Git. No Java build or database is needed.

## Enable GitHub Pages

1. Push the documentation, `mkdocs.yml`, `requirements-docs.txt`, `scripts/docs_hooks.py`, and `.github/workflows/docs.yml` to the repository's `main` branch.
2. Open **Settings → Pages → Build and deployment** in the GitHub repository. Set **Source** to **GitHub Actions**. Repository administration permissions may be required.
3. Open **Actions → Documentation → Run workflow** and select `main`. This also retries publication if the initial push occurred before Pages was enabled.
4. Wait for the `build` and `deploy` jobs to succeed. Open the URL shown by the `github-pages` environment.

Expected URL: **https://substring-projects.github.io/jpa-admin/**. This address becomes available only after a successful deployment.

Documentation changes pushed to `main` trigger publication automatically. Pull requests build the site with strict validation but do not deploy. Manual runs from branches other than `main` only build the site. The workflow uses GitHub's built-in token; no personal access token is needed.

See [GitHub's custom Pages workflow documentation](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages) for repository and environment configuration.

## Maintain the site

- Add new pages to `nav` in `mkdocs.yml` and link them from the documentation index.
- Keep relative links between guides. The build hook converts links outside `docs/` into GitHub repository links, preserving source-code references without copying application files into the website.
- Run the strict build before submitting changes. Resolve missing-page and invalid-link warnings.
- Update `site_url`, `repo_url`, navigation URLs, and the workflow branch if you rename or fork the repository. The source-link hook and edit links currently target `main`.
- Review dependency updates and rebuild before changing the pinned documentation tooling.

If deployment fails, check the Actions logs, confirm Pages uses GitHub Actions, and review the `github-pages` environment's branch restrictions. A successful local build does not establish that the public website is deployed.
