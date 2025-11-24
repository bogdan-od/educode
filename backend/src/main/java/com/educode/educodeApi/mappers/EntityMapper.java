package com.educode.educodeApi.mappers;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Базовый интерфейс для ручных мапперов entity <-> dto.
 * E  - Entity
 * D  - DTO (обычно response/transport DTO)
 * CD - Create DTO (опционально)
 * UD - Update DTO (опционально)
 */
public interface EntityMapper<E, D> {
    /** full conversion entity -> dto */
    D toDto(E entity);

    /** full conversion dto -> entity (создание новой entity) */
    E toEntity(D dto);

    /**
     * Merge/patch: влить данные из DTO в существующую managed entity.
     * Должен мутировать entity, не создавать новую.
     */
    void updateFromDto(D dto, E entity);

    /* --- удобные default методы --- */

    default List<D> toDtoList(Collection<E> entities) {
        return entities == null ? Collections.emptyList()
                : entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    default Set<D> toDtoSet(Collection<E> entities) {
        return entities == null ? Collections.emptySet()
                : entities.stream().map(this::toDto).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default List<E> toEntityList(Collection<D> dtos) {
        return dtos == null ? Collections.emptyList()
                : dtos.stream().map(this::toEntity).collect(Collectors.toList());
    }

    default Optional<D> toDtoOptional(Optional<E> maybe) {
        return maybe == null ? Optional.empty() : maybe.map(this::toDto);
    }
}
