package de.bsi.secvisogram.csaf_cms_backend.repository;

import de.bsi.secvisogram.csaf_cms_backend.entity.CounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for sequential counters.
 * Provides atomic increment to replace the racy read-modify-write CouchDB pattern.
 */
public interface CounterRepository extends JpaRepository<CounterEntity, String> {

    /**
     * Atomically increment the counter and return the new value.
     * This eliminates the race condition present in the CouchDB implementation.
     *
     * <p>Note: {@code @Modifying} is intentionally omitted. The {@code RETURNING} clause
     * makes this a row-returning query in PostgreSQL, so Spring Data must use
     * {@code getResultList()} (SELECT path) rather than {@code executeUpdate()} to
     * capture the returned value.</p>
     */
    @Query(value = "UPDATE counters SET count = count + 1 WHERE id = :id RETURNING count",
            nativeQuery = true)
    Long incrementAndGet(@Param("id") String id);
}
