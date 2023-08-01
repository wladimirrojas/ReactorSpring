package com.ideas.springboot.reactor.app;

import com.ideas.springboot.reactor.app.models.Comments;
import com.ideas.springboot.reactor.app.models.User;
import com.ideas.springboot.reactor.app.models.UserComment;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootReactorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        counterPressure();
    }

    public void counterPressure() {

        Flux.range(1, 10)
                .log()
                //.limitRate(5)
                .subscribe(
                        new Subscriber<Integer>() {

                    private Subscription s;

                    private Integer limit = 5;
                    private Integer consumed = 0;

                    @Override
                    public void onSubscribe(Subscription s) {
                        this.s = s;
                        s.request(limit);
                    }

                    @Override
                    public void onNext(Integer o) {
                        log.info(o.toString());
                        consumed++;
                        if (consumed == limit) {
                            consumed = 0;
                            s.request(limit);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void intervalByCreate() {
        Flux.create(emitter -> {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                private Integer counter = 0;
                @Override
                public void run() {
                    emitter.next(++counter);
                    if (counter == 10) {
                        timer.cancel();
                        emitter.complete();
                    }
                    if (counter == 5) {
                        timer.cancel();
                        emitter.error(new InterruptedException("We have stopped at 5"));
                    }

                }
            }, 1000, 1000);
        })
                .subscribe(next -> log.info(next.toString()),
                        error -> log.error(error.getMessage()),
                        () -> log.info("We have finished"));
    }

    public void endlessInterval() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        Flux.interval(Duration.ofSeconds(1))
                .doOnTerminate(latch::countDown)
                .flatMap(i -> {
                    if (i >= 5) {
                        return Flux.error(new InterruptedException("Only to five"));
                    }
                    return Flux.just(i);
                })
                .map(i -> "Hola " + i)
                .retry(2)
                .subscribe(s -> log.info(s), e -> log.error(e.getMessage()));

        latch.await();
    }

    public void delayExample() throws InterruptedException {
        Flux<Integer> range = Flux.range(1, 12)
                .delayElements(Duration.ofSeconds(1))
                .doOnNext(i -> log.info(i.toString()));

        range.subscribe();

        Thread.sleep(13000);
    }

    public void interalExample() {
        Flux<Integer> range = Flux.range(1, 12);
        Flux<Long> delay = Flux.interval(Duration.ofSeconds(1));

        range.zipWith(delay, (ra, de) -> ra)
                .doOnNext(i -> log.info(i.toString()))
                .blockLast();
    }

    public void zipWithRange() {
        Flux.just(1, 2, 3, 4)
                .map(i -> (i*2))
                .zipWith(Flux.range(0, 4), (first, second) -> String.format("First Flux: %d, Second Flux: %d", first, second))
                .subscribe(text -> log.info(text));
    }

    public void userCommentZipTwo() {
        //Mono<User> user = Mono.fromCallable(() -> userCreator());
        Mono<User> userMono = Mono.fromCallable(() -> new User("Wladimir", "Rojas"));

        Mono<Comments> userComments = Mono.fromCallable(() -> {
            Comments comments = new Comments();
            comments.addComments("Hola Soy Wladimir!");
            comments.addComments("Los saludo desde Github!");
            return comments;
        });

        Mono<UserComment> userCommentMono = userMono.zipWith(userComments)
                .map(tuple -> {
                    User u = tuple.getT1();
                    Comments c = tuple.getT2();
                    return new UserComment(u, c);
                });

        userCommentMono.subscribe(userComment -> log.info(userComment.toString()));

    }

    public void userCommentZip() {
        //Mono<User> user = Mono.fromCallable(() -> userCreator());
        Mono<User> userMono = Mono.fromCallable(() -> new User("Wladimir", "Rojas"));

        Mono<Comments> userComments = Mono.fromCallable(() -> {
            Comments comments = new Comments();
            comments.addComments("Hola Soy Wladimir!");
            comments.addComments("Los saludo desde Github!");
            return comments;
        });

        Mono<UserComment> userCommentMono = userMono.zipWith(userComments, (user, comment) -> new UserComment(user, comment));
        userCommentMono.subscribe(userComment -> log.info(userComment.toString()));

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
