package io.github.javiercanillas;

import io.github.javiercanillas.workers.WorkerConfiguration;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@Import( { WorkerConfiguration.class } )
public class SpringConfiguration {

    @Bean
    public WorkflowClient workflowClient(@Value("${temporal-server.host}") String host,
                                         @Value("${temporal-server.port}") String port) {
        var wsso = WorkflowServiceStubsOptions.newBuilder()
                .setTarget("localhost:7233")
                .build();
        WorkflowServiceStubs service = WorkflowServiceStubs.newInstance(wsso);
        WorkflowClientOptions options = WorkflowClientOptions.newBuilder()
                .build();

        return WorkflowClient.newInstance(service, options);
    }


//    @Bean(name = "backgroundExecutor")
//    public ExecutorService backgroundExecutor() {
//        return new ThreadPoolExecutor(10, 50, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
//                new ThreadFactory() {
//                    final AtomicLong count = new AtomicLong(1L);
//                    @Override
//                    public Thread newThread(Runnable r) {
//                        var thread = Executors.defaultThreadFactory().newThread(r);
//                        thread.setName(String.format(Locale.ROOT, "consumer-%2d", count.getAndIncrement()));
//                        thread.setUncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
//                        return thread;
//                    }
//                });
//    }


}
