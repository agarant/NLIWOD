package org.aksw.hawk.experiment;

public class EvaluationMetrics {
    private double precision;
    private double recall;
    private double fMeasure;

    EvaluationMetrics(double precision, double recall) {
        this.precision = precision;
        this.recall = recall;
        this.fMeasure = calculateFMeasure(precision, recall);
    }

    private double calculateFMeasure(double precision, double recall) {
        double fMeasure = 0;
        if ((precision + recall) > 0) {
            fMeasure = (2 * precision * recall) / (precision + recall);
        }
        return fMeasure;
    }

    public double getFMeasure() {
        return fMeasure;
    }

    public double getRecall() {
        return recall;
    }
    public double getPrecision() {
        return precision;
    }
}
