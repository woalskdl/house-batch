package com.fastcampus.housebatch.core.repository;

import com.fastcampus.housebatch.core.entity.Lawd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LawdRepository extends JpaRepository<Lawd, Long> {
    Optional<Lawd> findByLawdCd(String lawdCd);

    // SELECT DISTINCT LEFT(a.lawd_cd, 5) FROM lawd a WHERE a.exist = 1 AND RIGHT(a.lawd_cd, 8) != '00000000' // LEFT > 오류 발생
    @Query("SELECT DISTINCT SUBSTRING(a.lawdCd, 1, 5) FROM Lawd a WHERE a.exist = 1 AND a.lawdCd NOT LIKE '%00000000'")
    List<String> findDistinctGuLawdCd();
}
