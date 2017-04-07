package org.aksw.hawk.querybuilding;

import com.google.common.collect.Sets;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.nlp.MutableTreeNode;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.qa.commons.sparql.SPARQLQuery;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SparqlQueryBuilder {
	private static Logger log = LoggerFactory.getLogger(SparqlQueryBuilder.class);

	public static Set<SPARQLQuery> build(final HAWKQuestion q) {
		SPARQLQuery initialQuery = new SPARQLQuery();
		initialQuery.isASKQuery(q.getIsClassifiedAsASKQuery());
		List<SPARQLQuery> rootQueries = new ArrayList<>();
		rootQueries.add(initialQuery);
    MutableTreeNode root = q.getTree().getRoot();
    return generateSparqlQueries(root, rootQueries)
      .collect(Collectors.toSet());
	}

	private static Stream<SPARQLQuery> generateSparqlQueries(MutableTreeNode node, List<SPARQLQuery> parentQueries) {
	  List<String> constraints = generateNodeConstraints(node);
	  List<Filter> filters = generateNodeFilters(node);

	  List<SPARQLQuery> currentNodeQueries = new ArrayList<>();
	  if (node.posTag.matches("WP"))  {
      // for Who and What
	    currentNodeQueries.addAll(parentQueries);
    }

	  for (SPARQLQuery query : parentQueries) {
      extendQueryWithConstraints(currentNodeQueries, query, constraints);
      extendQueryWithFilters(currentNodeQueries, query, filters);
    }

    Stream<SPARQLQuery> childrenQueries = node
      .getChildren()
      .stream()
      .flatMap(child -> generateSparqlQueries(child, currentNodeQueries));

    return Stream.concat(currentNodeQueries.stream(), childrenQueries);
  }

  private static void extendQueryWithConstraints(List<SPARQLQuery> list, SPARQLQuery query, List<String> constraints) {
    constraints
      .forEach(constraint -> {
        SPARQLQuery variant = null;
        try {
          variant = ((SPARQLQuery) query.clone());
          variant.addConstraint(constraint);
        } catch (CloneNotSupportedException e) {
          log.error("Exception while extending query with constraints", e);
        }
        if (variant != null) list.add(variant);
      });
  }

  private static void extendQueryWithFilters(List<SPARQLQuery> list, SPARQLQuery query, List<Filter> filters) {
    filters
      .forEach(filter -> {
        SPARQLQuery variant = null;
        try {
          variant = ((SPARQLQuery) query.clone());
          variant.addFilterOverAbstractsContraint(filter.getVariable(), filter.getLabel());
        } catch (CloneNotSupportedException e) {
          log.error("Exception while extending query with filters", e);
        }
        if (variant != null) list.add(variant);
      });
  }

	private static List<String> generateNodeConstraints(MutableTreeNode node) {
    return node.getAnnotations().size() > 0
      ? nonEmptyAnnotationsConstraintsGeneration(node)
      : emptyAnnotationsConstraintsGeneration(node);
  }

  private static List<String> nonEmptyAnnotationsConstraintsGeneration(MutableTreeNode node) {
    return node.getAnnotations()
      .stream()
      .flatMap(annotation -> {
        if (node.posTag.matches("VB(.)*")) {
          return Stream.of(
            "?proj <" + annotation + "> ?const.",
            "?const <" + annotation + "> ?proj.",
            "?const ?proot ?proj."
          );
        } else if (node.posTag.matches("NN(.)*|WRB")) {
          return Stream.of(
            "?const <" + annotation + "> ?proj.",
            "?const a <" + annotation + ">.",
            "?proj a <" + annotation + ">."
          );
        } else if (node.posTag.matches("WP")) {
          return Stream.of(
            "?const a <" + annotation + ">.",
            "?proj a <" + annotation + ">."
          );
        } else {
          log.error("Tmp: " + node.label + " pos: " + node.posTag);
          return Stream.empty();
        }
      })
      .collect(Collectors.toList());
  }

  private static List<String> emptyAnnotationsConstraintsGeneration(MutableTreeNode node) {
    List<String> generatedList = new ArrayList<>();
    if (node.posTag.matches("ADD")) {
      generatedList.add("?proj ?pbridge <" + node.label + ">.");
    }
    return generatedList;
  }

  private static List<Filter> generateNodeFilters(MutableTreeNode node) {
	  return node.getAnnotations().size() > 0
      ? nonEmptyAnnotationsFilterGenerations(node)
      : emptyAnnotationsFiltersGeneration(node);
  }

  private static List<Filter> nonEmptyAnnotationsFilterGenerations(MutableTreeNode node) {
    List<Filter> generatedList = new ArrayList<>();
    if (node.posTag.matches("NN(.)*|WRB")) {
        generatedList.add(new Filter("?proj", node.label));
        generatedList.add(new Filter("?const", node.label));
    }
    return generatedList;
  }

  private static List<Filter> emptyAnnotationsFiltersGeneration(MutableTreeNode node) {
    String tag = node.posTag;
    List<Filter> generatedList = new ArrayList<>();
    if (tag.matches("CombinedNN|NNP(.)*|JJ|CD") ||
      tag.matches("VB(.)*") ||
      tag.matches("NN|NNS")
      ) {
      generatedList.add(new Filter("?proj", node.label));
      generatedList.add(new Filter("?const", node.label));
    }
    else if (tag.matches("ADD")) {
      /* TODO hack query for correct label of node ie Cleopatra
       * can be undone when each ADD node knows is original label*/
      getOrigLabels(node.label).forEach(label -> {
          generatedList.add(new Filter("?proj", label));
          generatedList.add(new Filter("?const", label));
      });
    }
    return generatedList;
  }

	// TODO refactor to use SPAQRL.java instead of creating a stand-alone
	// execution factory
  private static Set<String> getOrigLabels(final String label) {
    Set<String> resultset = Sets.newHashSet();
    String query = "SELECT str(?proj)  WHERE { <" + label + "> <http://www.w3.org/2000/01/rdf-schema#label> ?proj. FILTER(langMatches( lang(?proj), \"EN\" ))}";
    try {
      QueryExecutionFactory qef = new QueryExecutionFactoryHttp("http://139.18.2.164:3030/ds/sparql");
      QueryExecution qe = qef.createQueryExecution(query);
      if (qe != null) {
        log.debug(query.toString());
        ResultSet results = qe.execSelect();
        while (results.hasNext()) {
          QuerySolution next = results.next();
          String varName = next.varNames().next();
          resultset.add(next.get(varName).toString());
        }
      }
    } catch (Exception e) {
      log.error(query.toString(), e);
    }
    return resultset;
  }

	private static class Filter {
	  private String variable;
	  private String label;

	  Filter(String variable, String label) {
	    this.variable = variable;
	    this.label = label;
    }

    String getVariable() {
	    return this.variable;
    }

    String getLabel() {
	    return this.label;
    }
  }
}
