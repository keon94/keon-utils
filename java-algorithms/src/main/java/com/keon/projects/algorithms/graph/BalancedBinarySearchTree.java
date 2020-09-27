package com.keon.projects.algorithms.graph;

public class BalancedBinarySearchTree<T extends Comparable<T>> extends BinarySearchTree<T> {

    @Override
    @SuppressWarnings("unchecked")
    protected <U extends AbstractNode<T, U>> U newRoot(T data) {
        return (U) new BalancedNode<>(data);
    }

    static class BalancedNode<T extends Comparable<T>>
            extends AbstractNode<T, BalancedNode<T>> {

        BalancedNode(T data) {
            super(data);
        }

        enum ImbalancedType {
            NONE, RR, RL, LR, LL
        }

        private BalancedNode(T data, BalancedNode<T> left, BalancedNode<T> right) {
            super(data);
            this.left = left;
            this.right = right;
        }

        @Override
        public void insert(T data) {
            insertNode(new BalancedNode<>(data));
        }

        @Override
        public void insertNode(BalancedNode<T> node) {
            insert(node, null);
        }

        private void insert(BalancedNode<T> node, BalancedNode<T> parent) {
            final int comp = node.data.compareTo(this.data);
            if (comp == 0) {
                return;
            }
            if (comp > 0) {
                if (right == null) {
                    right = node;
                    right.balance(this, parent);
                } else {
                    right.insert(node, this);
                }
            } else {
                if (left == null) {
                    left = node;
                    left.balance(this, parent);
                } else {
                    left.insert(node, this);
                }
            }
        }

        private void balance(BalancedNode<T> parent, BalancedNode<T> grandparent) {

            final ImbalancedType imbalance = isImbalanced(parent, grandparent);
            if (imbalance == ImbalancedType.NONE) {
                return;
            }
            if (imbalance == ImbalancedType.LL) {
                grandparent.left = new BalancedNode<>(this.data);
                grandparent.right = new BalancedNode<>(grandparent.data, null, grandparent.right);
                grandparent.data = parent.data;
            } else if (imbalance == ImbalancedType.LR) {
                grandparent.left = new BalancedNode<>(parent.data);
                grandparent.right = new BalancedNode<>(grandparent.data, null, grandparent.right);
                grandparent.data = this.data;
            } else if (imbalance == ImbalancedType.RL) {
                grandparent.left = new BalancedNode<>(grandparent.data, grandparent.left, null);
                grandparent.right = new BalancedNode<>(parent.data);
                grandparent.data = this.data;
            } else if (imbalance == ImbalancedType.RR) {
                grandparent.left = new BalancedNode<>(grandparent.data, grandparent.left, null);
                grandparent.right = new BalancedNode<>(this.data);
                grandparent.data = parent.data;
            } else {
                assert false; //unreachable
            }
        }

        private ImbalancedType isImbalanced(BalancedNode<T> parent, BalancedNode<T> grandparent) {
            if (grandparent == null) {
                return ImbalancedType.NONE;
            }
            if (grandparent.right == parent) {
                if (parent.right == this && parent.left == null) {
                    return ImbalancedType.RR;
                }
                if (parent.left == this && parent.right == null) {
                    return ImbalancedType.RL;
                }
            } else { //parent must be in the left branch
                if (parent.right == this && parent.left == null) {
                    return ImbalancedType.LR;
                }
                if (parent.left == this && parent.right == null) {
                    return ImbalancedType.LL;
                }
            }
            return ImbalancedType.NONE;

        }

        @Override
        public void delete(T data) {
            //TODO implement
            throw new UnsupportedOperationException(toString());
        }
    }

}
