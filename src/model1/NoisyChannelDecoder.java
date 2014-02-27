package model1;

public class NoisyChannelDecoder {
	
	public static void main(String[] args){
		MTModel mtModel=new MTModel();
		mtModel.trainParameter(args[1], args[0]);
		PaddedUnigramModel uniModel=new PaddedUnigramModel();
		uniModel.trainModel(args[0]);
		//optimize alpha
		double alpha=1.7;
		//initialize bigram model and save training data to its map
		BigramModel biModel=new BigramModel(uniModel,alpha);
		biModel.trainModel(args[0]);
		double beta=120;
		mtModel.translateTestFileWithNoisyChannelDecoding(args[2], biModel, beta);
	}
	
}
