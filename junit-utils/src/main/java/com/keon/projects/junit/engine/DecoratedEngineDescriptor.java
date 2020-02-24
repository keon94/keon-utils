package com.keon.projects.junit.engine;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

class DecoratedEngineDescriptor extends EngineDescriptor implements Node<JupiterEngineExecutionContext> {

    private final JupiterEngineDescriptor descriptor;

    DecoratedEngineDescriptor(final JupiterEngineDescriptor descriptor) {
        super(descriptor.getUniqueId(), "JUnit Keon");
        this.descriptor = descriptor;
        super.children.addAll(descriptor.getChildren());
    }

    JupiterConfiguration getConfiguration() {
        return descriptor.getConfiguration();
    }

    @Override
    public ExecutionMode getExecutionMode() {
        return descriptor.getExecutionMode();
    }

    @Override
    public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) {
        return descriptor.prepare(context);
    }

    @Override
    public void cleanUp(JupiterEngineExecutionContext context) throws Exception {
        descriptor.cleanUp(context);
    }
}
