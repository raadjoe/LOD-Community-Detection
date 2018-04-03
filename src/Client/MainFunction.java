package Client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import Communities.CommunityDetection;
import RocksDB.MyRocksDB;

import com.mashape.unirest.http.exceptions.UnirestException;

public class MainFunction {

	public static void main(String[] args) throws IOException, UnirestException, URISyntaxException{
		System.out.println("-- Program Start --" );
		System.out.println("" );
		long startTime = System.currentTimeMillis();


		//String baseURL = "sameas.cc/";
		//String identityClosureID = "5723"; // Barack Obama identity closure
		//String identityClosureID = "41204263";
		//String identityClosureID = "4073"; // biggest identity closure
		//String identityClosureID = "41182339"; // a small identity closure
		
		//Local Paths:
		/*String resultsPath = "data/links-score.tsv";
		String identitySetsPath = "../Identity-Sets/sameAs-implicit/id_terms.dat/id_terms.dat";
		String explicitStatementsPath = "../Explicit-Identity-Graph/id-ext.hdt";*/
		
		//Server Paths:
		String resultsPath = "/opt/joe-2/links-score.tsv";
		String identitySetsPath = "/opt/joe-2/id_terms.dat";
		String explicitStatementsPath = "/opt/joe-2/id-ext.hdt";
		String[] inputLouvain = new String[8];
		File f;
		inputLouvain[0] = "data/Communities.txt"; // Output File
		inputLouvain[1] = "1"; // Modularity function (1 = standard; 2 = alternative)
		inputLouvain[2] = "1.0"; // Value of the resolution parameter. Use a value of 1.0 for standard modularity-based community detection. Use a value above (below) 1.0 if you want to obtain a larger (smaller) number of communities.
		inputLouvain[3] = "1"; // Algorithm for modularity optimization (1 = original Louvain algorithm; 2 = Louvain algorithm with multilevel refinement; 3 = SLM algorithm)
		inputLouvain[4] = Integer.toString(10);  // Number of random starts
		inputLouvain[5] = "10"; // Number of iterations per random start
		inputLouvain[6] = "0"; // Seed of the random number generator
		inputLouvain[7] = "0"; // Whether or not to print output to the console (0 = no; 1 = yes)

		//new SampleApplication("Datasets_Graph");

		TreeMap<Float, Integer> allErrValues = new TreeMap<>();
		int totalStatements = 0;
		int totalDistinctStatements = 0;
		int totalEqualitySets = 0;
		f = new File(resultsPath);
		File equalitySetFile = null;
		HDT hdt = HDTManager.mapHDT(explicitStatementsPath, null);
		//HDT hdt = HDTManager.loadIndexedHDT("../Explicit-Identity-Graph/sameAs_lod", null);
		FileInputStream inputStream = null;
		//BOMInputStream ubis = null;
		try {
			try {
				inputStream = new FileInputStream(identitySetsPath);
				/*ubis = new BOMInputStream(inputStream, ByteOrderMark.UTF_8, 
						ByteOrderMark.UTF_16BE, 
						ByteOrderMark.UTF_16LE, 
						ByteOrderMark.UTF_32BE, 
						ByteOrderMark.UTF_32LE);*/
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
				String[] dataArray = thisLine.split(" ");
				String equalitySetID = dataArray[0];
				EqualitySet EqSet = new EqualitySet(equalitySetID, dataArray, hdt);
				//System.out.println("Number of Edges in this Equality Set: " + EqSet.statementsCounter);
				//System.out.println("Number of Distinct Edges in this Equality Set: " + EqSet.distinctStatementsCounter);

				totalStatements = totalStatements + EqSet.statementsCounter;
				totalDistinctStatements = totalDistinctStatements + EqSet.distinctStatementsCounter;
				new CommunityDetection(inputLouvain, EqSet, equalitySetFile, f, allErrValues);
				
				
				if(totalEqualitySets%100000 == 0)
				{
					Long endTime = System.currentTimeMillis();
					Long totalTimeMs = (endTime - startTime) ;
					System.out.print("Run Time: ");
					System.out.println(String.format("%d min, %d sec", 
							TimeUnit.MILLISECONDS.toMinutes(totalTimeMs),
							TimeUnit.MILLISECONDS.toSeconds(totalTimeMs) - 
							TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTimeMs))));		
					System.out.println(totalEqualitySets + " / 48.99 M equality sets");
					System.out.println(totalStatements + " / 558.9 M statements");
					System.out.println(totalDistinctStatements + " / 330 M distinct statements");
					System.out.println("----------");
				}
				
				/*if(EqSet.statementsCounter >0)
				{
					File equalitySetFile = new File("data/equality-set-"+equalitySetID+".txt");
					EqSet.writeExplicitGraph(equalitySetFile);
					new CommunityDetection(inputLouvain, EqSet, equalitySetFile, f, allErrValues);
					equalitySetFile.delete();
				}
				else
				{
					System.out.println("Something is wrong");
				}*/


				//System.out.println("----------");
				//System.out.println(line);
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

		try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);)
		{
			for(Entry<Float, Integer> thisEntry: allErrValues.entrySet())
			{
				bw.write(thisEntry.getKey() + "\t" + thisEntry.getValue()+"\n");
			}	
		}
















		//IdentityNetwork ICG = new IdentityNetwork(baseURL, identityClosureID, fileName);
		/*
		IdentityClosureExplicitGraph ICG = new IdentityClosureExplicitGraph(baseURL, identityClosureID, fileName);

		System.out.println("Number of Edges in this Identity Closure: " + ICG.statementsCounter);
		System.out.println("Number of Distinct Edges in this Identity Closure: " + ICG.distinctStatementsCounter);

		String[] inputLouvain = new String[8];
		inputLouvain[0] = "data/Communities.txt"; // Output File
		inputLouvain[1] = "1"; // Modularity function (1 = standard; 2 = alternative)
		inputLouvain[2] = "1.0"; // Value of the resolution parameter. Use a value of 1.0 for standard modularity-based community detection. Use a value above (below) 1.0 if you want to obtain a larger (smaller) number of communities.
		inputLouvain[3] = "1"; // Algorithm for modularity optimization (1 = original Louvain algorithm; 2 = Louvain algorithm with multilevel refinement; 3 = SLM algorithm)
		//inputLouvain[4] = Integer.toString(ICG.URItoID.size()/4);  // Number of random starts
		inputLouvain[4] = Integer.toString(10);  // Number of random starts
		inputLouvain[5] = "10"; // Number of iterations per random start
		inputLouvain[6] = "0"; // Seed of the random number generator
		inputLouvain[7] = "0"; // Whether or not to print output to the console (0 = no; 1 = yes)

		new CommunityDetection(inputLouvain, ICG);
		 */


		System.out.println("-- Program End --" );
		Long endTime = System.currentTimeMillis();
		Long totalTimeMs = (endTime - startTime) ;
		System.out.println(String.format("%d min, %d sec", 
				TimeUnit.MILLISECONDS.toMinutes(totalTimeMs),
				TimeUnit.MILLISECONDS.toSeconds(totalTimeMs) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTimeMs))));
	}



}
