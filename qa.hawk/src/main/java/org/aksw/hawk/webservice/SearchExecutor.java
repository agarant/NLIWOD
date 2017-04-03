package org.aksw.hawk.webservice;

import com.google.common.base.Joiner;
import org.aksw.hawk.controller.AbstractPipeline;
import org.aksw.hawk.datastructures.Answer;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.ranking.FeatureBasedRanker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Component
@Service("searchExecutor")
public class SearchExecutor {
	private AbstractPipeline pipeline;
	private Logger log = LoggerFactory.getLogger(SearchExecutor.class);


	public void setPipeline(final AbstractPipeline pipeline) {
		this.pipeline = pipeline;
	}

	public String runPipeline(final String question) {
		HAWKQuestion q = new HAWKQuestion();
		q.getLanguageToQuestion().put("en", question);
		log.info("Run pipeline on " + q.getLanguageToQuestion().get("en"));
		List<Answer> answers = pipeline.getAnswersToQuestion(q);

		FeatureBasedRanker feature_ranker = new FeatureBasedRanker();
		feature_ranker.train();
		log.info("Bucket-based ranking");
		List<Answer> rankedAnswer = feature_ranker.rank(answers, q);
		log.info(Joiner.on("\n\t").join(rankedAnswer));
		q.setFinalAnswer(rankedAnswer);
		return q.getJSONStatus();
	}

}
