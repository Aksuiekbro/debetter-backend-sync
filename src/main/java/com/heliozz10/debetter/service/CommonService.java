package com.heliozz10.debetter.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CommonService {
    @Transactional
    public <T> T findOrCreateEntity(String name, Class<T> entityClass, EntityManager em) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        List<T> existing = em.createQuery(
                        "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.name = :name", entityClass)
                .setParameter("name", name)
                .setMaxResults(1)
                .getResultList();

        if (!existing.isEmpty()) {
            return existing.getFirst();
        }

        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();

            entityClass.getMethod("setName", String.class).invoke(entity, name);

            em.persist(entity);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create entity " + entityClass.getSimpleName(), e);
        }
    }
}
