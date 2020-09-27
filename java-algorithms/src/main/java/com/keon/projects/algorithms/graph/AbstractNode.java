package com.keon.projects.algorithms.graph;

import java.util.function.Function;

public abstract class AbstractNode<T extends Comparable<T>, N extends AbstractNode<T, N>> implements BinaryNode<T> {

    protected T data;
    protected N left, right;

    protected AbstractNode(T data) {
        this.data = data;
    }

    @Override
    public boolean find(T data) {
        return findNode(data) != null;
    }

    public N findNode(final T data) {
        return findFirst(node -> data.compareTo(node.data));
    }

    @SuppressWarnings("unchecked")
    public N findFirst(final Function<N, Integer> qualifier) {
        final int comp = qualifier.apply((N) this);
        if (comp == 0) {
            return (N) this;
        }
        if (comp > 0) {
            return right == null ? null : right.findFirst(qualifier);
        } else {
            return left == null ? null : left.findFirst(qualifier);
        }
    }

    public abstract void insertNode(N node);

    public void swap(N node) {
        final T thisData = data;
        final N thisLeft = left;
        final N thisRight = right;
        this.data = node.data;
        this.left = node.left;
        this.right = node.right;
        node.data = thisData;
        node.left = thisLeft;
        node.right = thisRight;
    }

    @Override
    public String toString() {
        //return data.toString();
        return "{\"data\":" + data + ",\"left\":" + (left == null ? "null" : left) + ",\"right\":" + (right == null ? "null" : right) + "}";
    }

}
