package io.k8swatcher.podinformer;

import io.k8swatcher.annotation.EnableInformer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableInformer
public class PodInformerExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(PodInformerExampleApplication.class, args);
    }
}
