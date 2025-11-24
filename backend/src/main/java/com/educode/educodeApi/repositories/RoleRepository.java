package com.educode.educodeApi.repositories;

import com.educode.educodeApi.enums.RoleScope;
import com.educode.educodeApi.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Репозиторій для роботи з ролями користувачів в системі.
 * Забезпечує базові операції CRUD та додаткові методи пошуку ролей.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Знаходить роль за її назвою
     * @param name назва ролі для пошуку
     * @return Optional, що містить знайдену роль, або порожній Optional якщо роль не знайдено
     */
    Optional<Role> findByName(String name);

    /**
     * Находит роли по набору имен (типов)
     * @param names набор типов ролей
     * @return список найденных ролей
     */
    List<Role> findByNameIn(Set<String> names);

    /**
     * Проверяет существование роли по имени
     * @param name тип роли
     * @return true если роль существует
     */
    boolean existsByName(String name);

    List<Role> findAllByScope(RoleScope scope);

    List<Role> findAllByScopeIn(Collection<RoleScope> children);
}
