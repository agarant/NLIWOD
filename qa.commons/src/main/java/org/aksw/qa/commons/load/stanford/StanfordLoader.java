package org.aksw.qa.commons.load.stanford;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.datastructure.Question;

public class StanfordLoader {
	/**
	 * loads both the Stanford dev and train dataset
	 * 
	 * @param is
	 *            an InputStream containing the dataset
	 * @return a list of Questions with id, question string and a set of answer
	 *         strings
	 * 
	 */
	// TODO Jonathan System.out weg und logging einbauen
	public static List<IQuestion> load(InputStream is) {
		List<IQuestion> output = new ArrayList<>();
		JsonReader jsonReader = Json.createReader(is);
		JsonObject mainJsonObject = jsonReader.readObject();
		JsonArray dataArray = mainJsonObject.getJsonArray("data");
		System.out.println("Number of WikiArticles " + dataArray.size());
		dataArray.forEach(article -> {
			JsonArray contexts = ((JsonObject) article).getJsonArray("paragraphs");
			contexts.forEach(paragraph -> {
				JsonObject jsonObject = (JsonObject) paragraph;
				JsonArray qas = jsonObject.getJsonArray("qas");
				qas.forEach(x -> {
					IQuestion q = new Question();
					JsonString question = ((JsonObject) x).getJsonString("question");
					// FIXME Micha, geht das hier nicht schöner mit Java 8?
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("en", question.getString());
					q.setLanguageToQuestion(map);

					JsonString id = ((JsonObject) x).getJsonString("id");
					q.setId(id.getString());

					JsonArray answers = ((JsonObject) x).getJsonArray("answers");
					HashSet<String> goldenAnswers = new HashSet<String>();
					answers.forEach(y -> {
						JsonNumber answerStart = ((JsonObject) y).getJsonNumber("answer_start");
						JsonString text = ((JsonObject) y).getJsonString("text");
						if (text == null) {// TODO proper logging
							System.out.println("NULL");
						}
						goldenAnswers.add(text != null ? text.getString().trim() : null);
					});
					// TODO log.debu instead of system.out
					// if (goldenAnswers.size() > 1) {
					// goldenAnswers.forEach(System.out::print);
					// }
					q.setGoldenAnswers(goldenAnswers);
					output.add(q);
				});
			});

		});
		System.out.println("Number of Questions: " + output.size());

		List<IQuestion> result = output.stream().filter(x -> x.getGoldenAnswers().size() > 1).collect(Collectors.toList());
		System.out.println("Number of Questions with more than one answer: " + result.size());

		return output;
	}
}