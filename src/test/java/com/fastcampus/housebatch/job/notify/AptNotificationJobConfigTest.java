package com.fastcampus.housebatch.job.notify;

import com.fastcampus.housebatch.BatchTestConfig;
import com.fastcampus.housebatch.adapter.FakeSendService;
import com.fastcampus.housebatch.core.dto.AptDto;
import com.fastcampus.housebatch.core.entity.AptNotification;
import com.fastcampus.housebatch.core.entity.Lawd;
import com.fastcampus.housebatch.core.repository.AptNotificationRepository;
import com.fastcampus.housebatch.core.repository.LawdRepository;
import com.fastcampus.housebatch.core.service.AptDealService;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBatchTest
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {AptNotificationJobConfig.class, BatchTestConfig.class})
class AptNotificationJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private AptNotificationRepository aptNotificationRepository;

    @MockBean
    private AptDealService aptDealService;

    @MockBean
    private FakeSendService fakeSendService;

    @MockBean
    private LawdRepository lawdRepository;

    @AfterEach
    public void tearDown() {
        aptNotificationRepository.deleteAll();
    }

    @Test
    public void success() throws Exception {
        // Given
        LocalDate dealDate = LocalDate.now().minusDays(1);
        givenAptNotification();
        givenLawdCd();
        givenApttDeal();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(
                new JobParameters(Maps.newHashMap("dealDate", new JobParameter(dealDate.toString())))
        );

        // Then
        assertEquals(jobExecution.getExitStatus(), ExitStatus.COMPLETED);
        verify(fakeSendService, times(1)).send(anyString(), anyString());

    }

    private void givenApttDeal() {
        when(aptDealService.findByGuLawdCdAndDealDate("11110", LocalDate.now().minusDays(1)))
                .thenReturn(Arrays.asList(
                        new AptDto("IT 아파트", 2000000000L),
                        new AptDto("한 아파트", 1500000000L)
                ));
    }

    private void givenAptNotification() {
        AptNotification notification = new AptNotification();
        notification.setEmail("abc@gmail.com");
        notification.setGuLawdCd("11110");
        notification.setEnabled(true);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        aptNotificationRepository.save(notification);
    }

    private void givenLawdCd() {
        when(lawdRepository.findByLawdCd("1111000000"))
                .thenReturn(
                        Optional.of(Lawd.builder()
                        .lawdCd("1111000000")
                        .lawdDong("경기도 성남시 분당구")
                        .exist(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build())
                );
    }

}