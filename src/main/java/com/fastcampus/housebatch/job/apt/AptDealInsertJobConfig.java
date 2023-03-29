package com.fastcampus.housebatch.job.apt;

import com.fastcampus.housebatch.adapter.ApartmentApiResource;
import com.fastcampus.housebatch.core.dto.AptDealDto;
import com.fastcampus.housebatch.core.repository.LawdRepository;
import com.fastcampus.housebatch.job.validator.LawdCdParameterValidator;
import com.fastcampus.housebatch.job.validator.YearMonthParameterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AptDealInsertJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ApartmentApiResource apartmentApiResource;
    private final LawdRepository lawdRepository;

    @Bean
    public Job aptDealInsertJob(
            Step guLawdCdStep,
            Step contextPrintStep
//            Step aptDealInsertStep
    ) {
        return jobBuilderFactory.get("aptDealInsertJob")
                .incrementer(new RunIdIncrementer())
                .validator(aptDealJobParameterValidator())
                .start(guLawdCdStep)
                .on("CONTINUABLE").to(contextPrintStep).next(guLawdCdStep)
                .from(guLawdCdStep)
                .on("*" ).end()
                .end()
//                .next(contextPrintStep)
//                .next(aptDealInsertStep)
                .build();
    }

    private JobParametersValidator aptDealJobParameterValidator() {
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
        // List 로 순서대로 진행됨.
        validator.setValidators(Arrays.asList(
                new YearMonthParameterValidator()
//                new LawdCdParameterValidator()
        ));

        return validator;
    }

    @JobScope
    @Bean
    public Step guLawdCdStep(Tasklet guLawdCdTasklet) {
        return stepBuilderFactory.get("guLawdCdStep")
                .tasklet(guLawdCdTasklet)
                .build();
    }

    /**
     * ExecutionContext 에 저장할 데이터
     * 1. guLawdCd      > 구 코드 - 다음 스텝에서 활용할 값
     * 2. guLawdCdList  > 구 코드 리스트
     * 3. itemCount     > 남아있는 구 코드의 갯수
     */
    @StepScope
    @Bean
    public Tasklet guLawdCdTasklet() {
        return (contribution, chunkContext) -> {
            StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
            // step to step 전달을 위해
            ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();

            // 데이터가 있으면 다음 스텝을 실행하고, 없으면 종료하도록 설정
            // 데이터가 있으면 > CONTINUALBLE
            List<String> guLawdCdList;
            if (!executionContext.containsKey("guLawdCdList")) {
                guLawdCdList = lawdRepository.findDistinctGuLawdCd();
                executionContext.put("guLawdCdList", guLawdCdList);
                executionContext.putInt("itemCount", guLawdCdList.size());
            } else {
                guLawdCdList = (List<String>) executionContext.get("guLawdCdList");
            }

            int itemCount = executionContext.getInt("itemCount");

            if (itemCount == 0) {
                contribution.setExitStatus(ExitStatus.COMPLETED);
                return RepeatStatus.FINISHED;
            }

            itemCount -= 1;

            String guLawdCd = guLawdCdList.get(itemCount);
            executionContext.putString("guLawdCd", guLawdCd);
            executionContext.putInt("itemCount", itemCount);

            contribution.setExitStatus(new ExitStatus("CONTINUABLE"));
            return RepeatStatus.FINISHED;
        };
    }

    @JobScope
    @Bean
    public Step contextPrintStep(Tasklet contextPrintTasklet) {
        return stepBuilderFactory.get("contextPrintStep")
                .tasklet(contextPrintTasklet)
                .build();
    }

    @StepScope
    @Bean
    public Tasklet contextPrintTasklet(
            @Value("#{jobExecutionContext['guLawdCd']}") String guLawdCd
    ) {
        return ((contribution, chunkContext) -> {
            // executionContext 값을 출력
            System.out.println("[contextPrintTasklet] guLawdCd : " + guLawdCd);

            return RepeatStatus.FINISHED;
        });
    }

    @JobScope
    @Bean
    public Step aptDealInsertStep(
            StaxEventItemReader<AptDealDto> aptDealResourceReader,
            ItemWriter<AptDealDto> aptDealWriter
    ) {
        return stepBuilderFactory.get("aptDealInsertStep")
                .<AptDealDto, AptDealDto>chunk(10)
                .reader(aptDealResourceReader)
                .writer(aptDealWriter)
                .build();
    }

    @StepScope
    @Bean
    public StaxEventItemReader<AptDealDto> aptDealResourceReader(
//            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{jobParameters['yearMonth']}") String yearMonth,
            @Value("#{jobExecutionContext['guLawdCd']}") String guLawdCd,
            Jaxb2Marshaller aptDealDtoMarshaller
    ) {
        return new StaxEventItemReaderBuilder<AptDealDto>()
                .name("aptDealResourceReader")
                .resource(apartmentApiResource.getResource(guLawdCd, YearMonth.parse(yearMonth)))
                .addFragmentRootElements("item")                // xml 파일에서 읽을 요소 (실거래 API 응답에서 item 으로 오기 때문)
                .unmarshaller(aptDealDtoMarshaller)             // 파일 > 객체 매핑
                .build();
    }

    @StepScope
    @Bean
    public Jaxb2Marshaller aptDealDtoMarshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(AptDealDto.class);
        return jaxb2Marshaller;
    }

    @StepScope
    @Bean
    public ItemWriter<AptDealDto> aptDealWriter() {
        return items -> {
            items.forEach(System.out::println);
            System.out.println("==================== Writing Completed =====================");     // 설정한 chunk 단위로 작업 수행 (10개 읽고 한번에 쓰여짐)
        };
    }

}
