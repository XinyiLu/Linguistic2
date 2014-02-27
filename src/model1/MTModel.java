package model1;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class MTModel{
	//structure that contains conditional probability p(f|e) and partial count of n(e,f)
	class ProbUnit{
		private double condProb;
		private double partialCount;
		
		public ProbUnit(){
			condProb=1;
			partialCount=0;
		}
		
		public ProbUnit(double prob,double count){
			condProb=prob;
			partialCount=count;
		}
	}
	
	//structure to save the best translation for each word,transProb is the conditional probability of p(f|e)
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
	
	//member variables
	//HashMap to save the iteration table,if we translate French to English, then English word is the key
	//all the possible French translations and its corresponding ProbUnit forms a submap as the value
	private HashMap<String,HashMap<String,ProbUnit>> translationProbMap;
	//the HashMap to save all the words for each English-French line pair
	private HashMap<Integer,ArrayList<String>[]> linePairMap;
	//the HashMap to map each French word to its best English translation with the largest conditional probability
	private HashMap<String,TranslationUnit> translationUnitMap;
	//count the number of lines in training file
	private int totalLineCount;
		
	public MTModel(){
		translationProbMap=new HashMap<String,HashMap<String,ProbUnit>>();
		linePairMap=new HashMap<Integer,ArrayList<String>[]>();
		translationUnitMap=new HashMap<String,TranslationUnit>();
	}
	
	//parse the training file to iteration table: translationProbMap
	@SuppressWarnings({ "rawtypes"})
	public void parseFileToMap(String kFile,String vFile,HashMap hashMap){
		totalLineCount=0;
		try {
			BufferedReader kReader=new BufferedReader(new InputStreamReader(new FileInputStream(kFile),"ISO-8859-1"));
			BufferedReader vReader=new BufferedReader(new InputStreamReader(new FileInputStream(vFile),"ISO-8859-1"));
			String kline=null,vline=null;
			//each time we read a line, increment the line count
			int count=0;
			while((kline=kReader.readLine())!=null&&(vline=vReader.readLine())!=null){
				count++;
				saveLinePairToMap(kline,vline,hashMap,count);
			}
			totalLineCount=count;
			//close the buffered reader
			kReader.close();
			vReader.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	//sub-function to save a pair of lines to iteration table
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void saveLinePairToMap(String kline, String vline, HashMap hashMap,int index) {
		
		HashMap<String,HashMap<String,ProbUnit>> map=(HashMap<String,HashMap<String,ProbUnit>>)hashMap;
		String[] kwords=kline.split(" ");
		String[] vwords=vline.split(" ");
		
		ArrayList<String> klist=new ArrayList<String>(Arrays.asList(kwords));
		ArrayList<String> vlist=new ArrayList<String>(Arrays.asList(vwords));
		assert(kwords.length>0&&vwords.length>0);
		//save the list pair to linePairMap given the index
		linePairMap.put(index,new ArrayList[]{klist,vlist});
		//save each pair of words to the map with initial probability 1.0
		for(String kword:klist){
			if(!map.containsKey(kword)){
				map.put(kword,new HashMap<String,ProbUnit>());
			}
			HashMap<String,ProbUnit> subMap=map.get(kword);
			for(String vword:vlist){
				if(!subMap.containsKey(vword)){
					subMap.put(vword,new ProbUnit());
				}
			}
		}
		
	}

	//update partial count and conditional probability in ProbUnit during one EM iteration
	//the return value is the log value of the product of P(f) (likelihood function)
	public double updatePartialCountAndProb(){
		
		double resultProb=0.0;
		//clear the number of each pair
		for(String kword:translationProbMap.keySet()){
			HashMap<String,ProbUnit> subMap=translationProbMap.get(kword);
			for(String vword:subMap.keySet()){
				subMap.get(vword).partialCount=0;
			}
		}
		//update the partial count for each line
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
			//calculate the likelihood function
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
	
	//function to iterate through EM steps until converge
	public void trainTranslationProb(){
		double prevProb=0;
		double curProb=updatePartialCountAndProb();
		double tau=0.5;
		//while the difference of two iterations are larger than tau
		while(Math.abs(Math.abs(curProb)-Math.abs(prevProb))>tau){
			prevProb=curProb;
			curProb=updatePartialCountAndProb();
		}
		
		//update translation unit map
		for(String kword:translationProbMap.keySet()){
			HashMap<String,ProbUnit> subMap=translationProbMap.get(kword);
			for(String vword:subMap.keySet()){
				if(!translationUnitMap.containsKey(kword)){
					translationUnitMap.put(kword,new TranslationUnit(vword,subMap.get(vword).condProb));
				}else if(translationUnitMap.get(kword).transProb<subMap.get(vword).condProb){
					//save the translation word with larger conditional probability
					translationUnitMap.put(kword,new TranslationUnit(vword,subMap.get(vword).condProb));
				}
			}
		}
	}
	
	//train the conditional probability p(f|e) given two taining files
	public void trainParameter(String kfile,String vfile){
		parseFileToMap(kfile,vfile,translationProbMap);
		trainTranslationProb();
	}
	
	
	public void translateTestFileWithDumbDecoding(String testFile){
		try {
			BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(testFile),"ISO-8859-1"));
			
			String line=null;
			//each time we read a line, count its words
			while((line=reader.readLine())!=null){
				System.out.println(translateSentenceWithDumbDecoding(line));
			}
			//close the buffered reader
			reader.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}
		
	public void translateTestFileWithNoisyChannelDecoding(String testFile,BigramModel biModel,double beta){
		try {
		//first, we need to get the typeCount of the bigram model
			HashMap tempMap=biModel.generateHashMap();
			biModel.parseFileToMap(testFile,tempMap);
			int typeCount=biModel.getTypeCount(tempMap);
			tempMap.clear();
			
			BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(testFile),"ISO-8859-1"));
			String line=null;
			//each time we read a line, count its words
			while((line=reader.readLine())!=null){
				System.out.println(translateSentenceWithNoisyChannelDecoding(line,biModel,beta,typeCount));
			}
			//close the buffered reader
			reader.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}
		
	public String translateSentenceWithNoisyChannelDecoding(String line,BigramModel biModel,double beta,int typeCount){
		String[] words=line.split(" ");
		String resultStr="";
		ArrayList<String> list=new ArrayList<String> (Arrays.asList(words));
		if(list.size()>10){
			return line;
		}
		
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
		if(subMap==null)
			return word;
		String bestWord="";
		double prob=Integer.MIN_VALUE;
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
		if(words.length>10){
			return line;
		}
		for(String word:words){
			if(!translationUnitMap.containsKey(word)){
				resultStr+=word+" ";
			}else{
				TranslationUnit unit=translationUnitMap.get(word);
				resultStr+=unit.bestWord+" ";
			}
		}
		
		return resultStr.substring(0,resultStr.length()-1);
	}
		
	public double getFScore(String resultFile,String correctFile){
		//get the word count of resultFile and correctFile
		int tCount=0,rCount=0,sCount=0;
		try {
			BufferedReader tReader = new BufferedReader(new InputStreamReader(new FileInputStream(resultFile),"ISO-8859-1"));
			BufferedReader cReader=new BufferedReader(new InputStreamReader(new FileInputStream(correctFile),"ISO-8859-1"));
			String tline=null,cline=null;
			//each time we read a line, count its words
			while((tline=tReader.readLine())!=null&&(cline=cReader.readLine())!=null){
				String[] twords=tline.split(" ");
				if(twords.length>10)
					continue;
				String[] cwords=cline.split(" ");
				tCount+=twords.length;
				sCount+=cwords.length;
				HashSet<String> hashSet=new HashSet<String>(Arrays.asList(twords));
				for(String word:cwords){
					if(hashSet.contains(word)){
						rCount++;
					}
				}
			}
			//close the buffered reader
			tReader.close();
			cReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double precision=rCount*1.0/tCount;
		double recall=rCount*1.0/sCount;
		return 2*precision*recall/(precision+recall);		
	}

}
