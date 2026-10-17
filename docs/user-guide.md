# User guide

[Documentation index](README.md) · [Project overview](../README.md)

Applies to `1.0.0-SNAPSHOT` (Spring Boot 4.0.0).

- **Navigation:** Search entities by name or group. Use the footer control to collapse the desktop sidebar, or the menu button to open mobile navigation.
- **Tables:** Search text, click sortable headers, apply filters, and select a page size. Wide tables scroll horizontally on small screens.
- **Selection:** Select rows for batch deletion or applicable custom actions. Destructive operations use confirmation dialogs.
- **Create/edit:** Forms are generated from metadata. Required fields, email inputs, numeric limits, and text lengths use browser validation where supported.
- **Relations:** Search a related entity, then choose a result. Existing selections remain visible even when outside the current lookup results.
- **Sensitive fields:** Leave a sensitive field blank during editing to retain its current value. The frontend omits unchanged sensitive values from updates.
- **Related collections:** Inline rows are read-only. Manage child records through their own entity lists.
- **Feedback:** Skeletons appear during loading. Save and action controls indicate pending requests; notifications report success or failure.
- **Theme/account:** Toggle light/dark mode in the header and sign out from the sidebar footer.

The dashboard currently summarizes discovered entities and groups, not database record totals or an activity feed.
