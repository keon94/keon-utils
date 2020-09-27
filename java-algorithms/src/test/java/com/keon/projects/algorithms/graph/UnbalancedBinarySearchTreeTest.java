package com.keon.projects.algorithms.graph;

public class UnbalancedBinarySearchTreeTest extends BinarySearchTreeTest<UnbalancedBinarySearchTree<Integer>> {

    @Override
    protected UnbalancedBinarySearchTree<Integer> initTree() {
        return new UnbalancedBinarySearchTree<>();
    }
}
