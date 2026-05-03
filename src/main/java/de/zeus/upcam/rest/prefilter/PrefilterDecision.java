package de.zeus.upcam.rest.prefilter;

public class PrefilterDecision {
    public enum Action { ACCEPT, NOISE, DROP }

    private final Action action;
    private final String reason;
    private final String hashHex;
    private final int hammingDistance;
    private final double motionScore;
    private final double edgeDiffRatio;
    private final double foregroundRatio;
    private final int foregroundArea;
    private final int largestComponentArea;
    private final String cameraSignalSummary;
    private final boolean cameraSignalActive;

    public PrefilterDecision(Action action, String reason, String hashHex, int hammingDistance) {
        this(action, reason, hashHex, hammingDistance, 0.0, 0.0, 0.0, 0, 0, "", false);
    }

    public PrefilterDecision(Action action,
                             String reason,
                             String hashHex,
                             int hammingDistance,
                             double motionScore,
                             double edgeDiffRatio,
                             double foregroundRatio,
                             int foregroundArea,
                             int largestComponentArea,
                             String cameraSignalSummary,
                             boolean cameraSignalActive) {
        this.action = action;
        this.reason = reason;
        this.hashHex = hashHex;
        this.hammingDistance = hammingDistance;
        this.motionScore = motionScore;
        this.edgeDiffRatio = edgeDiffRatio;
        this.foregroundRatio = foregroundRatio;
        this.foregroundArea = foregroundArea;
        this.largestComponentArea = largestComponentArea;
        this.cameraSignalSummary = cameraSignalSummary == null ? "" : cameraSignalSummary;
        this.cameraSignalActive = cameraSignalActive;
    }

    public Action getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public String getHashHex() {
        return hashHex;
    }

    public int getHammingDistance() {
        return hammingDistance;
    }

    public double getMotionScore() {
        return motionScore;
    }

    public double getEdgeDiffRatio() {
        return edgeDiffRatio;
    }

    public double getForegroundRatio() {
        return foregroundRatio;
    }

    public int getForegroundArea() {
        return foregroundArea;
    }

    public int getLargestComponentArea() {
        return largestComponentArea;
    }

    public String getCameraSignalSummary() {
        return cameraSignalSummary;
    }

    public boolean isCameraSignalActive() {
        return cameraSignalActive;
    }
}
