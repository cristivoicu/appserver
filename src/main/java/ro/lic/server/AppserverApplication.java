package ro.lic.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppserverApplication.class, args);

        System.out.println("Server successfully started!");
    }

}
