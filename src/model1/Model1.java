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
		linePairMap.put(index,(ArrayList<String>[])(new ArrayList[]{klist,vlist}));
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
	
	public double updatePartialCountAndProb(){
		
		double resultProb=0.0;
		for(int lineCount=1;lineCount<=totalLineCount;lineCount++){
			//for this line update the partial count
			ArrayList<String>[] linePair=linePairMap.get(lineCount);
			assert(linePair!=null&&linePair.length==2);
			//for each pair of words, update the count
			//first need to get P(fk) for each vword, keep a hashmap for this
			HashMap<String,Double> totalProbMap=new HashMap<String,Double>();	
			for(String kword:linePair[0]){
				HashMap<String,ProbUnit> subMap=translationProbMap.get(kword);
				for(String vword:linePair[1]){
					assert(subMap.containsKey(vword));
					totalProbMap.put(vword,(totalProbMap.containsKey(vword)?totalProbMap.get(vword):0)+subMap.get(vword).condProb);
				}
			}
			
			//update the partial count for each pair
			for(String kword:linePair[0]){
				HashMap<String,ProbUnit> subMap=translationProbMap.get(kword);
				for(String vword:linePair[1]){
					ProbUnit unit=subMap.get(vword);
					assert(unit.sentenceMap.containsKey(lineCount));
					unit.sentenceMap.put(lineCount,unit.condProb/totalProbMap.get(vword));
				}
			}
			
			for(double prob:totalProbMap.values()){
				resultProb+=Math.log(prob);
			}
		}
		
		//update the conditional probability like p(f|e)
		for(String kword:translationProbMap.keySet()){
			double count=0;
			HashMap<String,ProbUnit> subMap=translationProbMap.get(kword);
			for(String vword:subMap.keySet()){
				HashMap<Integer,Double> sentenceMap=subMap.get(vword).sentenceMap;
				for(Double subCount:sentenceMap.values()){
					count+=subCount;
				}
			}	
			
			for(String vword:subMap.keySet()){
				int unitCount=0;
				for(Double subCount:subMap.get(vword).sentenceMap.values()){
					unitCount+=subCount;
				}
				subMap.get(vword).condProb=unitCount/count;
			}
		}
		
		return resultProb;
	}

	public void trainTranslationProb(){
		double prevProb=0;
		double curProb=updatePartialCountAndProb();
		
		while(Math.abs(prevProb-curProb)>0.000001){
			prevProb=curProb;
			curProb=updatePartialCountAndProb();
		}
	}
	
	
	
	
	
	
}
