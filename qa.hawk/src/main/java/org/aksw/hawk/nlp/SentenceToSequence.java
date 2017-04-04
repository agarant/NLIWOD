package org.aksw.hawk.nlp;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.qa.commons.datastructure.Entity;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class SentenceToSequence {

	static Logger log = LoggerFactory.getLogger(SentenceToSequence.class);

	public static void runPhraseCombination(final HAWKQuestion q, final List<String> tokens, final Map<String, String> label2pos) {
		//remove quotation marks from the sentence, then send it to the combination algo
		List<String> question = Lists.newArrayList();
		String tempSubsequance2 = new String();
		String isNamedEntity = new String(); 
   	        String namedEntity = new String();
		String namedEntityOrCombinedPhrase = new String();
		

		for (int i = 0; i < tokens.size(); i++) 
			{
				String qtoken = tokens.get(i);
		        String qPos = label2pos.get(qtoken);
				if(!qPos.matches("\"|'|''|SYM")|| !qtoken.matches("\"|''"))
				{
				question.add(qtoken);
				}
			}
		// run phrase combination
		int tokenOffset = 0;
		int wordCounter = 0;
		List<String> subsequence = Lists.newArrayList();
		List<String> subsequanceNE = Lists.newArrayList();
		for (int tcounter = 0; tcounter < question.size(); tcounter++) {
			wordCounter += question.get(tcounter).split(" ").length;
			String token = question.get(tcounter);
			String pos = label2pos.get(token);
			String nextPos = (tcounter + 1) == question.size() ? null : label2pos.get(question.get(tcounter + 1));
			String lastPos = tcounter == 0 ? null : label2pos.get(question.get(tcounter - 1));

			if (subsequence.isEmpty()) {
				tokenOffset = wordCounter;
			}


			// look for start "RB|JJ|NN(.)*"
			//add JJS to the list AND add a new condition to prevent addding many|much|old
			if (subsequence.isEmpty() && (null != pos) && (( pos.matches("CD|NN(.)*|RB(.)*")) || (pos.matches("JJ(.)*") && !token.matches("many|much|old") )  )) {
				tempSubsequance2 = tempSubsequance2 + " " + token;
				tempSubsequance2 = tempSubsequance2.trim();
			}
			
			else if (!subsequence.isEmpty() && (null != pos) && lastPos.matches("JJ(.)*|HYPH|NN(.)*") && pos.matches("VB(.)*|IN|WDT") && (null != nextPos) && nextPos.matches("NN(.)*") )
			{
				tempSubsequance2 = tempSubsequance2 + " " + token;
				tempSubsequance2 = tempSubsequance2.trim();
			}
			// split "of the" or "of all" or "against" via pos_i=IN and
			// pos_i+1=DT
			else if (!subsequence.isEmpty() && (null != pos) && ((tcounter + 1) < question.size()) && (null != nextPos) && pos.matches("IN") && !token.matches("of")
			        && nextPos.matches("(W)?DT|NNP(S)?")) {
				           namedEntityOrCombinedPhrase = checkNamedEntites(tempSubsequance2,q);
					   namedEntityOrCombinedPhrase = namedEntityOrCombinedPhrase.replace("[","");
					   namedEntityOrCombinedPhrase = namedEntityOrCombinedPhrase.replace("]","");
					   String a[] = namedEntityOrCombinedPhrase.split("&");
					   isNamedEntity = a[0];	
				           namedEntity = a[1];
				           if (isNamedEntity != null)
				              {
						   if (isNamedEntity.contains("0"))  // NE = CNN => NE
						      {
							   subsequance.add(namedEntity);   
							   transformTree(subsequance, q, tokenOffset); 
						      }

						  else if (isNamedEntity.contains("1")) //NE € CNN => CNN
						          {
								String temp[]=tempSubsequance2.split(" ");
								if (temp.length == 2 && temp[1].matches("of")) {}
							        else {
									subsequanceNE.add(tempSubsequance2);   
							                transformTree(subsequanceNE, q, tokenOffset);
								      } 
							   }
					          else if (isNamedEntity.contains("2")) // CNN ‡ NE => CNN
						           {
								String temp[]=tempSubsequance2.split(" ");
								if (temp.length == 2 && temp[1].matches("of")) {}
							        else 
								    {
									subsequanceNE.add(tempSubsequance2); 
							                transformTree(subsequanceNE, q, tokenOffset);
								    }
									
							    }
                                                 else if(isNamedEntity.contains("3")) {}
					   }
				tempSubsequance2 = new String();
				subsequance = Lists.newArrayList();
				subsequanceNE = Lists.newArrayList();
				tcounter--;				
			}
			// do not combine NNS and NNPS but combine "stage name",
			// "British Prime minister"
			else if (!subsequence.isEmpty() && (null != pos) && (null != lastPos) && lastPos.matches("NNS") && pos.matches("NNP(S)?")) {
				      namedEntityOrCombinedPhrase = checkNamedEntites(tempSubsequance2,q);
				      namedEntityOrCombinedPhrase = namedEntityOrCombinedPhrase.replace("[","");
				      namedEntityOrCombinedPhrase = namedEntityOrCombinedPhrase.replace("]","");
				      String a[] = namedEntityOrCombinedPhrase.split("&");
				      isNamedEntity = a[0];	
				      namedEntity = a[1];
				      if (isNamedEntity != null)
				         {
				         if (isNamedEntity.contains("0"))  // NE = CNN => NE
					    {
						subsequance.add(namedEntity); 
					        transformTree(subsequance, q, tokenOffset); 
					     }
					  else if (isNamedEntity.contains("1")) //NE € CNN => CNN
						   {
						        String temp[]=tempSubsequance2.split(" ");
						        if (temp.length == 2 && temp[1].matches("of")) {}
							else 
							     {
								subsequanceNE.add(tempSubsequance2);   
							        transformTree(subsequanceNE, q, tokenOffset); 
							      }
							 }
					   else if (isNamedEntity.contains("2")) // CNN ‡ NE => CNN
						    {
						           String temp[]=tempSubsequance2.split(" ");
							   if (temp.length == 2 && temp[1].matches("of")) {}
							   else 
							        {
								     subsequanceNE.add(tempSubsequance2); 
								     transformTree(subsequanceNE, q, tokenOffset);
							         }
						      }

                                            else if (isNamedEntity.contains("3")) {}
			           }
				       tempSubsequance2 = new String();
				       subsequance = Lists.newArrayList();
				       subsequanceNE = Lists.newArrayList();
				       tcounter--;
			}
			// finish via VB* or IN -> null or IN -> DT or WDT (now a that or
			// which follows)
			else if (!subsequence.isEmpty() && !lastPos.matches("JJ|HYPH")
			        && ((null == pos) || pos.matches("\\.|WDT") ||(pos.matches("VB(.)*") && nextPos != null) || (pos.matches("IN") && (nextPos == null)) || (pos.matches("IN") && (nextPos == null)) || (pos.matches("IN") && nextPos.matches("DT")))) {
				// more than one token, so summarizing makes sense
					   namedEntityOrCombinedPhrase = checkNamedEntites(tempSubsequance2,q);
					   namedEntityOrCombinedPhrase = namedEntityOrCombinedPhrase.replace("[","");
					   namedEntityOrCombinedPhrase = namedEntityOrCombinedPhrase.replace("]","");
					   String a[] = namedEntityOrCombinedPhrase.split("&");
					   isNamedEntity = a[0];	
				           namedEntity = a[1];
					   if (isNamedEntity != null)
				              {
						   if (isNamedEntity.contains("0"))  // NE = CNN => NE
						      {
							   subsequance.add(namedEntity);   
							   transformTree(subsequance, q, tokenOffset); 
						       }
						    else if (isNamedEntity.contains("1")) //NE € CNN => CNN
						            {
								String temp[]=tempSubsequance2.split(" ");
								if (temp.length == 2 && temp[1].matches("of")) {}
							        else 
								    {
									subsequanceNE.add(tempSubsequance2);   
							                transformTree(subsequanceNE, q, tokenOffset); 
							            }
									
							     }
						   else if (isNamedEntity.contains("2")) // CNN ‡ NE => CNN
						            {
								String temp[]=tempSubsequance2.split(" ");
								if (temp.length == 2 && temp[1].matches("of")) {}
							        else 
								    {
									subsequanceNE.add(tempSubsequance2); 
								        transformTree(subsequanceNE, q, tokenOffset);
							             }
									
							      }
                                                    else if(isNamedEntity.contains("3")) {}
			           }
				       tempSubsequance2 = new String();
				       subsequance = Lists.newArrayList();
				       subsequanceNE = Lists.newArrayList();	
                                       tcounter--;						
			}
			
			//combine phrases that have possessive s. Example: world's most & United States' dominance
			else if (!subsequence.isEmpty() && (null != pos) && ( (pos.matches("POS")) && (null != nextPos) && nextPos.matches("RBS|JJ(.)*|NN(.)*") )){
                                if (pos.matches("POS")) {tempSubsequance2 = tempSubsequance2;} // if (') is combined then no query is generated + token;
                                else tempSubsequance2 = tempSubsequance2 + " " + token;			
			        }
			// continue via "NN(.)*|RB|CD|CC|JJ|DT|IN|PRP|HYPH"
			//remove VBN to avoid situations like (Martin Luther King born) => (Martin Luther King)
			else if (!subsequence.isEmpty() && (null != pos) && pos.matches("NN(.)*|RB(.)*|CD|CC|JJ(.)*|DT|IN|PRP|HYPH")) {
                                 tempSubsequance2 = tempSubsequance2 + " " + token;
			} else {
                                   subsequance.add(tempSubsequance2); 
				   tempSubsequance2 = new String();
				   subsequance = Lists.newArrayList();			
			        }
		        }
		log.debug(q.getLanguageToNounPhrases().toString());
	}

	
	    public static String checkNamedEntites(String tempSubsequance2, final HAWKQuestion q)
	{
		String 	stringSubsequance = ""+ tempSubsequance2;
		stringSubsequance = stringSubsequance.replace("[",""); 
		stringSubsequance = stringSubsequance.replace("]",""); 
		int count = 0;
		int k=0;
		String check="CASE 3 & & ";
       	        List<Entity> NamedEntity = q.getLanguageToNamedEntites().get("en");
		String stringSubsequanceList[]=stringSubsequance.split(" "); 
		String NE = ""+NamedEntity; 
		NE = NE.replace("),",")%");
	        NE = NE.replace("[","");
		NE = NE.replace("]","");  
		String NEList[] = NE.split("%");
		int stringSubsequanceListLength=stringSubsequanceList.length;
		String tempCheck[] =new String[NEList.length];
		int temp[]=new int[NEList.length];
		    
		if (!stringSubsequance.isEmpty() && !NE.isEmpty() && !NE.matches("null"))
		   { 
	              int z=1;
		      while (k < NEList.length && (z%2 != 0) )
	                    {
			       z++;
			       String phrase = NEList[k].replace("(","(%");
			       String phraseList[] = phrase.split("%"); 
			       phraseList[0] = phraseList[0].trim();
			       phraseList[1] = phraseList[1].replace("uri","").replace("; type: )","");													
			       String namedEntityList = phraseList[0].replace("(","");
			       namedEntityList = namedEntityList.replace(" ","%");
			       String namedEntity[] = namedEntityList.split("%");
			       String namedEntityURI = phraseList[1];
			       if (namedEntityURI.startsWith(":")) {namedEntityURI = namedEntityURI.substring(1);}
			       namedEntityURI = namedEntityURI.replaceAll("uri.*?http","http").replace("; type: )","").trim(); 
			       for (int j=0; j < namedEntity.length; j++)
		                   {
				     for(int i=0; i < stringSubsequanceList.length; i++)
					{
					   if (stringSubsequanceList[i].equals(namedEntity[j]))
			                      { 
		                                 count++;
				               }	
					 }
				     }
			      if (count > 0)
                                 {
                                     if (stringSubsequanceList.length == namedEntity.length && namedEntity.length == count)
                                         {tempCheck[k] = "CASE 0" +"&"+ namedEntityList.replace("%"," ") +"%"+ namedEntityURI;} // CNN = NE => NE	
                                     else{ tempCheck[k] = "CASE 1" + "&"+ namedEntityList.replace("%"," ") +"%"+ namedEntityURI;}
                                  }	
                               else if (stringSubsequanceList.length == 1) { tempCheck[k] = "CASE 3" + "&"+ namedEntityList.replace("%"," ") +"%"+ namedEntityURI;}
				        
				     else {tempCheck[k] = "CASE 2" +"&"+ namedEntityList.replace("%"," ") +"%"+ namedEntityURI; }
					 check = tempCheck[k];
					 if (tempCheck[k].contains("0") || tempCheck[k].contains("1"))  { return tempCheck[k]; }
					 else if (NEList.length == k)  { return tempCheck[k]; }
					       else if (tempCheck[k].contains("2") || tempCheck[k].contains("3")) { z++; k++; }
				}
			
		}
		else if(stringSubsequanceListLength > 1 && ( NE.isEmpty() || NE.matches("null")) )
		       {
				   check = "CASE 2 & %"; 
				   return check;
			}
			   else if(stringSubsequanceListLength == 1 && ( NE.isEmpty() || NE.matches("null")))
			          {
						  check = "CASE 3 & %"; 
				          return check;
				   }
		return check;
        }
			   
	public static void transformTree(final List<String> subsequence, final HAWKQuestion q, final int subsequenceStartOffset) {
		String isNamedEntity = ""+subsequance;
		String combinedNN = null;
		String combinedURI = null;
		if (isNamedEntity.contains("resource")) 
		   {
			String namedEntity = ""+subsequance; 
			namedEntity = namedEntity.replace("[","");
			namedEntity = namedEntity.replace("]","");
			String namedEntityLabelUri[] = namedEntity.split("%");
			combinedNN = namedEntityLabelUri[0];
		        combinedURI = namedEntityLabelUri[1].trim();
			}
		else 
			{
		                combinedNN = Joiner.on(" ").join(subsequance);
		                combinedURI = "http://aksw.org/combinedNN/" + Joiner.on("_").join(subsequance);
			}

		Entity tmpEntity = new Entity();
		tmpEntity.setOffset(subsequenceStartOffset);

		tmpEntity.setLabel(combinedNN);

		tmpEntity.getUris().add(new ResourceImpl(combinedURI));

		List<Entity> nounphrases = q.getLanguageToNounPhrases().get("en");
		if (null == nounphrases) {
			nounphrases = Lists.newArrayList();
		}
		nounphrases.add(tmpEntity);
		q.getLanguageToNounPhrases().put("en", nounphrases);

	}

	public static void resolveCompoundNouns(final MutableTree tree, final List<Entity> list) {

		Stack<MutableTreeNode> stack = new Stack<>();
		stack.push(tree.getRoot());
		while (!stack.isEmpty()) {

			MutableTreeNode thisNode = stack.pop();
			String label = thisNode.label;
			if (label.contains("aksw.org")) {
				thisNode.label = Joiner.on(" ").join(label.replace("http://aksw.org/combinedNN/", "").split("_"));
				thisNode.posTag = "CombinedNN";
			}
			for (MutableTreeNode child : thisNode.getChildren()) {
				stack.push(child);
			}
		}

	}

}
