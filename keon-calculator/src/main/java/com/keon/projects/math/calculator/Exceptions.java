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

class MisplacedOperatorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    MisplacedOperatorException(final String msg) {
        super(msg);
    }
    
    MisplacedOperatorException(final String msg, final Throwable t) {
        super(msg, t);
    }
}

class ArgumentCountException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ArgumentCountException(final String msg) {
        super(msg);
    }
    
    ArgumentCountException(final String msg, final Throwable t) {
        super(msg, t);
    }
}
