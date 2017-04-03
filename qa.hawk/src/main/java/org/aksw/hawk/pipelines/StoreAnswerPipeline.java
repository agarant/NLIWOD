package org.aksw.hawk.pipelines;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aksw.hawk.controller.AbstractPipeline;
import org.aksw.hawk.controller.PipelineStanford;
import org.aksw.hawk.datastructures.Answer;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.sparql.SPARQLQuery;
import org.apache.jena.rdf.model.RDFNode;

import java.util.List;
import java.util.stream.Stream;

/** The goal of this pipeline is to be able to get all the possible answers to a dataset without doing ranking
 * on them, and saving them to a file. This makes it easier to experiment with the ranker as we can simply load
 * answers without having to run the whole pipeline **/
public class StoreAnswerPipeline {


  public static void main(String args[]) {
    AbstractPipeline pipeline = new PipelineStanford();

    Stream<HAWKQuestion> pipelineQuestions = loadQuestions(Dataset.QALD6_Train_Multilingual)
      .stream()
      .filter(StoreAnswerPipeline::questionOfInterest)
      .limit(1);

    pipelineQuestions
      .forEach(q -> {
        List<Answer> answers = pipeline.getAnswersToQuestion(q);
        storeAnswers(answers, q);
      });
  }

  private static void storeAnswers(List<Answer> answers, HAWKQuestion question) {
    ObjectMapper om = new ObjectMapper();
    try {
      for (Answer answer : answers) {
        for (RDFNode node : answer.answerSet) {
          String nodeStr = om.writeValueAsString(node);
          String a = "ALLO";
        }
        String querySeria = om.writeValueAsString(answer.query);
        SPARQLQuery deseQuery = om.readValue(querySeria, SPARQLQuery.class);
        String serialized = om.writeValueAsString(answer);
        String b = "SALUT";
      }
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
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
