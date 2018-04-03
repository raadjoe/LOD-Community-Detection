package Communities;

import RocksDB.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.text.html.parser.Entity;

import org.rocksdb.RocksDB;

import Client.EqualitySet;

public class ModularityOptimizer {
	boolean printOutput, update;
	Clustering clustering;
	Console console;
	double modularity, maxModularity, resolution, resolution2;
	int algorithm, i, j, modularityFunction, nIterations, nRandomStarts;
	long beginTime, endTime, randomSeed;
	Network network;
	Random random;
	String inputFileName, outputFileName;
	VOSClusteringTechnique VOSClusteringTechnique;
	public String identityClosureID;
	private static RocksDB ROCKS_DB_ID2Communities = null;
	private static RocksDB ROCKS_DB_Community2Terms = null;
	private static RocksDB ROCKS_DB_Term2Community = null;
	public static MyRocksDB MyDB;

	public ModularityOptimizer(String[] args)
	{      
		outputFileName = args[0];
		modularityFunction = Integer.parseInt(args[1]);
		resolution = Double.parseDouble(args[2]);
		algorithm = Integer.parseInt(args[3]);
		nRandomStarts = Integer.parseInt(args[4]);
		nIterations = Integer.parseInt(args[5]);
		randomSeed = Long.parseLong(args[6]);
		printOutput = (Integer.parseInt(args[7]) > 0);
		/*MyDB = new MyRocksDB("data/Communities/");
		MyDB.deleteRocksDB();
		MyDB.openRocksDB();
		ROCKS_DB_ID2Communities = MyDB.opendb("ROCKS_DB_ID2Communities");
		ROCKS_DB_Community2Terms = MyDB.opendb("ROCKS_DB_Community2Terms");
		ROCKS_DB_Term2Community = MyDB.opendb("ROCKS_DB_Term2Community");*/
	}


	public Clustering detectCommunities(EqualitySet EqSet, File inputFile) throws IOException
	{

		//System.out.println("Reading input file...");
		//System.out.println();

		//sortFile(inputFile);
		
		
		network = readInputFile(inputFile, EqSet, modularityFunction);

		//System.out.format("Number of nodes: %d%n", network.getNNodes());
		//System.out.format("Number of edges: %d%n", network.getNEdges());
		//System.out.println();
		//System.out.println("Running " + ((algorithm == 1) ? "Louvain algorithm" : ((algorithm == 2) ? "Louvain algorithm with multilevel refinement" : "smart local moving algorithm")) + "...");
		//System.out.println();

		resolution2 = ((modularityFunction == 1) ? (resolution / (2 * network.getTotalEdgeWeight() + network.totalEdgeWeightSelfLinks)) : resolution);

		beginTime = System.currentTimeMillis();
		clustering = null;
		maxModularity = Double.NEGATIVE_INFINITY;
		random = new Random(randomSeed);
		for (i = 0; i < nRandomStarts; i++)
		{
			if (printOutput && (nRandomStarts > 1))
				System.out.format("Random start: %d%n", i + 1);

			VOSClusteringTechnique = new VOSClusteringTechnique(network, resolution2);

			j = 0;
			update = true;
			do
			{
				if (printOutput && (nIterations > 1))
					System.out.format("Iteration: %d%n", j + 1);

				if (algorithm == 1)
					update = VOSClusteringTechnique.runLouvainAlgorithm(random);
				else if (algorithm == 2)
					update = VOSClusteringTechnique.runLouvainAlgorithmWithMultilevelRefinement(random);
				else if (algorithm == 3)
					VOSClusteringTechnique.runSmartLocalMovingAlgorithm(random);
				j++;

				modularity = VOSClusteringTechnique.calcQualityFunction();

				if (printOutput && (nIterations > 1))
					System.out.format("Modularity: %.4f%n", modularity);
			}
			while ((j < nIterations) && update);

			if (modularity > maxModularity)
			{
				clustering = VOSClusteringTechnique.getClustering();
				maxModularity = modularity;
			}

			if (printOutput && (nRandomStarts > 1))
			{
				if (nIterations == 1)
					System.out.format("Modularity: %.4f%n", modularity);
				System.out.println();
			}
		}
		endTime = System.currentTimeMillis();

		if (printOutput)
		{
			if (nRandomStarts == 1)
			{
				if (nIterations > 1)
					System.out.println();
				System.out.format("Modularity: %.4f%n", maxModularity);
			}
			else
				System.out.format("Maximum modularity in %d random starts: %.4f%n", nRandomStarts, maxModularity);
			System.out.format("Number of communities: %d%n", clustering.getNClusters());
			System.out.format("Elapsed time: %d seconds%n", Math.round((endTime - beginTime) / 1000.0));
			System.out.println();
			System.out.println("Writing output file...");
			System.out.println();
		}

		/*        HashMap<Integer, ArrayList<Integer>> clusters = writeOutputFile(outputFileName, clustering);
        writeClusters(terms, clustersFileName, clusters);*/
		return clustering;
	}
	
	
	@SuppressWarnings("unused")
	private static void sortFile(File f) throws IOException
	{
		BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
		List<String> lineList = new ArrayList<String>();
		String inputLine;
		while ((inputLine = bufferedReader.readLine()) != null)
		{
			lineList.add(inputLine);
		}
		bufferedReader.close();
		Collections.sort(lineList);		
	}

