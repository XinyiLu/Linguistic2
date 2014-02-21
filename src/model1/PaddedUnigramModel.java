package model1;

import java.util.ArrayList;
import java.util.HashMap;

// Padded Unigram Model inherits from PlainUnigramModel
public class PaddedUnigramModel extends PlainUnigramModel {
	
	//the only difference from plain model is adding a padding symbol to each line
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void splitLineToMap(HashMap hashMap, String line) {
		HashMap<String,Integer> unigramMap=(HashMap<String,Integer>)hashMap;
		String[] words=line.split(" ");
		ArrayList<String> list=new ArrayList<String> (words.length+1);
		for(String word:words){
			if(!word.isEmpty()){
				list.add(word);
			}
		}
		//use empty string to represent the padding symbol
		list.add("");
		//count the number of each word
		for(String word:list){
			//if the word hasn't been counted, add it to the map
			if(!hashMap.containsKey(word)){
				unigramMap.put(word,0);
			}
			//add the count of this word by one
			unigramMap.put(word,unigramMap.get(word)+1);
		}
	}
	
}
