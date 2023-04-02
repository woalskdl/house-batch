package com.fastcampus.housebatch.core.entity;

import com.fastcampus.housebatch.core.dto.AptDealDto;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "apt")
@EntityListeners(AuditingEntityListener.class)
public class Apt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aptId;
    @Column(nullable = false)
    private String aptName;
    @Column(nullable = false)
    private String jibun;
    @Column(nullable = false)
    private String dong;
    @Column(nullable = false)
    private String guLawdCd;
    @Column(nullable = false)
    private Integer builtYear;
    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Apt of(AptDealDto dto) {
        return Apt.builder()
                .aptName(dto.getAptName().trim())
                .jibun(dto.getJibun().trim())
                .dong(dto.getDong().trim())
                .guLawdCd(dto.getRegionalCode().trim())
                .builtYear(dto.getBuiltYear())
                .build();
    }
}
