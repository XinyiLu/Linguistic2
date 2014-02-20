package model1;

import java.util.*;

public class Model1 extends BaseParser{
	
	class ProbUnit{
		double condProb;
		HashMap<Integer,Double> sentenceMap;
		//index is key, while double is value(count)
		
		public ProbUnit(){
			condProb=1;
			sentenceMap=new HashMap<Integer,Double>();
		}
	}
	
	HashMap<String,HashMap<String,ProbUnit>> translationProbMap;
	HashMap<Integer,ArrayList<String>[]> linePairMap;
	
	
	public Model1(){
		translationProbMap=new HashMap<String,HashMap<String,ProbUnit>>();
		linePairMap=new HashMap<Integer,ArrayList<String>[]>();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void saveLinePairToMap(String kline, String vline, HashMap hashMap,int index) {
		
		HashMap<String,HashMap<String,ProbUnit>> map=(HashMap<String,HashMap<String,ProbUnit>>)hashMap;
		String[] kwords=kline.split(" ");
		String[] vwords=vline.split(" ");
		
		ArrayList<String> klist=new ArrayList<String> (kwords.length+1);
		for(String word:kwords){
			if(!word.isEmpty()){
				klist.add(word);
			}
		}
		
		ArrayList<String> vlist=new ArrayList<String> (vwords.length+1);;
		for(String word:vwords){
			if(!word.isEmpty()){
				vlist.add(word);
			}
		}
		
		assert(klist.size()>0&&vlist.size()>0);
		double partialCount=1.0/klist.size();
		for(String kword:kwords){
			if(!map.containsKey(kword)){
				map.put(kword,new HashMap<String,ProbUnit>());
			}
			HashMap<String,ProbUnit> subMap=map.get(kword);
			for(String vword:vwords){
				if(!subMap.containsKey(vword)){
					subMap.put(vword,new ProbUnit());
				}
				HashMap<Integer,Double> lineMap=subMap.get(vword).sentenceMap;
				lineMap.put(index,partialCount);
			}
		}
		
	}
	
	public void updatePartialCount(HashMap<String,HashMap<String,ProbUnit>> hashMap){
		for(int lineCount=1;lineCount<=totalLineCount;lineCount++){
			//for this line update the partial count
			
		}
		
	}
	
}
