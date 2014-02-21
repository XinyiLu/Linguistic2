package model1;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Model1 extends BaseParser{
	
	class ProbUnit{
		double condProb;
		double partialCount;
		//index is key, while double is value(count)
		
		public ProbUnit(){
			condProb=1;
			partialCount=0;
		}
		
		public ProbUnit(int prob,int count){
			condProb=prob;
			partialCount=count;
		}
	}
	
	class TranslationUnit{
		double transProb;
		String bestWord;
		public TranslationUnit(String word,double prob){
			bestWord=word;
			transProb=prob;
		}
	}
	
	HashMap<String,HashMap<String,ProbUnit>> translationProbMap;
	HashMap<Integer,ArrayList<String>[]> linePairMap;
	HashMap<String,TranslationUnit> translationUnitMap;
	
	
	public Model1(){
		translationProbMap=new HashMap<String,HashMap<String,ProbUnit>>();
		linePairMap=new HashMap<Integer,ArrayList<String>[]>();
		translationUnitMap=new HashMap<String,TranslationUnit>();
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
				subMap.get(vword).partialCount+=partialCount;
			}
		}
		
	}
	
	public double updatePartialCountAndProb(){
		
		double resultProb=0.0;
		//clear the number of each pair
		for(String kword:translationProbMap.keySet()){
			HashMap<String,ProbUnit> subMap=translationProbMap.get(kword);
			for(String vword:subMap.keySet()){
				subMap.get(vword).partialCount=0;
			}
		}
		
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
					unit.partialCount+=unit.condProb/totalProbMap.get(vword);
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
				count+=subMap.get(vword).partialCount;
			}	
			for(String vword:subMap.keySet()){
				double unitCount=subMap.get(vword).partialCount;
				subMap.get(vword).condProb=unitCount/count;
			}
		}
		
		return resultProb;
	}

	public void trainTranslationProb(){
		double prevProb=0;
		double curProb=updatePartialCountAndProb();
		
		while(Math.abs(curProb-prevProb)>10){
			System.out.println(curProb-prevProb);
			prevProb=curProb;
			curProb=updatePartialCountAndProb();
		}
		
		//update translation unit map
		for(String kword:translationProbMap.keySet()){
			HashMap<String,ProbUnit> subMap=translationProbMap.get(kword);
			for(String vword:subMap.keySet()){
				if(!translationUnitMap.containsKey(vword)){
					translationUnitMap.put(vword,new TranslationUnit(kword,subMap.get(vword).condProb));
				}else if(translationUnitMap.get(vword).transProb<subMap.get(vword).condProb){
					translationUnitMap.put(vword,new TranslationUnit(kword,subMap.get(vword).condProb));
				}
			}
		}
	}
	
	public void trainParameter(String kfile,String vfile){
		parseFileToMap(kfile,vfile,translationProbMap);
		trainTranslationProb();
	}
	
	public void saveTranslationMapToFile(String fileName){
		try {
			BufferedWriter writer=new BufferedWriter(new FileWriter(fileName));
			//for each word in keySet of the map, save the key and value and write a new line
			for(String word:translationUnitMap.keySet()){
				writer.write(word+" "+translationUnitMap.get(word).bestWord+" "+translationUnitMap.get(word).transProb);
				writer.newLine();
			}
			//close the buffered writer
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		assert(args.length==3);
		Model1 model=new Model1();
		model.trainParameter(args[0], args[1]);
		model.saveTranslationMapToFile(args[2]);
		System.out.println("Finished");
	}
	
	
	
	
}
