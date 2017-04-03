package org.aksw.hawk.querybuilding;


import org.aksw.hawk.datastructures.Answer;
import org.aksw.qa.commons.sparql.SPARQL;
import org.aksw.qa.commons.sparql.SPARQLQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class SparqlQueryRunner {

  private static Logger log = LoggerFactory.getLogger(SparqlQueryRunner.class);
  private SPARQL sparqlEndpoint;

  public SparqlQueryRunner(SPARQL sparql) {
    this.sparqlEndpoint = sparql;
  }

  public List<Answer> run(Set<SPARQLQuery> queries) {
    return queries.stream()
      .map(SPARQLQuery::generateQueries)
      .flatMap(Collection::stream)
      .map(this::findAnswer)
      .collect(Collectors.toList());
  }

  private Answer findAnswer(String queryString) {
    Answer a = new Answer();
    a.answerSet = sparqlEndpoint.sparql(queryString);
    a.queryString = queryString;
    return a;
  }


}
