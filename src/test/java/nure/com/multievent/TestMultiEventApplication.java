package nure.com.multievent;

import org.springframework.boot.SpringApplication;

public class TestMultiEventApplication {

    public static void main(String[] args) {
        new SpringApplication(MultiEventApplication.class, TestcontainersConfiguration.class).run(args);
    }

}
