package com.keon.projects.junit.engine;

import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;

public class CustomEngine implements TestEngine {

    private final TestEngine engine = new JupiterTestEngine();

    @Override
    public String getId() {
        return "KEON_ID";
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        //ignore incoming request
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(discoverClasses())
                .build();
        return engine.discover(request, uniqueId);
    }

    @Override
    public void execute(ExecutionRequest request) {
        engine.execute(request);
    }

    private ClassSelector[] discoverClasses() {
        try {
            final URL testClassesURL = Paths.get("target/test-classes").toUri().toURL();
            final Collection<Class<?>> types = new Reflections(new ConfigurationBuilder()
                    .addClassLoader(URLClassLoader.newInstance(new URL[]{testClassesURL},ClasspathHelper.staticClassLoader()))
                    .forPackages("")
                    .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner()))
                    .getTypesAnnotatedWith(CustomRunner.class, true);
            final Set<Class<?>> sorted = new SuiteSorter(types).getSorted();
            return sorted.stream().map(DiscoverySelectors::selectClass).toArray(ClassSelector[]::new);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
