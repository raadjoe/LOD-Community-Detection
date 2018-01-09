package Client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import Communities.CommunityDetection;

import com.mashape.unirest.http.exceptions.UnirestException;

public class MainFunction {

	public static void main(String[] args) throws IOException, UnirestException, URISyntaxException{
		System.out.println("-- Program Start --" );
		System.out.println("" );
		long startTime = System.currentTimeMillis();


		String baseURL = "sameas.lod.labs.vu.nl/";
		String identityClosureID = "5723"; // Barack Obama identity closure
		//String identityClosureID = "41204263";
		//String identityClosureID = "4073"; // biggest identity closure
		//String identityClosureID = "41182339"; // a small identity closure
		String fileName = "explicit";

		//new SampleApplication("Datasets_Graph");

		//new DatasetsRelations(baseURL, identityClosureID, "data/Datasets-Graph.dot");


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

		

		System.out.println("-- Program End --" );
		Long endTime = System.currentTimeMillis();
		Long totalTimeMs = (endTime - startTime) ;
		System.out.println(String.format("%d min, %d sec", 
				TimeUnit.MILLISECONDS.toMinutes(totalTimeMs),
				TimeUnit.MILLISECONDS.toSeconds(totalTimeMs) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTimeMs))));
	}

}
