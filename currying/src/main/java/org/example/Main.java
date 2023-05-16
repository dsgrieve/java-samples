package org.example;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Main {

    record Arg<R>(String name, R value) {}

    interface Fn<R> extends BiFunction<List<Arg<?>>, Kernel, R> {};

    record NamedFn<R>(String name, Fn<R> body) {}

    static ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sample-thread");
        thread.setDaemon(true);
        return thread;
    });

    record Kernel(String name) {

        <R> Function<List<Arg<?>>, Future<R>> register(NamedFn<R> namedFn) {
            System.out.println("register NamedFn " + namedFn.name());
            return args -> {
                System.out.println("invoke NamedFn " + namedFn.name() + " with " + args);
                return executorService.submit(() -> namedFn.body().apply(args, Kernel.this));
            };
        }

    }

    static <R> Function<Kernel, Future<R>> register(NamedFn<R> namedFn, List<Arg<?>> args) {
        System.out.println("register NamedFn " + namedFn.name() + " with " + args);
        return kernel -> {
            System.out.println("invoke NamedFn " + namedFn.name() + " with " + args + " using kernel " + kernel.name() + "!");
            return executorService.submit(() -> namedFn.body().apply(args, kernel));
        };
    }

    public static void main(String[] args) throws Exception {

        List<Arg<?>> helloworldArgs = List.of(
                new Arg<>("arg1", "Hello"),
                new Arg<>("arg2", "World")
        );

        Fn<String> helloWorld = (argList, kernel) -> {
            String arg1Value = (String) argList.stream().filter(arg -> Objects.equals(arg.name(), "arg1")).findFirst().get().value();
            String arg2Value = (String) argList.stream().filter(arg -> Objects.equals(arg.name(), "arg2")).findFirst().get().value();
            System.out.println("fn helloWorld called with " + arg1Value + ", " + arg2Value + " and kernel " + kernel.name() + "!");
            return arg1Value + ", " + arg2Value + "!";
        };

        NamedFn<String> sayIt = new NamedFn<>("sayIt", helloWorld);
        // "Colonel Sanders", get it? :D
        Kernel kernel = new Kernel("Sanders");

        System.out.println("registering fn " + sayIt.name() + " with partially applied args kernel " + kernel.name() );
        Function<List<Arg<?>>, Future<String>> registeredFn = kernel.register(sayIt);
        Future<String> future = registeredFn.apply(helloworldArgs);
        System.out.println(future.get());

        System.out.println("registering fn " + sayIt.name() + " with partially applied args helloworldArgs " + helloworldArgs );
        Function<Kernel, Future<String>> registeredFn2 = register(sayIt, helloworldArgs);
        future = registeredFn2.apply(kernel);
        System.out.println(future.get());

    }
}
