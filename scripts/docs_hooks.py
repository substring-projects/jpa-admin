"""Keep repository-relative source links usable in the published docs.

Markdown remains directly browsable on GitHub. Only links escaping docs/ are
rewritten at build time; documentation links are left for MkDocs to resolve.
"""
import re


def on_page_markdown(markdown, page, config, files):
    repository = config["repo_url"].rstrip("/")
    return re.sub(
        r"\]\(\.\./([^)]+)\)",
        lambda match: f"]({repository}/tree/main/{match.group(1)})",
        markdown,
    )
