package org.aksw.hawk.ranking;

import com.google.common.collect.Sets;
import org.aksw.hawk.cache.StorageHelper;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.sparql.SPARQLQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;

public class FeatureBasedRankerDB {
	private static Logger log = LoggerFactory.getLogger(FeatureBasedRankerDB.class);
	private static String RANKING_FOLDER = "/home/agarant/GIT/test/NLIWOD/ranking/";

	// TODO Christian make it independent of OS
	public static Set<SPARQLQuery> readRankings() {
		Set<SPARQLQuery> set = Sets.newHashSet();
		for (File f : new File(RANKING_FOLDER).listFiles()) {
			log.debug("Reading file for ranking: " + f);
			set.add((SPARQLQuery) StorageHelper.readFromFileSavely(f.toString()));
		}
		return set;
	}

	/**
	 * stores a question
	 * 
	 */
	public static void store(IQuestion q, Set<SPARQLQuery> queries) {
		for (SPARQLQuery query : queries) {
			int hash = query.hashCode();
			String serializedFileName = getFileName(hash);
			// File tmp = new File(serializedFileName);
			StorageHelper.storeToFileSavely(query, serializedFileName);

		}
	}

	private static String getFileName(int hash) {
		String serializedFileName = RANKING_FOLDER + hash + ".question";
		return serializedFileName;
	}
}
