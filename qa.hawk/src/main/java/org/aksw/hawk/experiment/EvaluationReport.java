package org.aksw.hawk.experiment;

import org.aksw.hawk.controller.EvalObj;

import java.util.List;

class EvaluationReport {
    private EvaluationMetrics averaged;
    private EvaluationMetrics best;
    private int numberOfEvaluations;

    EvaluationReport(List<List<EvalObj>> evaluations) {
        evaluations
          .forEach(list -> addToReport(list.get(0), list));
    }

    EvaluationMetrics getBest() {
        return best;
    }

    EvaluationMetrics getAveraged() {
        return averaged;
    }

    private void addToReport(EvalObj best, List<EvalObj> allEvaluation) {
        this.best = calculateUpdatedMetrics(this.best, convertToEvaluationMetrics(best), numberOfEvaluations);
        this.averaged = calculateUpdatedMetrics(this.averaged, calculateAverage(allEvaluation), numberOfEvaluations);
        numberOfEvaluations++;
    }

    private EvaluationMetrics convertToEvaluationMetrics(EvalObj eval){
        return new EvaluationMetrics(eval.getPmax(), eval.getRmax());
    }

    private EvaluationMetrics calculateUpdatedMetrics(EvaluationMetrics oldBest, EvaluationMetrics newBest, int numberOfEvaluations) {
        return oldBest == null
          ? newBest
          : new EvaluationMetrics(
                    calculateUpdatedAverage(best.getPrecision(), newBest.getPrecision(), numberOfEvaluations),
                    calculateUpdatedAverage(best.getRecall(), newBest.getRecall(), numberOfEvaluations)
            );
    }

    private EvaluationMetrics calculateAverage(List<EvalObj> evaluations) {
        double precision = evaluations
          .stream()
          .mapToDouble(EvalObj::getPmax)
          .sum();
        double recall = evaluations
          .stream()
          .mapToDouble(EvalObj::getRmax)
          .sum();
       return new EvaluationMetrics(precision / evaluations.size(), recall/evaluations.size());
    }

    private double calculateUpdatedAverage(double oldValue, double newValue, int numberOfOld) {
        return ((oldValue * numberOfOld) + newValue ) / (numberOfOld + 1);
    }
}
