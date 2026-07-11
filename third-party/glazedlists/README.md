# Glazed Lists

The Glazed Lists core source in `src/main/java/ca/odell/glazedlists` and its
resources in `src/main/resources/resources` are vendored from the upstream
Glazed Lists repository:

- Repository: https://github.com/glazedlists/glazedlists
- Revision: `9ace85251cc0f8e70eebc9dfdb2cb2a920cb0275`
- Revision date: 2025-05-11
- Upstream source path: `core/src/main`

Only the self-contained core module is included. Optional extension modules,
including all JavaFX, SWT, and Hibernate components, are intentionally excluded
because they are not used by MediathekView.

The following unused deprecated compatibility classes are also omitted in favor
of their retained replacements:

- `EventListModel` (`DefaultEventListModel`)
- `EventComboBoxModel` (`DefaultEventComboBoxModel`)
- `EventSelectionModel` (`DefaultEventSelectionModel`)
- `EventTableModel` (`DefaultEventTableModel`)
- `ca.odell.glazedlists.impl.SerializedReadWriteLock`
  (`ca.odell.glazedlists.util.concurrent.SerializedReadWriteLock`)

See `LICENSE` in this directory for the upstream licensing terms.
