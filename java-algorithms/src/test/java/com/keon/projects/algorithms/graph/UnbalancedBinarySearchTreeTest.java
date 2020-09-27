package com.keon.projects.algorithms.graph;

import org.junit.jupiter.api.Disabled;

@Disabled
public class UnbalancedBinarySearchTreeTest extends BinarySearchTreeTest<UnbalancedBinarySearchTree<Integer>> {

    @Override
    protected UnbalancedBinarySearchTree<Integer> initTree() {
        return new UnbalancedBinarySearchTree<>();
    }
}
