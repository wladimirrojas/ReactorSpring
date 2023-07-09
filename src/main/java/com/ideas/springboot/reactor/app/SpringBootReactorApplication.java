package com.ideas.springboot.reactor.app;

import com.ideas.springboot.reactor.app.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootReactorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Flux<User> names = Flux.just("Wladimir", "Pedro", "Maria", "Diego", "Juan")
                .map(name -> new User(name.toUpperCase(), null))
                .doOnNext(user ->
                        {
                        if (user == null) {
                            throw new RuntimeException("names cannot be empty");
                        }
                            System.out.println(user.getName());
                })
                .map(user -> {
                    String name = user.getName().toLowerCase();
                    user.setName(name);
                    return user;
                });

        names.subscribe(
                user -> log.info(user.toString()),
                error -> log.error(error.getMessage()),
                new Runnable() {
                    @Override
                    public void run() {
                        log.info("Ha finalizado la ejecución del observable con éxito");
                    }
                });
    }
}
