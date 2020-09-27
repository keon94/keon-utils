package com.keon.projects.algorithms.graph;

public class BalancedBinarySearchTreeTest extends BinarySearchTreeTest<BalancedBinarySearchTree<Integer>> {
    @Override
    protected BalancedBinarySearchTree<Integer> initTree() {
        return new BalancedBinarySearchTree<>();
    }
}
