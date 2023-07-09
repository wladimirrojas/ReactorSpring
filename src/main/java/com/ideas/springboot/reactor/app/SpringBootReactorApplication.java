package com.ideas.springboot.reactor.app;

import com.ideas.springboot.reactor.app.models.Comments;
import com.ideas.springboot.reactor.app.models.User;
import com.ideas.springboot.reactor.app.models.UserComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootReactorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        userCommentFlatMap();
    }

    public User userCreator() {
        return new User("Wladimir", "Rojas");
    }

    public void userCommentFlatMap() {
        //Mono<User> user = Mono.fromCallable(() -> userCreator());
        Mono<User> user = Mono.fromCallable(() -> new User("Wladimir", "Rojas"));

        Mono<Comments> userComments = Mono.fromCallable(() -> {
            Comments comments = new Comments();
            comments.addComments("Hola Soy Wladimir!");
            comments.addComments("Los saludo desde Github!");
            return comments;
        });

        user.flatMap(userMono -> userComments.map(comment -> new UserComment(userMono, comment)))
                .subscribe(userComment -> log.info(userComment.toString()));

    }

    public void collectList() throws Exception {

        List<User> usersNamesList = new ArrayList<>();
        Collections.addAll(usersNamesList,
                new User("Wladimir", "Rojas"),
                new User("Pablo", " Parlo"),
                new User("Maria", "Nadie"),
                new User("Juan", "Castillo"),
                new User("José", "Megan"),
                new User("Bruce", "Lee"),
                new User("Bruce", "Willis"));

        Flux.fromIterable(usersNamesList)
                .collectList()
                .subscribe(userList -> userList.forEach(user -> log.info(user.toString())));
    }

    public void flatMapOperatorToString() throws Exception {

        List<User> usersNamesList = new ArrayList<>();
        Collections.addAll(usersNamesList,
                new User("Wladimir", "Rojas"),
                new User("Pablo", " Parlo"),
                new User("Maria", "Nadie"),
                new User("Juan", "Castillo"),
                new User("José", "Megan"),
                new User("Bruce", "Lee"),
                new User("Bruce", "Willis"));

        Flux.fromIterable(usersNamesList)
                .map(user -> user.getName().toUpperCase().concat(" ").concat(user.getLastName().toUpperCase()))
                .flatMap(name -> {
                    if (name.contains("bruce".toUpperCase())) {
                        return Mono.just(name);
                    }
                    return Mono.empty();
                })
                .map(String::toLowerCase)
                .subscribe(name -> log.info(name.toString()));
    }



    public void flatMapOperator() throws Exception {

        List<String> usersNamesList = new ArrayList<>();
        Collections.addAll(usersNamesList, "Wladimir Rojas", "Pablo Parlo", "Maria Nadie", "Juan Castillo", "José Megan", "Bruce Lee", "Bruce Willis");

        Flux.fromIterable(usersNamesList)
                .map(name -> new User(name.split(" ")[0].toUpperCase(), name.split(" ")[1].toUpperCase()))
                .flatMap(user -> {
                    if (user.getName().equalsIgnoreCase("bruce")) {
                        return Mono.just(user);
                    }
                    return Mono.empty();
                })
                .map(user -> {
                    String name = user.getName().toLowerCase();
                    user.setName(name);
                    return user;
                })
                .subscribe(user -> log.info(user.toString()));
    }

    public void iterable() throws Exception {

        List<String> usersNamesList = new ArrayList<>();
        Collections.addAll(usersNamesList, "Wladimir Rojas", "Pablo Parlo", "Maria Nadie", "Juan Castillo", "José Megan", "Bruce Lee", "Bruce Willis");

        Flux<String> names = Flux.fromIterable(usersNamesList);

        Flux<User> users = names
                .map(name -> new User(name.split(" ")[0].toUpperCase(), name.split(" ")[1].toUpperCase()))
                .filter(user -> user.getName().toLowerCase().equals("bruce"))
                .doOnNext(user ->
                {
                    if (user == null) {
                        throw new RuntimeException("names cannot be empty");
                    }
                    System.out.println(user.getName().concat(" ").concat(user.getLastName()));
                })
                .map(user -> {
                    String name = user.getName().toLowerCase();
                    user.setName(name);
                    return user;
                });

        users.subscribe(
                user -> log.info(user.toString()),
                error -> log.error(error.getMessage()),
                () -> log.info("Ha finalizado la ejecución del observable con éxito"));
    }
}
