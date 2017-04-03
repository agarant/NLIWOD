package org.aksw.hawk.webservice;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.aksw.hawk.controller.PipelineStanford;
import org.aksw.hawk.datastructures.Answer;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.ranking.FeatureBasedRanker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

@Service("asyncSearchExecutor")
public class AsyncSearchExecutor {
	private PipelineStanford pipeline = new PipelineStanford();
	private Logger log = LoggerFactory.getLogger(AsyncSearchExecutor.class);

	@Async
	public Future<HAWKQuestion> search(HAWKQuestion q) {
		log.info("Run pipeline on " + q.getLanguageToQuestion().get("en"));
		List<Answer> answers = pipeline.getAnswersToQuestion(q);

		// FIXME improve ranking, put other ranking method here
		// bucket-based ranking
		FeatureBasedRanker feature_ranker = new FeatureBasedRanker();
		for (Set<FeatureBasedRanker.Feature> featureSet : Sets.powerSet(new HashSet<>(Arrays.asList(FeatureBasedRanker.Feature.values())))) {
			if (!featureSet.isEmpty()) {
				feature_ranker.setFeatures(featureSet);
				feature_ranker.train();
				log.info("Bucket-based ranking");
//		List<Answer> rankedAnswer = bucket_ranker.rank(answers, q);
				List<Answer> rankedAnswer = feature_ranker.rank(answers, q);
				log.info(Joiner.on("\n\t").join(rankedAnswer));
				q.setFinalAnswer(rankedAnswer);
			}
		}
//		BucketRanker bucket_ranker = new BucketRanker();


		return new AsyncResult<HAWKQuestion>(q);
	}
}