	private static Network readInputFile(File f, EqualitySet EqSet, int modularityFunction) throws IOException
	{
		BufferedReader bufferedReader;
		double[] edgeWeight1, edgeWeight2, nodeWeight;
		int i, j=0, nEdges, nLines=0, nNodes;
		int[] firstNeighborIndex, neighbor, nNeighbors, node1, node2;
		Network network;
		String[] splittedLine;
		
		
		nLines = EqSet.edges.size();	
		/*bufferedReader = new BufferedReader(new FileReader(f));
		nLines = 0;
		while (bufferedReader.readLine() != null)
		{
			nLines++;
		}
		bufferedReader.close();*/
		
		node1 = new int[nLines];
		node2 = new int[nLines];
		edgeWeight1 = new double[nLines];
		i = -1;
		for (Entry<String, Integer> edge : EqSet.edges.entrySet())
		{
			splittedLine = edge.getKey().split("\t");
			node1[j] = Integer.parseInt(splittedLine[0]);
			if (node1[j] > i)
				i = node1[j];
			node2[j] = Integer.parseInt(splittedLine[1]);
			if (node2[j] > i)
				i = node2[j];
			edgeWeight1[j] = edge.getValue();
			j++;
		}
		nNodes = i + 1;
		/*bufferedReader = new BufferedReader(new FileReader(f));
		for (j = 0; j < nLines; j++)
		{
			splittedLine = bufferedReader.readLine().split("\t");
			node1[j] = Integer.parseInt(splittedLine[0]);
			if (node1[j] > i)
				i = node1[j];
			node2[j] = Integer.parseInt(splittedLine[1]);
			if (node2[j] > i)
				i = node2[j];
			edgeWeight1[j] = (splittedLine.length > 2) ? Double.parseDouble(splittedLine[2]) : 1;
		}
		nNodes = i + 1;
		bufferedReader.close();*/

		nNeighbors = new int[nNodes];
		for (i = 0; i < nLines; i++)
			if (node1[i] < node2[i])
			{
				nNeighbors[node1[i]]++;
				nNeighbors[node2[i]]++;
			}

		firstNeighborIndex = new int[nNodes + 1];
		nEdges = 0;
		for (i = 0; i < nNodes; i++)
		{
			firstNeighborIndex[i] = nEdges;
			nEdges += nNeighbors[i];
		}
		firstNeighborIndex[nNodes] = nEdges;

		neighbor = new int[nEdges];
		edgeWeight2 = new double[nEdges];
		Arrays.fill(nNeighbors, 0);
		for (i = 0; i < nLines; i++)
			if (node1[i] < node2[i])
			{
				j = firstNeighborIndex[node1[i]] + nNeighbors[node1[i]];
				neighbor[j] = node2[i];
				edgeWeight2[j] = edgeWeight1[i];
				nNeighbors[node1[i]]++;
				j = firstNeighborIndex[node2[i]] + nNeighbors[node2[i]];
				neighbor[j] = node1[i];
				edgeWeight2[j] = edgeWeight1[i];
				nNeighbors[node2[i]]++;
			}

		if (modularityFunction == 1)
			network = new Network(nNodes, firstNeighborIndex, neighbor, edgeWeight2);
		else
		{
			nodeWeight = new double[nNodes];
			Arrays.fill(nodeWeight, 1);
			network = new Network(nNodes, nodeWeight, firstNeighborIndex, neighbor, edgeWeight2);
		}

		return network;
	}

