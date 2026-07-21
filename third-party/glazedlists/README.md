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

| Upstream Java source | Local Kotlin source | Intentional differences |
| --- | --- | --- |
| `TransactionList.java` | `TransactionList.kt` | Adds `withTransaction`; rollback-disabled construction is internal. |
| `UndoRedoSupport.java` | `UndoRedoSupport.kt` | Uses Kotlin collection and functional-interface idioms. |
| `impl/ObservableConnector.java` | `impl/ObservableConnector.kt` | Replaces deprecated `Observable`/`Observer` with a property-change contract. |
| `impl/beans/BeanConnector.java` | `impl/beans/BeanConnector.kt` | Uses Kotlin reflection-call and null-safety idioms. |
| `impl/filter/SearchTerm.java` | `impl/filter/SearchTerm.kt` | Uses a data class while excluding reusable scratch state from value equality. |
| `impl/gui/ThreadProxyEventList.java` | `impl/gui/ThreadProxyEventList.kt` | Adds idempotent disposal and protects queued event state. |
| `impl/swing/SwingThreadProxyEventList.java` | `impl/swing/SwingThreadProxyEventList.kt` | No intentional behavior change. |
| `matchers/ThreadedMatcherEditor.java` | `matchers/ThreadedMatcherEditor.kt` | Uses coroutines and virtual threads and is explicitly closeable. |
| `impl/functions/ConstantFunction.java` | `impl/functions/ConstantFunction.kt` | No intentional behavior change. |
| `impl/filter/StringLengthComparator.java` | `impl/filter/StringLengthComparator.kt` | No intentional behavior change. |
| `impl/sort/BooleanComparator.java` | `impl/sort/BooleanComparator.kt` | Preserves null-first ordering and class-based equality. |
| `impl/sort/ComparableComparator.java` | `impl/sort/ComparableComparator.kt` | Preserves null-first natural ordering and class-based equality. |
| `impl/sort/ReverseComparator.java` | `impl/sort/ReverseComparator.kt` | Rejects a null source comparator at construction. |
| `impl/sort/ComparatorChain.java` | `impl/sort/ComparatorChain.kt` | Preserves both constructors, defensive array copies, and content-based equality; no longer a Java record. |
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

See `LICENSE` in this directory for the upstream licensing terms.
