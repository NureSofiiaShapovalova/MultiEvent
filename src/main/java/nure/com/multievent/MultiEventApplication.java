package nure.com.multievent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MultiEventApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiEventApplication.class, args);
    }

}
