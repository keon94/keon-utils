package com.keon.projects.algorithms.graph;

import org.junit.jupiter.api.Disabled;

@Disabled
public class BalancedBinarySearchTreeTest extends BinarySearchTreeTest<BalancedBinarySearchTree<Integer>> {
    @Override
    protected BalancedBinarySearchTree<Integer> initTree() {
        return new BalancedBinarySearchTree<>();
    }
}
