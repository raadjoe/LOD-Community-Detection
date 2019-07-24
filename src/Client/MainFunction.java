package Client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;


import Communities.CommunityDetection;



public class MainFunction {

	public static void main(String[] args) throws IOException, URISyntaxException{
		System.out.println("-- Program Start --" );
		System.out.println("" );
		long startTime = System.currentTimeMillis();


		//String baseURL = "sameas.cc/";
		//String identityClosureID = "5723"; // Barack Obama identity closure
		//String identityClosureID = "41204263";
		//String identityClosureID = "4073"; // biggest identity closure
		//String identityClosureID = "41182339"; // a small identity closure

		// Paths to save the outputs:
		String linksVariationPath = "links-score-variation-no-weight.tsv";
		String resultsPath = "links-score.tsv";
		
		// Path for the equivalence classes (the file id2terms.csv can be downloaded from https://zenodo.org/record/3345674)
		String identitySetsPath = "../id2termstest.csv";
		
		// Path for the sameAs network (sameAs-Network.hdt can be downloaded from https://zenodo.org/record/1973099)
		String explicitStatementsPath = "../sameAs-Network.hdt";

		//Server Paths:
		/*String resultsPath = "/opt/joe-2/all-sameAs-error-degrees.tsv";
		String identitySetsPath = "/opt/joe-2/id_terms.dat";
		String explicitStatementsPath = "/opt/joe-2/id-ext.hdt";*/
		
		String[] inputLouvain = new String[8];
		File f, f1;
		inputLouvain[0] = "data/Communities.txt"; // Output File
		inputLouvain[1] = "1"; // Modularity function (1 = standard; 2 = alternative)
		inputLouvain[2] = "1.0"; // Value of the resolution parameter. Use a value of 1.0 for standard modularity-based community detection. Use a value above (below) 1.0 if you want to obtain a larger (smaller) number of communities.
		inputLouvain[3] = "1"; // Algorithm for modularity optimization (1 = original Louvain algorithm; 2 = Louvain algorithm with multilevel refinement; 3 = SLM algorithm)
		inputLouvain[4] = Integer.toString(1);  // Number of random starts
		inputLouvain[5] = "5"; // Number of iterations per random start
		inputLouvain[6] = "0"; // Seed of the random number generator
		inputLouvain[7] = "0"; // Whether or not to print output to the console (0 = no; 1 = yes)
		TreeMap<Float, Integer> allErrValues = new TreeMap<>();


		int totalNumberOfCommunities = 0;
		int totalNumberOfTerms = 0;
		int totalStatements = 0;
		int totalDistinctStatements = 0;
		int totalReflexiveStatements = 0;
		int totalEqualitySets = 0;
		int maxEqSize = 0;
		String maxEqID = "";
		int maxCommunity = 0;
		String maxCommunityEqID = "";
		f = new File(resultsPath);
		f1 = new File(linksVariationPath);
		File equalitySetFile = null;
		HDT hdt = HDTManager.mapHDT(explicitStatementsPath, null);	
		//HDT hdt = HDTManager.loadIndexedHDT("../Explicit-Identity-Graph/sameAs_lod", null);
		FileInputStream inputStream = null;

		
		int intra =0,inter =0;
		
		
		try {
			try {
				inputStream = new FileInputStream(identitySetsPath);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			InputStreamReader isr = new InputStreamReader(inputStream);
			BufferedReader br = new BufferedReader(isr);
			String thisLine = null;
			while ((thisLine = br.readLine()) != null)
			{	
				totalEqualitySets++;
				String[] dataArray = thisLine.split(" ", 2);					
				String equalitySetID = dataArray[0];
				//System.out.println(equalitySetID);
				if(equalitySetID.equals("17915084"))
				{
					inputLouvain[3] = "3";
				}
				else
				{
					inputLouvain[3] = "1";
				}
				String[] setOfTerms = dataArray[1].split("> ");				
				EqualitySet EqSet = new EqualitySet(equalitySetID, setOfTerms, hdt);
				totalStatements = totalStatements + EqSet.statementsCounter;
				totalDistinctStatements = totalDistinctStatements + EqSet.distinctStatementsCounter;
				totalReflexiveStatements = totalReflexiveStatements + EqSet.reflexiveStatementsCounter;
				int thisSize = EqSet.URItoID.size();
				totalNumberOfTerms = totalNumberOfTerms + thisSize;
				if(thisSize > maxEqSize)
				{
					maxEqSize = thisSize;
					maxEqID = equalitySetID;
				}

				/*System.out.println(equalitySetID);
				System.out.println("Number of Edges in this Equality Set: " + EqSet.statementsCounter);
				System.out.println("Number of Distinct Edges in this Equality Set: " + EqSet.distinctStatementsCounter);
				System.out.println("Number of Reflexive Statements in this Equality Set: " + EqSet.reflexiveStatementsCounter);
				System.out.println("-----------------");*/
				CommunityDetection cd = new CommunityDetection(inputLouvain, EqSet, equalitySetFile, f, allErrValues);
				totalNumberOfCommunities = totalNumberOfCommunities + cd.numberOfCommunities;
				
				intra = intra + cd.intra;
				inter = inter + cd.inter;
				if(cd.maxCommunity > maxCommunity)
				{
					maxCommunity = cd.maxCommunity;
					maxCommunityEqID = equalitySetID;
				}
				if(totalEqualitySets%100000 == 0)
				{
					Long endTime = System.currentTimeMillis();
					Long totalTimeMs = (endTime - startTime) ;

					System.out.print("Run Time: ");
					System.out.println(String.format("%d min, %d sec", 
							TimeUnit.MILLISECONDS.toMinutes(totalTimeMs),
							TimeUnit.MILLISECONDS.toSeconds(totalTimeMs) - 
							TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTimeMs))));		
					System.out.println(totalEqualitySets + " / ~48.99 M equality sets");
					System.out.println(totalStatements + " / ~558.9 M statements");
					System.out.println("	" + totalReflexiveStatements + " / ~2.8 M reflexive statements");
					System.out.println("	" + totalDistinctStatements + " / ~330 M distinct statements");
					System.out.println(totalNumberOfTerms + " terms");
					System.out.println(totalNumberOfCommunities + " communities");
					System.out.println("The largest community contains " + maxCommunity 
							+ " terms (Equality Set ID: " + maxCommunityEqID + ")");
					System.out.println("Largest Equality Set till now: " + maxEqID + " (size = " +maxEqSize +")");
					System.out.println("----------");
				}
			}
			
			try(FileWriter fw1 = new FileWriter(f1, true);
					BufferedWriter bw1 = new BufferedWriter(fw1);)
			{
				for(Entry<Float, Integer> thisEntry: allErrValues.entrySet())
				{
					bw1.write(thisEntry.getKey() + " " + thisEntry.getValue()+"\n");
				}	
			}
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("------- FINAL STATS -------");
		System.out.println("Number of Intra community links with err >0.99 is " + intra);
		System.out.println("Number of Inter community links with err >0.99 is " + inter);
		System.out.println(totalEqualitySets + " / ~48.99 M equality sets");
		System.out.println(totalStatements + " / ~558.9 M statements");
		System.out.println("	" + totalReflexiveStatements + " / ~2.8 M reflexive statements");
		System.out.println("	" + totalDistinctStatements + " / ~330 M distinct statements");
		System.out.println(totalNumberOfTerms + " terms");
		System.out.println(totalNumberOfCommunities + " communities");
		System.out.println("The largest community contains " + maxCommunity 
				+ " terms (Equality Set ID: " + maxCommunityEqID + ")");
		System.out.println("Largest Equality Set till now: " + maxEqID + " (size = " +maxEqSize +")");
		System.out.println("------- PROGRAM FINISHED -------");
		Long endTime = System.currentTimeMillis();
		Long totalTimeMs = (endTime - startTime) ;
		System.out.println(String.format("%d min, %d sec", 
				TimeUnit.MILLISECONDS.toMinutes(totalTimeMs),
				TimeUnit.MILLISECONDS.toSeconds(totalTimeMs) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTimeMs))));
	}



}
