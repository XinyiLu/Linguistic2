package model1;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public abstract class BaseParser {
	
	protected int totalLineCount;
	
	@SuppressWarnings({ "rawtypes"})
	protected void parseFileToMap(String kFile,String vFile,HashMap hashMap){
		totalLineCount=0;
		try {
			BufferedReader kReader=new BufferedReader(new InputStreamReader(new FileInputStream(kFile),"ISO-8859-1"));
			BufferedReader vReader=new BufferedReader(new InputStreamReader(new FileInputStream(vFile),"ISO-8859-1"));
			String kline=null,vline=null;
			//each time we read a line, count its words
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
	
	protected double getLanguageLogProb(String kfile,String testFile){
		PaddedUnigramModel uniModel=new PaddedUnigramModel();
		uniModel.trainModel(kfile);
		//optimize alpha
		double alpha=1.5;
		//initialize bigram model and save training data to its map
		BigramModel biModel=new BigramModel(uniModel,alpha);
		biModel.trainModel(kfile);
		double beta=120;
		return biModel.getLogModelProbFromFile(testFile,beta);
	}
	
	@SuppressWarnings({ "rawtypes"})
	abstract void saveLinePairToMap(String kline,String vline,HashMap hashMap,int index);
}
