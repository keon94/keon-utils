package com.keon.projects.io;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

class Args {

    private static class ArgRegexListType implements IStringConverter<List<String>> {
        @Override
        public List<String> convert(String s) {
            final String[] split = s.split(",");
            for (int i = 0; i < split.length; ++i) {
                split[i] = split[i].trim().replaceAll("([^.]){0}\\*", "$1\\.\\*");
            }
            return Arrays.asList(split);
        }
    }

    @Parameter(names = "-names", listConverter = ArgRegexListType.class)
    List<String> types = Collections.singletonList(".*");

    @Parameter(names = "-dir")
    String root = System.getProperty("user.dir");

    @Parameter(names = "-ref")
    private String reference = null;

    String getReference() {
        if (reference == null) {
            reference = root;
        }
        return reference;
    }
}

public class DuplicateFinderApplication {

    private final Args args;

    private DuplicateFinderApplication(final Args args) {
        this.args = args;
    }

    private Map<String, Set<Path>> mapFiles(final String base, final Map<String, Set<Path>> existing) throws IOException {
        final Path pwd = Paths.get(base).toAbsolutePath().normalize();
        final Map<String, Set<Path>> filesMap;
        filesMap = Files.walk(pwd)
                .filter(Files::isRegularFile)
                .filter(p ->
                        args.types.stream().anyMatch(t -> p.toString().matches(t)) && (existing == null || existing.containsKey(p.getFileName().toString())))
                .collect(groupingBy(
                        p -> p.toFile().getName(),
                        () -> existing != null ? existing : new TreeMap<>(Comparator.comparing(f -> f.replaceAll(".*\\.", "") + f)),
                        mapping(p -> p.toAbsolutePath().normalize(), toSet())));
        return filesMap;
    }

    private void run() throws IOException {
        Map<String, Set<Path>> filesMap = mapFiles(args.getReference(), null);
        if (!args.getReference().equals(args.root)) {
            filesMap = mapFiles(args.root, filesMap);
        }
        filesMap.entrySet().removeIf(e -> e.getValue().size() < 2);
        //print
        final int[] count = {1};
        filesMap.forEach((key, value) -> {
            System.out.println(count[0] + ". " + key);
            for (final Path path : value) {
                System.out.println(spaces(12) + Paths.get(args.root).toAbsolutePath().normalize().relativize(path));
            }
            count[0]++;
        });
    }

    private static String spaces(int count) {
        final StringBuilder res = new StringBuilder();
        while (count > 0) {
            res.append(" ");
            count--;
        }
        return res.toString();
    }

    public static void main(String... argv) throws Exception {
        final Args args = new Args();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(argv);
        new DuplicateFinderApplication(args).run();
    }
}