package com.keon.projects.algorithms.graph;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class BinarySearchTree<T extends Comparable<T>> implements BinaryNode<T> {

    private AbstractNode root;

    protected abstract <U extends AbstractNode<T, U>> U newRoot(final T data);

    protected <U extends AbstractNode<T, U>> U root() {
        return (U) root;
    }

    @Override
    public void insert(T data) {
        if (root == null) {
            root = newRoot(data);
        } else {
            root.insert(data);
        }
    }

    @Override
    public boolean find(T data) {
        return root != null && root.find(data);
    }

    @Override
    public void delete(T data) {
        if (root == null) {
            return;
        }
        if (root.data.equals(data)) {
            if (root.right == null) {
                root = root.left;
            } else {
                if (root.left != null) {
                    root.right.insertNode(root.left);
                }
                root = root.right;
            }
        } else {
            root.delete(data);
        }
    }

    @Override
    public String toString() {
        return root == null ? "{}" : root.toString();
    }

}


interface BinaryNode<T extends Comparable<T>> {

    void insert(T data);

    boolean find(T data);

    void delete(T data);

}
