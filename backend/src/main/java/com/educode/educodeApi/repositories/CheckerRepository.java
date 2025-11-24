package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.Checker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckerRepository extends JpaRepository<Checker, Long> {
    @Query("SELECT COUNT(p) FROM Puzzle p WHERE p.checker = :checker")
    Long countPuzzlesByChecker(Checker checker);

    @Query("SELECT c FROM Checker c WHERE " +
            "LOWER(c.name) LIKE %:query% " +
            "AND c.user.id = :userId")
    Page<Checker> searchByUserAndQuery(@Param("query") String query,
                              @Param("userId") Long userId,
                              Pageable pageable);
}
