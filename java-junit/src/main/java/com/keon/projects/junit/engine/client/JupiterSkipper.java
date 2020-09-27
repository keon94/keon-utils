package com.keon.projects.junit.engine.client;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;


class JupiterSkipper implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext context) {
        if (context.getRoot().getUniqueId().contains(JupiterEngineDescriptor.ENGINE_ID)) {
            return ConditionEvaluationResult.disabled("Test disabled for Jupiter engine");
        }
        return ConditionEvaluationResult.enabled("");
    }

}