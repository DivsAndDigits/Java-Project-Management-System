package dao;

/**
 * [OOP Concept: Interface]
 * [OOP Concept: Inheritance]
 * Generic Data Access Object interface extending CrudOperations<T, ID>.
 * Demonstrates Interface Inheritance with Java Generics.
 *
 * @param <T>  The entity domain type
 * @param <ID> The entity primary key identifier type
 */
public interface BaseDao<T, ID> extends CrudOperations<T, ID> {
    // Inherits standard findById, findAll, save, update, deleteById from CrudOperations<T, ID>
}
