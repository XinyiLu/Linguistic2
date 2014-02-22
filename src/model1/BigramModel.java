package model1;

import java.util.ArrayList;
import java.util.HashMap;

//Bigram Model inherits from BaseWordCounter
public class BigramModel extends BaseWordCounter {
	//the unigram model to calculate smoothed unigram probability
	private PlainUnigramModel unigram;
	private double alpha;
	
	public BigramModel(){
		super();
	}
	
	public BigramModel(PlainUnigramModel uni,double al){
		super();
		unigram=uni;
		alpha=al;
	}
	
	//override function
	void setTotalCount() {
		totalWordCount=unigram.getTotalCount();
	}

	//override function
	HashMap<String,HashMap<String,Integer>> generateHashMap() {
		return new HashMap<String,HashMap<String,Integer>>();
	}
	
	//override function
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void splitLineToMap(HashMap hashMap, String line) {
		String[] words=line.split(" ");
		HashMap<String,HashMap<String,Integer>> bigramMap=(HashMap<String,HashMap<String,Integer>>)hashMap;
		ArrayList<String> wordList=new ArrayList<String>(words.length+1);
		for(String word:words){
			if(word.isEmpty())
				continue;
			wordList.add(word);
		}
		wordList.add(0,"");
		wordList.add("");
		//count the number of each word
		for(int i=0;i<wordList.size();i++){
			//if the word is "",ignore it
			String word=wordList.get(i);
			//totalCount++;
			//if the word hasn't been counted, add it to the map
			if(!bigramMap.containsKey(word)){
				bigramMap.put(word,new HashMap<String,Integer>());
			}
			//if it's the last word, then follow word should be padding symbol
			String followWord=((i==wordList.size()-1)?"":wordList.get(i+1));
			HashMap<String,Integer> subMap=bigramMap.get(word);
			if(!subMap.containsKey(followWord)){
				subMap.put(followWord,0);
			}
			//add the count of this word by one
			subMap.put(followWord,subMap.get(followWord)+1);
		}	
		
	}
	
	//override function
	@SuppressWarnings({ "rawtypes", "unchecked" })
	int getTypeCount(HashMap hashMap) {
		HashMap<String,HashMap<String,Integer>> bigramMap=(HashMap<String,HashMap<String,Integer>>)hashMap;
		HashMap<String,Integer> unigramMap=(HashMap<String,Integer>)unigram.getHashMap();
		for(String word1:bigramMap.keySet()){
			//if the bigramMap has unknown words, add the type count by one 
			if(!unigramMap.containsKey(word1)){
				return unigramMap.size()+1;
			}
		}
		return unigramMap.size();
	}

	//override function
	@SuppressWarnings({ "rawtypes", "unchecked" })
	double getLogModelProbFromMap(HashMap hashMap, double beta, int typeCount) {
		HashMap<String,HashMap<String,Integer>> testMap=(HashMap<String,HashMap<String,Integer>>)hashMap;
		double prob=0.0;
		for(String word1:testMap.keySet()){
			//get all the words following word1
			HashMap<String,Integer> subMap=testMap.get(word1);
			for(String word2:subMap.keySet()){
				//get the probability for each pair (w,w')
				double bigTheta=getBigramWordSmoothedProb(word1,word2,beta,typeCount);
				prob+=subMap.get(word2)*Math.log(bigTheta);
			}
		}
		return prob;
	}
	
	//get the bigram smoothed probability for each word pair
	@SuppressWarnings({ "unchecked" })
	public double getBigramWordSmoothedProb(String word1,String word2,double beta,int typeCount){
		double theta=unigram.getUnigramWordSmoothedProb(word2, alpha,typeCount);
		HashMap<String,HashMap<String,Integer>> bigramMap=(HashMap<String,HashMap<String,Integer>>)map;
		HashMap<String,Integer> unigramMap=(HashMap<String,Integer>)unigram.getHashMap();
		//if the word is unknown, according to equation 1.11, we just need to return the unigram smoothed probability of w'
		if(!unigramMap.containsKey(word1)){
			return theta;
		}
		assert(bigramMap.containsKey(word1));
		HashMap<String,Integer> subMap=bigramMap.get(word1);
		int count=0;
		if(subMap.containsKey(word2)){
			count=subMap.get(word2);
		}
		
		return (count+beta*theta)*1.0/(unigramMap.get(word1)+beta);
	}

}
