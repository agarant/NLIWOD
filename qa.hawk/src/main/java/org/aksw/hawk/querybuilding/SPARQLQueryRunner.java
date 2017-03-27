package org.aksw.hawk.querybuilding;

import java.util.List;
import java.util.Set;
import java.util.Collecttion;
import java.util.stream.Collectors;


import org.aksw.hawk.datastructures.Answer;
import org.aksw.qa.commons.sparql.SPARQL;
import org.aksw.qa.commons.sparql.SPARQLQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SPARQLQueryRunner {
	private static Logger log = LoggerFactory.getLogger(SPARQLQueryRunner.class);
	private SPARQL sparqlEndpoint;
	
	public SPARQLQueryRunner(SPARQL sparql) {
		this.sparql = sparql;
			}

	public List<Answer> run(Set<SPARQLQuery> queries) {
		return queries.stream()
			.map(SPARQLQuery::generateQueries)
			.flatMap(Collection::stream)
			.map(this::findAnswer)
			.collect(Collectors.toList());
	}
	
	private Answer findAnswer(String queryString)
	{
		Answer a = new Answer();
		a.answerSet = sparqlEndpint.sparql(queryString);
		a.queryString = queryString;
		return a;
	}

}
