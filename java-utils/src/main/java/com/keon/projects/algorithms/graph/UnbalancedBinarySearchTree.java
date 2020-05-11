package com.keon.projects.algorithms.graph;

public class UnbalancedBinarySearchTree<T extends Comparable<T>> extends BinarySearchTree<T> {

    @Override
    @SuppressWarnings("unchecked")
    protected <U extends AbstractNode<T, U>> U newRoot(T data) {
        return (U) new UnbalancedNode<>(data);
    }

    protected static class UnbalancedNode<T extends Comparable<T>>
            extends AbstractNode<T, UnbalancedNode<T>> {

        public UnbalancedNode(final T data) {
            super(data);
        }

        @Override
        public void insert(T data) {
            insertNode(new UnbalancedNode<>(data));
        }

        @Override
        public void delete(T data) {
            UnbalancedNode<T> parent = findFirst(node -> node.parentOf(data) ? 0 : data.compareTo(node.data));
            if (parent == null) {
                return;
            }
            UnbalancedNode<T> deletable = parent.findNode(data);
            deletable.delete0(parent, parent.left == deletable);
        }

        @Override
        public void insertNode(UnbalancedNode<T> node) {
            final int comp = node.data.compareTo(this.data);
            if (comp == 0) {
                return;
            }
            if (comp > 0) {
                if (right == null) {
                    right = node;
                } else {
                    right.insertNode(node);
                }
            } else {
                if (left == null) {
                    left = node;
                } else {
                    left.insertNode(node);
                }
            }
        }

        private void delete0(final UnbalancedNode<T> parent, final boolean leftDeletable) {
            if (left == null) {
                if (leftDeletable) { //true if "this" is the parent's left node
                    parent.left = right;
                } else {
                    parent.right = right;
                }
            } else if (right == null) {
                if (leftDeletable) {
                    parent.left = left;
                } else {
                    parent.right = left;
                }
            } else {
                //this has both children. promote right child
                if (leftDeletable) {
                    parent.left = right;
                } else {
                    parent.right = right;
                }
                //now go ahead and insert the left child
                right.insertNode(left);
            }
        }

        private boolean parentOf(final T data) {
            return left != null && left.data.equals(data) || right != null && right.data.equals(data);
        }

    }
}

