package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

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
}
