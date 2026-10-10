package util;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * [Collections: Generics & Generic Utility]
 * Reusable type-safe utility class demonstrating advanced Java Generics:
 * <ul>
 *     <li>Generic methods with type parameters &lt;T&gt;, &lt;K&gt;, &lt;V&gt;</li>
 *     <li>Bounded wildcards following the PECS rule: Producer Extends, Consumer Super</li>
 *     <li>Common collection algorithms: filtering, sorting, searching, grouping, indexing</li>
 * </ul>
 */
public final class GenericCollectionUtils {

    private GenericCollectionUtils() {
        // Private constructor to prevent instantiation of utility class
    }

    /**
     * [Collections: Filtering & Generics]
     * Filters an input collection based on a generic predicate test.
     * Uses bounded wildcard {@code Predicate<? super T>} to accept predicates
     * capable of evaluating {@code T} or any superclass of {@code T}.
     *
     * @param collection source collection
     * @param predicate  filtering criteria
     * @param <T>        element type
     * @return filtered List of elements matching the predicate
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<? super T> predicate) {
        if (collection == null || collection.isEmpty() || predicate == null) {
            return Collections.emptyList();
        }
        return collection.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * [Collections: Sorting & Generics]
     * Sorts a collection into a new List using the specified generic comparator.
     * Uses {@code Comparator<? super T>} so a comparator defined for a supertype
     * (e.g. comparing Users) can sort subtypes (e.g. TeamMembers).
     *
     * @param collection source collection
     * @param comparator comparison strategy
     * @param <T>        element type
     * @return new sorted List preserving non-destructive semantics
     */
    public static <T> List<T> sort(Collection<T> collection, Comparator<? super T> comparator) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyList();
        }
        if (comparator == null) {
            return new ArrayList<>(collection);
        }
        return collection.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * [Collections: Searching & Generics]
     * Performs case-insensitive substring search over a collection using an extractor function.
     *
     * @param collection    source collection
     * @param textExtractor function extracting string attribute from element
     * @param query         search term
     * @param <T>           element type
     * @return list of elements whose extracted text contains the search query
     */
    public static <T> List<T> search(Collection<T> collection, Function<? super T, String> textExtractor, String query) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyList();
        }
        if (query == null || query.isBlank() || textExtractor == null) {
            return new ArrayList<>(collection);
        }
        String cleanQuery = query.trim().toLowerCase();
        return collection.stream()
                .filter(item -> {
                    if (item == null) return false;
                    String text = textExtractor.apply(item);
                    return text != null && text.toLowerCase().contains(cleanQuery);
                })
                .collect(Collectors.toList());
    }

    /**
     * [Collections: Map & Generics]
     * Groups elements into a Map where keys are produced by a classifier function.
     *
     * @param collection source collection
     * @param classifier function deriving the map key
     * @param <T>        element type
     * @param <K>        group key type
     * @return Map with key and list of matching elements
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<? super T, ? extends K> classifier) {
        if (collection == null || collection.isEmpty() || classifier == null) {
            return Collections.emptyMap();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * [Collections: Map & Generics]
     * Counts occurrences of grouped categories, producing a histogram Map&lt;K, Long&gt;.
     *
     * @param collection source collection
     * @param classifier function deriving category key
     * @param <T>        element type
     * @param <K>        category key type
     * @return Map with categories and occurrence count
     */
    public static <T, K> Map<K, Long> countBy(Collection<T> collection, Function<? super T, ? extends K> classifier) {
        if (collection == null || collection.isEmpty() || classifier == null) {
            return Collections.emptyMap();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.counting()));
    }

    /**
     * [Collections: Map & Generics]
     * Indexes a collection into a Map keyed by a unique attribute (e.g. ID).
     *
     * @param collection source collection
     * @param keyMapper  function extracting unique key
     * @param <T>        element type
     * @param <K>        key type
     * @return Map keyed by identifier
     */
    public static <T, K> Map<K, T> indexBy(Collection<T> collection, Function<? super T, ? extends K> keyMapper) {
        if (collection == null || collection.isEmpty() || keyMapper == null) {
            return Collections.emptyMap();
        }
        Map<K, T> map = new LinkedHashMap<>();
        for (T item : collection) {
            if (item != null) {
                K key = keyMapper.apply(item);
                if (key != null) {
                    map.put(key, item);
                }
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * [Collections: Set & Generics]
     * Converts a collection to an unmodifiable Set, eliminating duplicates based on equals()/hashCode().
     *
     * @param collection source collection
     * @param <T>        element type
     * @return Set containing distinct elements
     */
    public static <T> Set<T> toSet(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(collection));
    }

    /**
     * Finds the first element matching a predicate.
     *
     * @param collection source collection
     * @param predicate  filter condition
     * @param <T>        element type
     * @return Optional containing found element or empty
     */
    public static <T> Optional<T> findFirst(Collection<T> collection, Predicate<? super T> predicate) {
        if (collection == null || collection.isEmpty() || predicate == null) {
            return Optional.empty();
        }
        return collection.stream().filter(predicate).findFirst();
    }
}
