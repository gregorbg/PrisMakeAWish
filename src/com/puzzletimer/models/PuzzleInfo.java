package com.puzzletimer.models;

import static com.puzzletimer.Internationalization.i18n;

public class PuzzleInfo {
    private final String puzzleId;

    public PuzzleInfo(String puzzleId) {
        this.puzzleId = puzzleId;
    }

    public String getPuzzleId() {
        return this.puzzleId;
    }

    public String getDescription() {
        return i18n("puzzle." + this.puzzleId);
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
