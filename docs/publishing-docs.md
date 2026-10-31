# Documentation website

<p class="jpa-lead">The site is built with MkDocs Material from the Markdown in <code>docs/</code>, and published to GitHub Pages.</p>

## How the site is put together

| Piece | Purpose |
| --- | --- |
| `docs/*.md` | Page content. It also renders on GitHub. |
| `mkdocs.yml` | Navigation, theme features and Markdown extensions (admonitions, tabs, grid cards, Mermaid, keys). |
| `overrides/home.html` | The landing-page hero (`template: home.html` in `docs/README.md`). |
| `overrides/main.html` | The announcement bar shown on every page. |
| `docs/stylesheets/extra.css` | Brand styling: emerald accent, frosted header, cards, screenshots, steps and status pills. |
| `docs/assets/images/` | Screenshots of the sample app at 2× resolution, light and dark. |
| `scripts/docs_hooks.py` | Rewrites links that point outside `docs/` to GitHub source links. |

Useful building blocks for pages:

- `<p class="jpa-lead">…</p>` under the title for an introduction.
- `{ .jpa-screenshot }` on an image for a framed screenshot.
- `<div class="grid cards" markdown>` for feature cards with `:material-…:` icons.
- `<div class="jpa-steps" markdown>` around `###` headings for numbered steps.
- `<span class="jpa-pill jpa-pill--yes|partial|no">` for status labels.
- `!!! tip`, `!!! warning`, `??? example` for callouts; `++cmd+k++` for keys; ` ```mermaid ` for diagrams.

## Refresh the screenshots

Take screenshots from a **fresh** sample app (in-memory data, a temporary upload folder) so no personal data ends up on the site. Use a 1280×800 window at 2× scale, capture light and dark, and keep the file names used by the pages.

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

1. Push the documentation, `overrides/`, `mkdocs.yml`, `requirements-docs.txt`, `scripts/docs_hooks.py`, and `.github/workflows/docs.yml` to the repository's `main` branch.
2. Open **Settings → Pages → Build and deployment** in the GitHub repository. Set **Source** to **GitHub Actions**. Repository administration permissions may be required.
3. Open **Actions → Documentation → Run workflow** and select `main`. This also retries publication if the initial push occurred before Pages was enabled.
4. Wait for the `build` and `deploy` jobs to succeed. Open the URL shown by the `github-pages` environment.

Expected URL: **https://substring-projects.github.io/jpa-admin/**. This address becomes available only after a successful deployment.

Documentation changes pushed to `main` trigger publication automatically. Pull requests build the site with strict validation but do not deploy. Manual runs from branches other than `main` only build the site. The workflow uses GitHub's built-in token; no personal access token is needed.

See [GitHub's custom Pages workflow documentation](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages) for repository and environment configuration.

## Maintain the site

- Add new pages to `nav` in `mkdocs.yml` and link them from related pages. Anchors (`page.md#section`) can't be used in `nav`.
- Keep relative links between guides. The build hook converts links outside `docs/` into GitHub repository links, preserving source-code references without copying application files into the website.
- Run the strict build before submitting changes. Resolve missing-page and invalid-link warnings.
- Update `site_url`, `repo_url`, navigation URLs, and the workflow branch if you rename or fork the repository. The source-link hook and edit links currently target `main`.
- Review dependency updates and rebuild before changing the pinned documentation tooling.

If deployment fails, check the Actions logs, confirm Pages uses GitHub Actions, and review the `github-pages` environment's branch restrictions. A successful local build does not establish that the public website is deployed.