	public HashMap<Integer, ArrayList<Integer>> returnClusters (Clustering clustering) throws IOException
	{
		//BufferedWriter bufferedWriter;
		int i, nNodes;

		nNodes = clustering.getNNodes();

		clustering.orderClustersByNNodes();
		HashMap<Integer, ArrayList<Integer>> clusters = new HashMap<>();
		for(int c = 0 ; c < clustering.getNClusters(); c++)
		{
			clusters.put(c, new ArrayList<>());
		}

		//bufferedWriter = new BufferedWriter(new FileWriter(fileName));
		for (i = 0; i < nNodes; i++)
		{
			Integer clusterNumber = clustering.getCluster(i);
			/*bufferedWriter.write(Integer.toString(clusterNumber));
			bufferedWriter.newLine();*/
			clusters.get(clusterNumber).add(i);
		}
		//bufferedWriter.close();
		return clusters;
	}
	
	public HashMap<Integer, Integer> assignTermsToClusters(HashMap<Integer, ArrayList<Integer>> clusters)
	{
		HashMap<Integer, Integer> termsToClusters = new HashMap<>();
		for(Entry<Integer, ArrayList<Integer>> entry : clusters.entrySet())
		{
			for(Integer termID : entry.getValue())
			{
				termsToClusters.put(termID, entry.getKey());
			}
		}
		return termsToClusters;
	}

	public HashMap<Integer, Integer> writeClusters(TreeMap<Integer, String> hmp, String fileName,  HashMap<Integer, ArrayList<Integer>> clusters) throws IOException
	{
		HashMap<Integer, Integer> termsToClusters = new HashMap<>();
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName));
		bufferedWriter.write("## Total Number of Communities = " + Integer.toString(clusters.size()) + " ##");
		bufferedWriter.newLine(); bufferedWriter.newLine();
		int numberOfTerms = 0;
		byte[] serializedIdentityClosureID = serializeObject(identityClosureID);
		List<String> communities  = new LinkedList<>();
		for(Integer c : clusters.keySet())
		{
			List<Integer> nodesID = clusters.get(c);
			List<String> nodesNames = new LinkedList<>();
			numberOfTerms = numberOfTerms + nodesID.size();
			bufferedWriter.write("-- Community " + Integer.toString(c) + " --  (size = " + Integer.toString(nodesID.size()) + ")");
			bufferedWriter.newLine();			
			for(Integer n : nodesID)
			{
				String x = hmp.get(n);
				insert(nodesNames, x);			
				termsToClusters.put(n, c);
				bufferedWriter.write(x);
				bufferedWriter.newLine();
			}			
			try 
			{
				String community = hashCommunity(nodesNames);
				communities.add(community);		
				byte[] serializedCommunity = community.getBytes();
				byte[] serializedTerms = serializeObject(nodesNames);
				MyDB.addToDB(serializedCommunity, serializedTerms, ROCKS_DB_Community2Terms);
				MyDB.addListToDB(nodesNames, serializedCommunity, ROCKS_DB_Term2Community);
			} catch (NoSuchAlgorithmException e) 
			{
				e.printStackTrace();
			}
			bufferedWriter.newLine(); bufferedWriter.newLine();
		}
		byte[] serializedCommunities = serializeObject(communities);
		MyDB.addToDB(serializedIdentityClosureID, serializedCommunities, ROCKS_DB_ID2Communities);
		bufferedWriter.write("## Total Number of Terms = " + Integer.toString(numberOfTerms) + " ##");
		bufferedWriter.close();
		return termsToClusters;
	}


	public static void insert(List<String> terms, String x) 
	{
		int pos = Collections.binarySearch(terms, x);
		if (pos < 0) 
		{
			terms.add(-pos-1, x);
		}
	}

	// Serialize an object to a byte array
	public static byte[] serializeObject(Object obj)
	{
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bout);
			out.writeObject(obj);
			out.close();
			return bout.toByteArray();
			/*String result = new String(bout.toByteArray());
			//System.out.println("Data Serialized");
			return result.getBytes();*/
		}catch(IOException i) 
		{
			i.printStackTrace();
			return null;
		}
	}

	// De-serialize a byte array to object
	public static Object deSerializeObject(byte[] byteValue)
	{
		try {
			Object obj = null;
			ByteArrayInputStream bin = new ByteArrayInputStream(byteValue);
			ObjectInputStream in = new ObjectInputStream(bin);
			try 
			{
				obj = in.readObject();
			} catch (ClassNotFoundException e) 
			{
				e.printStackTrace();
			}
			in.close();
			return obj;
		}catch(IOException i) {
			i.printStackTrace();
			return null;
		}
	}	

	public static String hashCommunity(List<String> list) throws NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] serializedList = serializeObject(list);
		md.update(serializedList);
		byte byteData[] = md.digest();
		//convert the byte to hex format method 
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < byteData.length; i++) 
		{
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}
		//System.out.println("Digest(in hex format):: " + sb.toString());
		return sb.toString();
	}

}
