package com.sabbih.meshadaaiservices.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class SparkConfig {

    @Value(value = "${meshada.spark.master}")
    private String master;

    @Value(value = "${meshada.spark.app.name}")
    private String appName;

    @Bean
    public SparkSession sparkSession() {
        return SparkSession.builder()
                .appName(appName)
                .master(master)
                .config("spark.executor.memory", "4g")
                .config("spark.driver.memory", "2g")
                .config("spark.sql.shuffle.partitions", "200")
                .config("spark.executor.extraJavaOptions", "-Xss1m")
                .config("spark.driver.extraJavaOptions", "-Xss1m")
                .getOrCreate();
    }

    @Bean(name = "blockingTaskExecutor")
    public Executor blockingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // Minimum number of threads
        executor.setMaxPoolSize(50);  // Maximum number of threads
        executor.setQueueCapacity(100); // Queue size before spawning new threads
        executor.setThreadNamePrefix("BlockingExecutor-");
        executor.initialize();
        return executor;
    }
}
