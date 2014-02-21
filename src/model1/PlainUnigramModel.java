package model1;

import java.util.HashMap;


//Plain Unigram Model without padding, inherits from BaseWordCounter
public class PlainUnigramModel extends BaseWordCounter{

	//override function, split words in a line and save to given HashMap
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void splitLineToMap(HashMap hashMap, String line) {
		//cast to Unigram HashMap type
		HashMap<String,Integer> unigramMap=(HashMap<String,Integer>)hashMap;
		//split with whitespace
		String[] words=line.split(" ");

		//count the number of each word
		for(String word:words){
			//if the word is "",ignore it
			if(word.isEmpty())
				continue;
			//if the word hasn't been counted, add it to the map
			if(!hashMap.containsKey(word)){
				unigramMap.put(word,0);
			}
			//add the count of this word by one
			unigramMap.put(word,unigramMap.get(word)+1);
		}
	}
	
	//override function
	@SuppressWarnings({ "rawtypes", "unchecked" })
	int getTypeCount(HashMap hashMap) {
		int typeCount=map.size();
		HashMap<String,Integer> unigramMap=(HashMap<String,Integer>)hashMap;
		for(String word:unigramMap.keySet()){
			//if there are unknown words, add the typeCount by one
			if(!map.containsKey(word)){
				typeCount++;
				break;
			}
		}
		return typeCount;
	}

	//override function, para is the value of alpha
	@SuppressWarnings({ "rawtypes", "unchecked" })
	double getLogModelProbFromMap(HashMap hashMap, double para, int typeCount) {
		double prob=0.0;
		HashMap<String,Integer> unigramMap=(HashMap<String,Integer>)hashMap;
		for(String word:unigramMap.keySet()){
			double theta=getUnigramWordSmoothedProb(word,para,typeCount);
			prob+=unigramMap.get(word)*Math.log(theta);
		}
		return prob;
	}
	
	//get smoothed probability of given word
	@SuppressWarnings({ "unchecked" })
	public double getUnigramWordSmoothedProb(String word,double alpha,int typeCount){
		HashMap<String,Integer> hashMap=(HashMap<String,Integer>)map;
		int wCount=0;
		if(hashMap.containsKey(word)){
			wCount=hashMap.get(word);
		}
		double theta=(wCount+alpha)/(totalWordCount+alpha*typeCount);
		return theta;
	}

	//override function
	HashMap<String,Integer> generateHashMap() {
		return new HashMap<String,Integer>();
	}

	//set the totalWordCount variable
	@SuppressWarnings({ "unchecked" })
	void setTotalCount() {
		HashMap<String,Integer> hashMap=(HashMap<String,Integer>)map;
		totalWordCount=0;
		for(String word:hashMap.keySet()){
			totalWordCount+=hashMap.get(word);
		}
		
	}
	
	
}
