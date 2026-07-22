# Glazed Lists

The Glazed Lists core source in `src/main/java/ca/odell/glazedlists`, Kotlin
conversions in `src/main/kotlin/ca/odell/glazedlists`, and resources in
`src/main/resources/resources` are vendored from the upstream Glazed Lists
repository:

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

The obsolete Java 1.4 lock backport (`Lock`, `ReadWriteLock`, `LockFactory`, and
`J2SE50LockFactory`) is replaced by `java.util.concurrent.locks` types. The
default remains a non-fair `ReentrantReadWriteLock`.

Java object serialization support is intentionally omitted from the vendored
Glazed Lists types because MediathekView does not serialize those objects.

Upstream's `impl.testing` package is omitted from production sources. Its useful
event-consistency behaviors are covered by JUnit tests under `src/test`;
obsolete timing helpers and trivial test-data factories are not included.

## Kotlin conversions

Converted sources retain their upstream package and Java-facing contract unless
an intentional difference is documented below.

| Upstream Java source | Local source | Intentional differences |
| --- | --- | --- |
| `TransactionList.java` | `TransactionList.kt` | Adds `withTransaction`; rollback-disabled construction is internal. |
| `UndoRedoSupport.java` | `UndoRedoSupport.kt` | Uses Kotlin collection and functional-interface idioms. |
| `impl/adt/barcode2/ListToByteCoder.java` | `impl/adt/barcode2/ListToByteCoder.kt` | Rejects duplicate colors, documents the seven-bit capacity, omits unused decoding methods, and keeps `colorAsIndex` Kotlin-internal with a public JVM static bridge for Java callers. |
| `impl/adt/IntArrayList.java` | — | Removed; primitive event-block storage is owned directly by `BlockSequence.kt`. |
| `impl/ObservableConnector.java` | `impl/ObservableConnector.kt` | Replaces deprecated `Observable`/`Observer` with a property-change contract. |
| `impl/event/BlockSequence.java` | `impl/event/BlockSequence.kt` | Uses reusable `IntArray` storage while preserving block ordering, concatenation, iteration, and Java-facing contracts. |
| `impl/event/Tree4Deltas.java` | `impl/event/Tree4Deltas.kt` | Stores old and new values explicitly, fixes range-value isolation and contradictory insert updates, and removes the unused source-side API. |
| `event/Tree4DeltasListEvent.java` | `event/Tree4DeltasListEvent.java` | Retains the package-private Java boundary while publishing retained new values and implementing remaining-block counts for both event representations. |
| `impl/beans/BeanConnector.java` | `impl/beans/BeanConnector.kt` | Uses Kotlin reflection-call and null-safety idioms. |
| `gui/AbstractTableComparatorChooser.java` | `gui/AbstractTableComparatorChooser.kt` | Preserves subclass extension points, Java-record sort keys, immutable sort-key snapshots, and null-validation messages. |
| `gui/AdvancedTableFormat.java` | `gui/AdvancedTableFormat.kt` | Makes the nullable column comparator explicit; no intentional behavior change. |
| `gui/CheckableTableFormat.java` | `gui/CheckableTableFormat.kt` | No intentional behavior change. |
| `gui/TableFormat.java` | `gui/TableFormat.kt` | Makes nullable cell values explicit; no intentional behavior change. |
| `gui/WritableTableFormat.java` | `gui/WritableTableFormat.kt` | No intentional behavior change. |
| `impl/gui/MouseOnlySortingStrategy.java` | `impl/gui/MouseOnlySortingStrategy.kt` | Preserves single- and multiple-column click behavior. |
| `impl/filter/SearchTerm.java` | `impl/filter/SearchTerm.kt` | Uses a data class while excluding reusable scratch state from value equality. |
| `impl/gui/ThreadProxyEventList.java` | `impl/gui/ThreadProxyEventList.kt` | Adds idempotent disposal and protects queued event state. |
| `impl/swing/SwingThreadProxyEventList.java` | `impl/swing/SwingThreadProxyEventList.kt` | No intentional behavior change. |
| `matchers/ThreadedMatcherEditor.java` | `matchers/ThreadedMatcherEditor.kt` | Uses coroutines and virtual threads and is explicitly closeable. |
| `impl/functions/ConstantFunction.java` | `impl/functions/ConstantFunction.kt` | No intentional behavior change. |
| `impl/filter/StringLengthComparator.java` | `impl/filter/StringLengthComparator.kt` | No intentional behavior change. |
| `impl/filter/StringTextFilterator.java` | `impl/filter/StringTextFilterator.kt` | Makes nullable element handling explicit; no intentional behavior change. |
| `impl/filter/BoyerMooreCaseInsensitiveTextSearchStrategy.java` | `impl/filter/BoyerMooreCaseInsensitiveTextSearchStrategy.kt` | Preserves locale-independent Unicode case folding, UTF-16 result indices, and shortened shift-table behavior. |
| `impl/filter/StartsWithCaseInsensitiveTextSearchStrategy.java` | `impl/filter/StartsWithCaseInsensitiveTextSearchStrategy.kt` | Preserves specialized single- and multi-character prefix matching and default-locale case conversion. |
| `impl/filter/ExactCaseInsensitiveTextSearchStrategy.java` | `impl/filter/ExactCaseInsensitiveTextSearchStrategy.kt` | Preserves the open inheritance and exact-length matching contracts. |
| `impl/sort/BooleanComparator.java` | `impl/sort/BooleanComparator.kt` | Preserves null-first ordering and class-based equality. |
| `impl/sort/ComparableComparator.java` | `impl/sort/ComparableComparator.kt` | Preserves null-first natural ordering and class-based equality. |
| `impl/sort/ReverseComparator.java` | `impl/sort/ReverseComparator.kt` | Rejects a null source comparator at construction. |
| `impl/sort/ComparatorChain.java` | `impl/sort/ComparatorChain.kt` | Preserves both constructors, defensive array copies, and content-based equality; no longer a Java record. |
| `impl/sort/TableColumnComparator.java` | `impl/sort/TableColumnComparator.kt` | Preserves both constructors, exact-class equality, and helpful comparison failures. |
| `impl/matchers/TrueMatcher.java` | `impl/matchers/TrueMatcher.kt` | Preserves the generic singleton factory. |
| `impl/matchers/FalseMatcher.java` | `impl/matchers/FalseMatcher.kt` | Preserves the generic singleton factory. |
| `impl/matchers/NullMatcher.java` | `impl/matchers/NullMatcher.kt` | Preserves the generic singleton factory and string representation. |
| `impl/matchers/NotNullMatcher.java` | `impl/matchers/NotNullMatcher.kt` | Preserves the generic singleton factory and string representation. |
| `impl/matchers/NonNullAndNonEmptyStringMatcher.java` | `impl/matchers/NonNullAndNonEmptyStringMatcher.kt` | Makes the accepted nullable input explicit. |
| `impl/matchers/NotMatcher.java` | `impl/matchers/NotMatcher.kt` | Preserves null-parent validation and string representation. |
| `impl/matchers/TypeMatcher.java` | `impl/matchers/TypeMatcher.kt` | No intentional behavior change. |
| `impl/matchers/AndMatcher.java` | `impl/matchers/AndMatcher.kt` | Preserves contravariant matcher varargs. |
| `impl/matchers/OrMatcher.java` | `impl/matchers/OrMatcher.kt` | Preserves contravariant matcher varargs. |
| `impl/matchers/BeanPropertyMatcher.java` | `impl/matchers/BeanPropertyMatcher.kt` | Makes nullable bean input and expected property values explicit. |
| `impl/matchers/RangeMatcher.java` | `impl/matchers/RangeMatcher.kt` | Preserves inclusive and unbounded ranges, including null comparable behavior. |
| `impl/sort/BeanPropertyComparator.java` | `impl/sort/BeanPropertyComparator.kt` | Makes nullable bean comparison explicit. |
| `impl/filter/AbstractTextSearchStrategy.java` | `impl/filter/AbstractTextSearchStrategy.kt` | Preserves optional character mapping and subclass extension points. |
| `impl/filter/RegularExpressionTextSearchStrategy.java` | `impl/filter/RegularExpressionTextSearchStrategy.kt` | Preserves full-input regular-expression matching and initialization behavior. |
| `impl/filter/SingleCharacterCaseInsensitiveTextSearchStrategy.java` | `impl/filter/SingleCharacterCaseInsensitiveTextSearchStrategy.kt` | Preserves single-character validation, character mapping, and case-insensitive search. |
| `impl/filter/TextMatcher.java` | `impl/filter/TextMatcher.kt` | Preserves normalized search terms, strategy selection, and matcher equality. |
| `impl/filter/TextMatchers.java` | `impl/filter/TextMatchers.kt` | Preserves parsing, normalization, matching, and constraint/relaxation classification. |
| `impl/filter/TextSearchStrategy.java` | `impl/filter/TextSearchStrategy.kt` | Preserves the search-strategy and factory functional-interface contracts. |
| `impl/matchers/PropertyEventNameMatcher.java` | `impl/matchers/PropertyEventNameMatcher.kt` | Preserves vararg and collection construction and include/exclude semantics. |
| `impl/matchers/WeakReferenceMatcherEditor.java` | `impl/matchers/WeakReferenceMatcherEditor.kt` | Preserves weak listener registration, cleanup, and event rebroadcasting. |
| `impl/text/LatinDiacriticsStripper.java` | `impl/text/LatinDiacriticsStripper.kt` | Generates the Latin mapping table once with canonical Unicode decomposition and returns defensive snapshots. |
| `matchers/AbstractMatcherEditor.java` | `matchers/AbstractMatcherEditor.kt` | Preserves matcher state transitions and match-all/match-none identity checks. |
| `matchers/AbstractMatcherEditorListenerSupport.java` | `matchers/AbstractMatcherEditorListenerSupport.kt` | Preserves consistently sourced events and LIFO delivery to a stable listener snapshot. |
| `matchers/CompositeMatcherEditor.java` | `matchers/CompositeMatcherEditor.kt` | Preserves AND/OR composition and matcher-event relationship classification. |
| `matchers/FixedMatcherEditor.java` | `matchers/FixedMatcherEditor.kt` | Preserves the package-internal immutable matcher-editor implementation. |
| `matchers/Matcher.java` | `matchers/Matcher.kt` | Uses a Kotlin functional interface while preserving the `Predicate` bridge. |
| `matchers/MatcherEditor.java` | `matchers/MatcherEditor.kt` | Preserves listener, event, and fixed-editor factory contracts. |
| `matchers/Matchers.java` | `matchers/Matchers.kt` | Preserves the Java-static matcher factory and collection utility facade. |
| `matchers/RangeMatcherEditor.java` | `matchers/RangeMatcherEditor.kt` | Preserves inclusive, unbounded, and normalized ranges and event classification. |
| `matchers/SearchEngineTextMatcherEditor.java` | `matchers/SearchEngineTextMatcherEditor.kt` | Preserves search parsing, named fields, defensive field copies, and the Java-record field contract. |
| `matchers/SetMatcherEditor.java` | `matchers/SetMatcherEditor.kt` | Preserves blacklist/whitelist modes, defensive match-set copies, and event classification. |
| `matchers/TextMatcherEditor.java` | `matchers/TextMatcherEditor.kt` | Preserves matching modes, strategy singletons, and matcher-event classification. |
| `matchers/ThresholdMatcherEditor.java` | `matchers/ThresholdMatcherEditor.kt` | Preserves comparison operations, nullable thresholds, comparator fallback, and event classification. |

See `LICENSE` in this directory for the upstream licensing terms.
