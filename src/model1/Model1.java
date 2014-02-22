package model1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
		public TranslationUnit(){
			bestWord=new String();
			transProb=0;
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
		
		while(Math.abs(curProb-prevProb)>1){
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
	
	public void translateTestFileWithDumbDecoding(String testFile,String resultFile){
		try {
			BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(testFile),"ISO-8859-1"));
			BufferedWriter writer=new BufferedWriter(new FileWriter(resultFile));
			String line=null;
			//each time we read a line, count its words
			while((line=reader.readLine())!=null){
				writer.write(translateSentenceWithDumbDecoding(line));
				writer.newLine();
			}
			//close the buffered reader
			reader.close();
			writer.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	public void translateTestFileWithNoisyChannelDecoding(String testFile,String resultFile,BigramModel biModel,double beta){
		try {
			//first, we need to get the typeCount of the bigram model
			HashMap tempMap=biModel.generateHashMap();
			biModel.parseFileToMap(testFile,tempMap);
			int typeCount=biModel.getTypeCount(tempMap);
			tempMap.clear();
			
			BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(testFile),"ISO-8859-1"));
			BufferedWriter writer=new BufferedWriter(new FileWriter(resultFile));
			String line=null;
			//each time we read a line, count its words
			while((line=reader.readLine())!=null){
				writer.write(translateSentenceWithNoisyChannelDecoding(line,biModel,beta,typeCount));
				writer.newLine();
			}
			//close the buffered reader
			reader.close();
			writer.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	public String translateSentenceWithNoisyChannelDecoding(String line,BigramModel biModel,double beta,int typeCount){
		String[] words=line.split(" ");
		String resultStr="";
		ArrayList<String> list=new ArrayList<String> (words.length+2);
		for(String word:words){
			if(word.isEmpty())
				continue;
			list.add(word);
		}
		list.add("");
		
		//get each word's translation,the first word is padding symbol
		String prevWord="";
		for(String word:list){
			String curWord=getBestWordWithNoisyChannelDecoding(prevWord,word,biModel,beta,typeCount);
			prevWord=curWord;
			resultStr+=curWord+" ";
		}
	
		return resultStr.substring(0, resultStr.length()-1);
	}
	
	public String getBestWordWithNoisyChannelDecoding(String prevWord,String word,BigramModel biModel,double beta,int typeCount){
		HashMap<String,ProbUnit> subMap=translationProbMap.get(word);
		String bestWord="";
		double prob=Double.MIN_VALUE;
		for(String testWord:subMap.keySet()){
			double testProb=Math.log(biModel.getBigramWordSmoothedProb(prevWord, testWord, beta, typeCount))+Math.log(subMap.get(testWord).condProb);
			if(testProb>prob){
				bestWord=testWord;
				prob=testProb;
			}
		}
		
		return bestWord;
	}
	
	public String translateSentenceWithDumbDecoding(String line){
		String[] words=line.split(" ");
		String resultStr="";
		
		for(String word:words){
			if(word.isEmpty())
				continue;
			if(!translationUnitMap.containsKey(word)){
				resultStr+=word+" ";
			}else{
				TranslationUnit unit=translationUnitMap.get(word);
				resultStr+=unit.bestWord+" ";
			}
		}
		
		return resultStr.substring(0,resultStr.length()-1);
	}
	
	public static void main(String[] args){
		assert(args.length==3);
		Model1 model=new Model1();
		model.trainParameter(args[0], args[1]);
		//model.saveTranslationMapToFile(args[2]);
		
		
		model.translateTestFileWithDumbDecoding(args[3], args[4]);
		PaddedUnigramModel uniModel=new PaddedUnigramModel();
		uniModel.trainModel(args[0]);
		//optimize alpha
		double alpha=1.7;
		//initialize bigram model and save training data to its map
		BigramModel biModel=new BigramModel(uniModel,alpha);
		biModel.trainModel(args[0]);
		double beta=120;
		model.translateTestFileWithNoisyChannelDecoding(args[3], args[5], biModel, beta);
		System.out.println("Finished");
	}
	
	
	
	
}
