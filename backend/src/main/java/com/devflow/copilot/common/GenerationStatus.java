package com.devflow.copilot.common;

import java.util.EnumSet;
import java.util.Set;

public enum GenerationStatus {
    GENERATING,
    READY_FOR_REVIEW,
    SAVED,
    CONFIRMED,
    FAILED;

    public boolean canTransitionTo(GenerationStatus target) {
        return switch (this) {
            case GENERATING -> EnumSet.of(READY_FOR_REVIEW, FAILED).contains(target);
            case READY_FOR_REVIEW -> target == SAVED;
            case SAVED -> target == CONFIRMED;
            case CONFIRMED, FAILED -> false;
        };
    }

    public Set<GenerationStatus> allowedTargets() {
        return switch (this) {
            case GENERATING -> EnumSet.of(READY_FOR_REVIEW, FAILED);
            case READY_FOR_REVIEW -> EnumSet.of(SAVED);
            case SAVED -> EnumSet.of(CONFIRMED);
            case CONFIRMED, FAILED -> EnumSet.noneOf(GenerationStatus.class);
        };
    }
}
