package com.educode.educodeApi.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Сервіс для управління чергою компіляції коду.
 */
@Service
public class CompileQueueService {

    private final int maxConcurrentChecks;

    private final BlockingQueue<Long> inMemoryQueue;

    /**
     * Конструктор сервісу черги компіляції.
     * @param maxConcurrentChecks максимальна кількість одночасних перевірок
     */
    @Autowired
    public CompileQueueService(@Value("${code.max.concurrent.executions}") int maxConcurrentChecks) {
        this.maxConcurrentChecks = maxConcurrentChecks;
        this.inMemoryQueue = new LinkedBlockingQueue<>();
    }

    /**
     * Додає ідентифікатор до черги.
     * @param id ідентифікатор завдання для компіляції
     */
    public void addToQueue(Long id) {
        inMemoryQueue.add(id);
    }

    /**
     * Перевіряє чи знаходиться завдання серед перших у черзі.
     * @param id ідентифікатор завдання для перевірки
     * @return true якщо завдання знаходиться серед перших maxConcurrentChecks елементів, false - в іншому випадку
     */
    public boolean isInTop(Long id) {
        if (inMemoryQueue.isEmpty() || id == null) {
            return false;
        }

        // Отримуємо масив елементів з черги
        Object[] tempArray = inMemoryQueue.toArray();

        // Перевіряємо тільки перші maxConcurrentChecks елементів
        int limit = Math.min(maxConcurrentChecks, tempArray.length);
        for (int i = 0; i < limit; i++) {
            if (id.equals(tempArray[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * Очікує, поки завдання не опиниться серед перших у черзі.
     * @param decisionId ідентифікатор рішення для очікування
     * @throws InterruptedException якщо час очікування перевищив 2 хвилини
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void waitForQueue(Long decisionId) throws InterruptedException {
        long totalTime = 0L;
        while (!isInTop(decisionId)) {
            Thread.sleep(500);
            totalTime += 500;
            if (totalTime > 120000)
                throw new InterruptedException();
        }
    }

    /**
     * Видаляє завдання з черги.
     * @param id ідентифікатор завдання для видалення
     */
    public void removeFromQueue(Long id) {
        inMemoryQueue.remove(id);
    }
}