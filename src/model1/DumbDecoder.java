package model1;

public class DumbDecoder {
	public static void main(String[] args){
		assert(args.length==3);
		MTModel mtModel=new MTModel();
		//args[0] is English file, args[1] is French file, args[2] is test file in French
		mtModel.trainParameter(args[1], args[0]);	
		mtModel.translateTestFileWithDumbDecoding(args[2]);
	}
}
