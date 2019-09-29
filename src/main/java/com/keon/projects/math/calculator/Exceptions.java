package com.keon.projects.math.calculator;

class UnbalancedParanthesisException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    UnbalancedParanthesisException(final String msg) {
        super(msg);
    }

}

class MalformedFunctionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    MalformedFunctionException(final String msg) {
        super(msg);
    }
}
