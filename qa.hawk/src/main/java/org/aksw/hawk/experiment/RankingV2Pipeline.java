package org.aksw.hawk.experiment;

import org.aksw.hawk.controller.AbstractPipeline;
import org.aksw.hawk.controller.EvalObj;
import org.aksw.hawk.controller.PipelineStanford;
import org.aksw.hawk.datastructures.Answer;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.hawk.ranking.FeatureBasedRankerV2;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;

import java.util.List;
import java.util.stream.Collectors;

public class RankingV2Pipeline {
    public static void main(String args[]) {
        AbstractPipeline pipeline = new PipelineStanford();

        List<HAWKQuestion> pipelineQuestions = loadQuestions(Dataset.QALD6_Train_Multilingual);

        List<HAWKQuestion> filteredQuestions = pipelineQuestions
                .stream()
                .filter(RankingV2Pipeline::questionOfInterest)
                .collect(Collectors.toList());

        FeatureBasedRankerV2 rankerV2 = new FeatureBasedRankerV2();
        filteredQuestions
                .forEach(q -> {
                    List<Answer> answers = pipeline.getAnswersToQuestion(q);
                    List<Answer> rankedAnswers = rankerV2.rank(answers, q);
                    List<EvalObj> evaluations = Measures.measure(rankedAnswers, q, 10);
                    int a = 4;
                });

    }


    private static boolean questionOfInterest(HAWKQuestion q) {
        return (q.getHybrid() &
                q.getAnswerType().equals("resource") &
                q.getOnlydbo() &
                !q.getAggregation())
                || q.getLoadedAsASKQuery();
    }

    private static List<HAWKQuestion> loadQuestions(Dataset sourceDataset) {
        List<IQuestion> qaldQuestions = LoaderController.load(sourceDataset);
        return HAWKQuestionFactory.createInstances(qaldQuestions);
    }
}
