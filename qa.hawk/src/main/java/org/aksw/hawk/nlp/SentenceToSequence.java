package org.aksw.hawk.nlp;

import static com.google.common.collect.Lists.newArrayList;
import java.util.ArrayList;
import java.lang.*;
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
		public final String newq2="";


	public static void runPhraseCombination(final HAWKQuestion q, final List<String> tokens, final Map<String, String> label2pos) {
		//remove quotation marks from the sentence, then send it to the combination algo
		// ignor repeated words when they occurre after each other
		// Ex: QALD7, ID 49. Lamb and Harmon => Lamb Lamb and Harmon Harmon
		List<String> question = Lists.newArrayList();
		for (int i = 0; i < tokens.size(); i++) 
			{  
     			int z = i-1;
				String LastToken = null;
				if (i > 0) {LastToken = tokens.get(z);}
				String qtoken = tokens.get(i);
		        String qPos = label2pos.get(qtoken);
				if(!qtoken.matches("\"|''|'") && !qtoken.equals(LastToken))
				{
				question.add(qtoken);
				}
			}

		// run phrase combination
		int tokenOffset = 0;
		int wordCounter = 0;
		List<String> subsequance = Lists.newArrayList();
		
		for (int tcounter = 0; tcounter < question.size(); tcounter++) 
		{
			wordCounter += question.get(tcounter).split(" ").length;
			String token = question.get(tcounter);
			String pos = label2pos.get(token);
			String nextPos = (tcounter + 1) == question.size() ? null : label2pos.get(question.get(tcounter + 1));
			String lastPos = tcounter == 0 ? null : label2pos.get(question.get(tcounter - 1));
			String lastToken = tcounter == 0 ? null : question.get(tcounter - 1);

			if (subsequance.isEmpty()) 
			   {
				 tokenOffset = wordCounter;
			   }
			// look for start "CD|NN(.)*|RB(.)*|JJ(.)*"
			if (subsequance.isEmpty() && (null != pos) && ( (pos.matches("CD|NN(.)*|RB(.)*")) || (pos.matches("JJ(.)*") && !token.matches("many|much|old") )))			
               {
				 subsequance.add(token);  

   			   }
				
			//pos(t-1) => (JJ(.)* | HYPH | NN(.)* ) && pos => (VB(.)* | IN | WDT) && pos(t+1)=> (NN(.)*)
			//father of Singapore => JJ IN NN
			//book by William => NN  IN NNP
			else if (!subsequance.isEmpty() && (null != pos) && lastPos.matches("JJ(.)*|HYPH|NN(.)*") && pos.matches("VB(.)*|IN|WDT") && (null != nextPos) && nextPos.matches("NN(.)*") )
			        { 
					  subsequance.add(token);  
			        }
			
			// split "of the" or "of all" or "against" via pos_i=IN and
			// pos_i+1=DT
			else if (!subsequance.isEmpty() && (null != pos) && ((tcounter + 1) < question.size()) && (null != nextPos) && pos.matches("IN") && !token.matches("of")
			        && nextPos.matches("(W)?DT|NNP(S)?")) 
					{ 
					// call the checkNamedEntites methode to check if the combined phrases matches or contains a recognized named entity
					   checkNamedEntites(subsequance,q,tokenOffset);
				       subsequance = Lists.newArrayList();				   
					}
				   
			// do not combine NNS and NNPS but combine "stage name",
			// "British Prime minister"
			else if (!subsequance.isEmpty() && (null != pos) && (null != lastPos) && lastPos.matches("NNS") && pos.matches("NNP(S)?")) 
					{
						checkNamedEntites(subsequance,q,tokenOffset);
				        subsequance = Lists.newArrayList();		  
			       }
			
			// finish via VB* or IN -> null or IN -> DT or WDT (now a that or
			// which follows)
			else if (!subsequance.isEmpty() && !lastPos.matches("JJ(.)*|HYPH")
			        && ((null == pos) || pos.matches("\\.|WDT") ||(pos.matches("VB(.)*") && nextPos != null) || (pos.matches("IN") && (nextPos == null)) || (pos.matches("IN") && nextPos.matches("DT")))) 
					{
						checkNamedEntites(subsequance,q,tokenOffset);
				       subsequance = Lists.newArrayList();
				    }

			//combine phrases that have possessive s. Example: world's most & United States' dominance
			else if (!subsequance.isEmpty() && (null != pos) && ( (pos.matches("POS")) && (null != nextPos) && nextPos.matches("RBS|JJ(.)*|NN(.)*") ))
			        {		          
                        if (pos.matches("POS")) {} //{subsequance.add(token); } // if (') is combined then no query is generated, Temperarly ignore POS tag while combining
                        else  subsequance.add(token);  
		            }
			
			// continue via "NN(.)*|RB|CD|CC|JJ|DT|IN|PRP|HYPH"
			// remove VBN to avoid situations like (Martin Luther King born) => (Martin Luther King)
			else if (!subsequance.isEmpty() && (null != pos) && pos.matches("NN(.)*|RB(.)*|CD|CC|JJ(.)*|DT|IN|PRP|HYPH")) 
			        {						
				         subsequance.add(token);  
					} 
			else 
				{				       			
				       subsequance = Lists.newArrayList();
				}	
		}		
		log.debug(q.getLanguageToNounPhrases().toString());
	}
	 
	
    public static void checkNamedEntites(final List<String> subSequance, final HAWKQuestion q, int tokenOffset)
	{
	 // This method checks if the combined phrases (subSequance) matches or contains any of the recognized named entity from the given question
	 // and then send a string, that contains: the case number, subSequance(CNN/NE) & URI, to transformTree( , ,) to add it to the list of nounPhrases

        // subSequance is the combined phrases to be checked
		String 	stringSubsequance = ""+ subSequance;
		stringSubsequance = stringSubsequance.replace(",","").replace("[","").replace("]","").replace(" 's","'s"); 
		// Retrive all the recognized Named Entities and store them in list
       	List<Entity> NamedEntity = q.getLanguageToNamedEntites().get("en");
		String NE = ""+NamedEntity; 
		NE = NE.replace("),",")%").replace("[","").replace("]",""); 
		String NEList[] = NE.split("%");
		String tempCheck[] =new String[NEList.length];
		int temp[]=new int[NEList.length];

		int k = 0;
		String check = new String();
		List<String> subsequance = Lists.newArrayList(); 


		if (!stringSubsequance.isEmpty() && !NE.isEmpty() && !NE.matches("null"))
		   { 
	        int z=1;
			while (k < NEList.length && (z%2 != 0) )
	              {	// From the list of Named Entities, compare one NE with the CNN at a time 
			  		System.out.print("\n NEList.length:"+NEList.length+", z:"+z);  // Tue -
 			        z++;			
					String phrase = NEList[k].replace("(","(%");
			        String phraseList[] = phrase.split("%"); 
					// NE
					phraseList[0] = phraseList[0].trim();
                    String namedEntityList = phraseList[0].replace("(","");				
					// NE URI
					phraseList[1] = phraseList[1].replace("uri","").replace("; type: )",""); 															        
			        String namedEntityURI = phraseList[1];
					
					if (namedEntityURI.startsWith(":")){namedEntityURI = namedEntityURI.substring(1);}
			        namedEntityURI = namedEntityURI.replaceAll("uri.*?http","http").replace("; type: )","").trim();			  
						
						System.out.print("\n stringSubsequance :"+stringSubsequance+",namedEntityList :"+namedEntityList); 
                       if (stringSubsequance.matches(namedEntityList))
                          {
							  // Combined phrases match a recognized Named Entity
							   check= namedEntityList +"%"+ namedEntityURI; 
							   subsequance.add(check); 
 							   transformTree(subsequance, q, tokenOffset);
							   check = new String();
				               subsequance = Lists.newArrayList();
						  } 	
						  else if (NEList.length != k+1) 
						          { 
									  z++; k++;
								  } 
								  else 
								     {
										 if (stringSubsequance.length() == 1) // there is no matching bettwen CNN and NE
				                              {}
									     else  
					                     {  //Combined phrases contain a recognized Named Entity  
									        //OR Combined phrases don't match or contain a recognized Named Entity
								            if (checkOf(stringSubsequance))
									            { 
											      subsequance.add(stringSubsequance); 
 							                      transformTree(subsequance, q, tokenOffset);
				                                  subsequance = Lists.newArrayList(); 
							                    }							                										
					                     }                                       				                          
								     }										
				}	
		    }
		      // If no Named Entity is recognized in the question
		    else if(stringSubsequance.length() > 1 && ( NE.isEmpty() || NE.matches("null")) ) 
                   {
				      if (checkOf(stringSubsequance))
						  {	
					         subsequance.add(stringSubsequance); 
 							 transformTree(subsequance, q, tokenOffset);
				             subsequance = Lists.newArrayList(); 
						  }	
			        }			     
        }
		
		 public static boolean checkOf(String stringSubsequance)
		 { //Ex: QALD7, ID 0 => adaptation of
		   //Temp methode
		   //TODO by Rawan: avoid combining such tokens in runPhraseCombination( , , ) algorithm
			 String temp[]= stringSubsequance.split(" ");
			 if (temp.length == 2 && temp[1].matches("of")) 
		         { 		
					 return false;
				 }
			 else return true;  	 
		 }
		   
		public static void transformTree(final List<String> subsequance, final HAWKQuestion q, final int subsequenceStartOffset) 
	{
				// This methode is called in runPhraseCombination() and receives NE or CNN 
				String isNamedEntity = ""+subsequance;
				String combinedNN = null;
				String combinedURI = null;
				// Check if subsequance(isNamedEntity) is a NE or CNN 
			    if (isNamedEntity.contains("resource")) 
			       {
				       String namedEntity = ""+subsequance;
                       // NE					   
				       namedEntity = namedEntity.replace("[","");
				       namedEntity = namedEntity.replace("]","");
					   // NE URI
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
		        if (null == nounphrases) 
				   {
			             nounphrases = Lists.newArrayList();
		           }
		        nounphrases.add(tmpEntity);
		        q.getLanguageToNounPhrases().put("en", nounphrases);				   
	}
		
	public static void resolveCompoundNouns(final MutableTree tree, final List<Entity> list) 
	{

		Stack<MutableTreeNode> stack = new Stack<>();
		stack.push(tree.getRoot());
		while (!stack.isEmpty()) {

			MutableTreeNode thisNode = stack.pop();
			String label = thisNode.label;
			if (label.contains("resource"))
			{
				thisNode.posTag = "ADD";
			}
             
			else if (label.contains("aksw.org")) 
			{
				thisNode.label = Joiner.on(" ").join(label.replace("http://aksw.org/combinedNN/", "").split("_"));
				thisNode.posTag = "CombinedNN";
			}
		
			for (MutableTreeNode child : thisNode.getChildren()) {
				stack.push(child);

			}
		}

	}

}
