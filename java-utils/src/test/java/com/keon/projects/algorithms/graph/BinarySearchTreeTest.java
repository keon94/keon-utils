package com.keon.projects.algorithms.graph;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.primitives.Ints.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BinarySearchTreeTest<B extends BinarySearchTree<Integer>> {

    protected final BinaryNode<Integer> tree = new BinaryNode<Integer>() {

        final Set<Integer> findable = new HashSet<>();
        final Set<Integer> removed = new HashSet<>();

        private final B actual = initTree();

        @Override
        public void insert(final Integer data) {
            findable.add(data);
            removed.remove(data);
            actual.insert(data);
            findable.forEach(x -> assertTrue(find(x), "value: " + x + ", tree: " + this));
            removed.forEach(x -> assertFalse(find(x), "value: " + x + ", tree: " + this));
        }

        @Override
        public boolean find(Integer data) {
            return actual.find(data);
        }

        @Override
        public void delete(final Integer data) {
            if (findable.remove(data)) {
                removed.add(data);
            }
            actual.delete(data);
            findable.forEach(x -> assertTrue(find(x), "value: " + x + ", tree: " + this));
            removed.forEach(x -> assertFalse(find(x), "value: " + x + ", tree: " + this));
        }

        @Override
        public String toString() {
            return actual.toString();
        }
    };

    protected abstract B initTree();

    //Test the tree
    @Test
    public void test() {
        final List<Integer> input = asList(5, 4, 3, 6, 2, 12, 9, 8);
        input.forEach(tree::insert);
        assertFalse(tree.find(100));
        tree.delete(5);
        tree.delete(12);
        tree.delete(8);
        tree.insert(5);
        tree.insert(7);
        tree.delete(4);
        tree.delete(6);
        System.out.println(tree);
    }

}
