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
}
