package com.educode.educodeApi.config;

import com.educode.educodeApi.models.BeforeRealUpdate;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SaveInterceptor {

    private final EntityManager em;

    public SaveInterceptor(EntityManager em) { this.em = em; }

//    @Around("execution(* org.springframework.data.repository.CrudRepository+.save(..))")
//    public Object aroundRepoSave(ProceedingJoinPoint pjp) throws Throwable {
//        Object arg = pjp.getArgs()[0];
//
//        boolean isUpdate = isLikelyUpdate(arg);
//        if (isUpdate) callPreIfNeeded(arg);
//
//        return pjp.proceed();
//    }

    @Around("execution(* jakarta.persistence.EntityManager.merge(..))")
    public Object aroundMerge(ProceedingJoinPoint pjp) throws Throwable {
        Object arg = pjp.getArgs()[0];
        // Для merge более логично вызывать на возвращённом managed-инстансе,
        // но если логика preRealUpdate не зависит от managed-статуса — можно до.
        boolean isUpdate = isLikelyUpdate(arg);
        if (isUpdate) callPreIfNeeded(arg);
        return pjp.proceed();
    }

    private boolean isLikelyUpdate(Object entity) {
        if (entity == null) return false;

        // 1) Если реализует Persistable — доверяем isNew()
        if (entity instanceof Persistable<?> p) {
            return !p.isNew();
        }

        // 2) Получаем идентификатор (работает с proxy)
        Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        if (id == null) {
            return false; // явно new
        }

        Class<?> entityClass = getRealClass(entity);

        // 3) Уже в persistence context? — тогда update
        if (em.contains(entity)) return true;

        // 5) Fallback: проверка в БД (единственный SELECT)
        try {
            Object found = em.find(entityClass, id);
            return found != null;
        } catch (IllegalArgumentException ex) {
            // на всякий случай — если find не поддерживает такой класс, считаем update=false
            return false;
        }
    }

    private Class<?> getRealClass(Object entity) {
        // Hibernate proxies: try to unwrap, but на общем уровне — берём класс
        return entity.getClass();
    }

    private void callPreIfNeeded(Object entity) {
        if (entity instanceof BeforeRealUpdate up) {
            try { up.preRealUpdate(); }
            catch (Exception ex) { System.err.println("preRealUpdate error: " + ex); }
        }
    }
}
