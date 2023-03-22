package com.fastcampus.housebatch.core.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@Builder
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lawd")
public class Lawd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lawdId;

    @Column(nullable = false)
    private String lawdCd;

    @Column(nullable = false)
    private String lawdDong;

    @Column(nullable = false)
    private Boolean exist;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

}
