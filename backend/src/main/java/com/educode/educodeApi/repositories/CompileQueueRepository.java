package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.CompileQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Репозиторій для роботи з чергою компіляції.
 * Забезпечує доступ до операцій з базою даних для сутності CompileQueue.
 */
public interface CompileQueueRepository extends JpaRepository<CompileQueue, Long> {

    /**
     * Знаходить перші чотири записи (це можна міняти) з черги компіляції, відсортованих за ідентифікатором.
     * @return Список з чотирьох перших (це можна міняти) записів з черги компіляції, відсортованих за ідентифікатором
     */
    @Query("SELECT c FROM CompileQueue c ORDER BY c.id ASC LIMIT 4") // Вказує скільки паралельно сервер може компілювати
    List<CompileQueue> findFourFirstByOrderByPriorityAsc();
}
