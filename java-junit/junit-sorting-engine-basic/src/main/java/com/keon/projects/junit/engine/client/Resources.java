package com.keon.projects.junit.engine.client;

import com.keon.projects.junit.engine.client.ResourceGraph.Resource;
import com.keon.projects.junit.engine.client.ResourceGraph.Resource.Builder;

import java.util.Set;

public class Resources {

    public static final String A = "a", B = "b", C = "c", D = "d", E = "e", F = "f", G = "g", H = "h";

    private final ResourceGraph<String> graph = new ResourceGraph<>();

    public Resources add(final String... resources) {
        for (final String resource : resources) {
            if (A.equals(resource))
                graph.add(Builder.id(A).weight(1.0f).dependencies(B, C, D).build());
            else if (B.equals(resource))
                graph.add(Builder.id(B).weight(1.1f).dependencies(C, D).build());
            else if (C.equals(resource))
                graph.add(Builder.id(C).weight(1.2f).dependencies(H).build());
            else if (D.equals(resource))
                graph.add(Builder.id(D).weight(1.0f).dependencies(C, G).build());
            else if (E.equals(resource))
                graph.add(Builder.id(E).weight(1.3f).dependencies(B, C, D).build());
            else if (F.equals(resource))
                graph.add(Builder.id(F).weight(1.5f).dependencies(E, C, D).build());
            else if (G.equals(resource))
                graph.add(Builder.id(G).weight(1.8f).dependencies(A, C, E, F, B).build());
            else if (H.equals(resource))
                graph.add(Builder.id(H).weight(2.0f).dependencies(G).build());
        }
        return this;
    }

    public Set<Resource<String>> getResources() {
        return graph.getResources();
    }

}
