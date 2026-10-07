package de.bsi.secvisogram.csaf_cms_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for the counters table.
 */
@Entity
@Table(name = "counters")
public class CounterEntity {

    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false)
    private Long count = 0L;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
