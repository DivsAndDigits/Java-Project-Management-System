package dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * [OOP Concept: Interface]
 * [Collections: Generics]
 * Fundamental generic contract defining Create, Read, Update, and Delete operations.
 * Demonstrates Java Generics <T, ID> for scalable, type-safe data access abstractions.
 *
 * @param <T>  Domain entity type
 * @param <ID> Primary identifier type
 */
public interface CrudOperations<T, ID> {

    /**
     * Retrieves an entity by its unique identifier.
     *
     * @param id the primary identifier
     * @return Optional containing the entity if found, empty Optional otherwise
     */
    Optional<T> findById(ID id);

    /**
     * Retrieves all entities in the storage repository as a List.
     *
     * @return list containing all persistent entities
     */
    List<T> findAll();

    /**
     * [Collections: Map]
     * Retrieves all entities as an indexed Map keyed by primary identifier.
     * Provides O(1) key-based lookup across cached entities.
     *
     * @return map of ID to entity
     */
    Map<ID, T> findAllAsMap();

    /**
     * Persists a new entity to the repository.
     *
     * @param entity the entity to store
     * @return the saved entity with assigned identifier
     */
    T save(T entity);

    /**
     * Updates an existing entity's state.
     *
     * @param entity the entity with updated values
     * @return true if updated, false if entity not found
     */
    boolean update(T entity);

    /**
     * Deletes an entity matching the given identifier.
     *
     * @param id the identifier of the entity to remove
     * @return true if removed, false otherwise
     */
    boolean deleteById(ID id);

    /**
     * Checks if an entity exists for the given identifier.
     *
     * @param id primary identifier
     * @return true if entity exists
     */
    boolean existsById(ID id);

    /**
     * Returns total number of entities managed by this repository.
     *
     * @return count of entities
     */
    long count();
}
