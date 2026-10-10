package dao;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * [OOP Concept: Abstraction]
 * [OOP Concept: Inheritance]
 * Abstract base class providing common thread-safe in-memory persistence logic.
 * Implements BaseDao<T, ID> and its inherited CrudOperations<T, ID> interface.
 *
 * @param <T>  Domain entity type
 * @param <ID> Primary identifier type
 */
public abstract class AbstractInMemoryDao<T, ID> implements BaseDao<T, ID> {

    // =========================================================================
    // [OOP Concept: Encapsulation]
    // [Collections: Thread-Safe Collections]
    // ConcurrentHashMap is explicitly used as the backing storage repository because in
    // a multi-threaded web application (e.g., Apache Tomcat worker threads processing
    // concurrent HTTP Servlet requests), multiple threads concurrently read, query,
    // insert, and modify entities. ConcurrentHashMap provides thread-safe operations
    // without full-table locks, preventing race conditions and ConcurrentModificationExceptions.
    // =========================================================================
    protected final Map<ID, T> storage = new ConcurrentHashMap<>();
    protected final AtomicLong idGenerator = new AtomicLong(100);

    // =========================================================================
    // [OOP Concept: Abstraction]
    // Abstract template methods required from concrete subclasses
    // =========================================================================

    /**
     * Extracts the primary identifier from the entity.
     */
    protected abstract ID getEntityId(T entity);

    /**
     * Assigns a newly generated identifier to the entity.
     */
    protected abstract void setEntityId(T entity, ID id);

    // =========================================================================
    // Concrete Implementations of CrudOperations
    // =========================================================================

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Map<ID, T> findAllAsMap() {
        // [Collections: Map]
        // Returns an unmodifiable snapshot of persistent storage keyed by entity ID
        return Collections.unmodifiableMap(new LinkedHashMap<>(storage));
    }

    @Override
    public boolean existsById(ID id) {
        if (id == null) {
            return false;
        }
        return storage.containsKey(id);
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public T save(T entity) {
        if (entity == null) {
            return null;
        }
        ID id = getEntityId(entity);
        if (id == null) {
            id = generateNewId();
            setEntityId(entity, id);
        }
        storage.put(id, entity);
        return entity;
    }

    @Override
    public boolean update(T entity) {
        if (entity == null) {
            return false;
        }
        ID id = getEntityId(entity);
        if (id != null && storage.containsKey(id)) {
            storage.put(id, entity);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteById(ID id) {
        if (id == null) {
            return false;
        }
        return storage.remove(id) != null;
    }

    @SuppressWarnings("unchecked")
    protected ID generateNewId() {
        return (ID) Long.valueOf(idGenerator.incrementAndGet());
    }
}
