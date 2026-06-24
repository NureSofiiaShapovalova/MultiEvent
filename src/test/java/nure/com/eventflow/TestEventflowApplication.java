package nure.com.eventflow;

import org.springframework.boot.SpringApplication;

public class TestEventflowApplication {

    public static void main(String[] args) {
        new SpringApplication(EventflowApplication.class, TestcontainersConfiguration.class).run(args);
    }

}
