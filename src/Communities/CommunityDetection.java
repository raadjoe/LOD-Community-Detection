package Communities;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import Client.EqualitySet;

public class CommunityDetection {

	public String equalitySetID;
	public String[] inputs;
	int savedValues = 0;
	int test = 0;


	public CommunityDetection(String[] inputs, EqualitySet EqSet, File equalitySetFile, File f, TreeMap<Float, Integer> allErrValues) throws IOException
	{
		if(EqSet.URItoID.size() == 2)
		{
			float errValue;
			int w = 1;
			if(EqSet.statementsCounter-EqSet.reflexiveStatementsCounter == 1)
			{
				errValue = (float) 0.5;
			}
			else
			{
				errValue = 0;
				w = 2;
			}
			//saveErroneousValue(allErrValues, errValue, w);
			try(FileWriter fw = new FileWriter(f, true);
					BufferedWriter bw = new BufferedWriter(fw);)
			{
				bw.write(EqSet.URItoID.firstKey() + " " + EqSet.URItoID.lastKey()
				+ " " + errValue + " " + w + " "  
				+ EqSet.equalitySetID + " 0\n");
			}
		}
		else
		{
			this.inputs = inputs;
			this.equalitySetID = EqSet.equalitySetID;
			ModularityOptimizer mo = new ModularityOptimizer(inputs);
			mo.identityClosureID = this.equalitySetID;

			Clustering cl = mo.detectCommunities(EqSet, equalitySetFile); // execute community detection algorithm
			HashMap<Integer, ArrayList<Integer>> clusters = mo.returnClusters(cl); // returns the list of clusters
			HashMap<Integer, Integer> termsToClusters = mo.assignTermsToClusters(clusters);
			classifyEdges(f, EqSet, clusters, termsToClusters, allErrValues);
		}

		//System.out.println("DONE");

		//HashMap<Integer, Integer> termsToClusters = mo.writeClusters(EqSet.IDtoURI, mo.outputFileName, clusters); // write the clusters on a .txt file
		//constructSameAsNetwork(equalitySetFile, "data/Communities-Graph.dot", EqSet.identityClosureID, clusters, termsToClusters, EqSet.IDtoURI);
		//writeSVG("data/Communities-Graph.dot");

		/*HashMap<Integer, ArrayList<Integer>> clusters = mo.returnClusters(cl); // returns the list of clusters
		HashMap<Integer, Integer> termsToClusters = mo.writeClusters(ICG.IDtoURI, mo.outputFileName, clusters); // write the clusters on a .txt file
		constructSameAsNetwork(ICG.f, "data/Communities-Graph.dot", ICG.identityClosureID, clusters, termsToClusters, ICG.IDtoURI);
		//constructTheCommunitiesGraph(ICG.f, "data/Communities-Graph.dot", ICG.identityClosureID, clusters, termsToClusters, ICG.IDtoURI);
		//HashMap<String, GraphEdge> edges = collectEdges(f, termsToClusters, ICG.IDtoURI, clustersInternalEdges); // returns the list of edges between the clusters
		//constructCommunitiesGraph("data/Communities-Graph.dot", "data/Edges.txt", edges); // write the edges between the clusters on a .txt file and write the .dot file 
		writeSVG("data/Communities-Graph.dot");*/
	}




	public void classifyEdges(File f,
			EqualitySet EqSet,
			HashMap<Integer, ArrayList<Integer>> clusters, 
			HashMap<Integer, Integer> termsToClusters,
			TreeMap<Float, Integer> allErrValues)
	{
		try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);)
		{
			HashMap<Integer, Integer> intraCommEdges = new HashMap<>();
			TreeMap<String, Integer> interCommEdges = new TreeMap<>();
			TreeMap<Integer, Float> measureValuesIntra = new TreeMap<>();
			TreeMap<String, Float> measureValuesInter = new TreeMap<>();
			String[] splittedLine;
			int n1, n2, w;
			for(Entry<Integer, TreeMap<Integer,Integer>> theFirst : EqSet.edges.entrySet())
			{
				n1 = theFirst.getKey();				
				for(Entry<Integer,Integer> theSecond : theFirst.getValue().entrySet())
				{
					n2 = theSecond.getKey();
					w = theSecond.getValue();

					/*splittedLine = thisEdge.getKey().split("\t");
					n1 = Integer.parseInt(splittedLine[0]);
					n2 = Integer.parseInt(splittedLine[1]);     
					w = thisEdge.getValue();*/
					int c1 = termsToClusters.get(n1);
					int c2 = termsToClusters.get(n2);
					if(c1 == c2)
					{
						if(intraCommEdges.containsKey(c1))
						{
							intraCommEdges.put(c1, intraCommEdges.get(c1)+w);
						}
						else
						{
							intraCommEdges.put(c1, w);
						}

					}
					else
					{
						String interCommunityKey = c1+"-"+c2;
						if(c2 < c1)
						{
							interCommunityKey = c2+"-"+c1;
						}
						if(interCommEdges.containsKey(interCommunityKey))
						{
							interCommEdges.put(interCommunityKey, interCommEdges.get(interCommunityKey)+w);
						}
						else
						{
							interCommEdges.put(interCommunityKey, w);
						}

					}
				}
			}

			// Intra-Links Ranking
			for(Entry<Integer, ArrayList<Integer>> cluster: clusters.entrySet())
			{
				int E_in = 0;
				int clusterID = cluster.getKey();
				if(intraCommEdges.containsKey(clusterID))
				{
					E_in = intraCommEdges.get(clusterID);
				}
				float C = cluster.getValue().size();
				float err = 1 - (E_in /(C*(C-1)));
				measureValuesIntra.put(clusterID, err);
			}

			// Inter-Links Ranking
			for(Entry<String, Integer> interCommEdge: interCommEdges.entrySet())
			{
				int E_ex = interCommEdge.getValue();
				String[] linkedCommunities = interCommEdge.getKey().split("-");
				float C1 = clusters.get(Integer.parseInt(linkedCommunities[0])).size();
				float C2 = clusters.get(Integer.parseInt(linkedCommunities[1])).size();
				float err = 1 - (E_ex /(2*C1*C2));
				measureValuesInter.put(interCommEdge.getKey(), err);
			}
			for(Entry<Integer, TreeMap<Integer,Integer>> theFirst : EqSet.edges.entrySet())
			{
				n1 = theFirst.getKey();				
				for(Entry<Integer,Integer> theSecond : theFirst.getValue().entrySet())
				{
					n2 = theSecond.getKey();
					w = theSecond.getValue();
					int c1 = termsToClusters.get(n1);
					int c2 = termsToClusters.get(n2);
					float errValue, roundedValue;
					if(c1 == c2)
					{
						errValue = measureValuesIntra.get(c1) / w ;
						roundedValue = (float) (Math.round(errValue*100.0)/100.0);
						//saveErroneousValue(allErrValues, errValue, w);
						try {
						bw.write(EqSet.IDtoURI.get(n1) + " " + EqSet.IDtoURI.get(n2) 
						+ " " + roundedValue + " " + w + " " 
						+ equalitySetID + " " + c1 + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					}
					else
					{
						String clusterID;
						if(c1 < c2)
						{
							clusterID = c1+"-"+c2;
						}
						else
						{
							clusterID = c2+"-"+c1;
						}
						errValue = measureValuesInter.get(clusterID) / w;
						roundedValue = (float) (Math.round(errValue*100.0)/100.0);
						//saveErroneousValue(allErrValues, errValue, w);
						try {
						bw.write(EqSet.IDtoURI.get(n1) + " " + EqSet.IDtoURI.get(n2) 
						+ " " + roundedValue + " " + w + " " 
						+ equalitySetID + " " + clusterID + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					}
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}


	public void saveErroneousValue(TreeMap<Float, Integer> allErrValues, float errValue, int weight)
	{
		float roundedValue = (float) (Math.round(errValue*100.0)/100.0);
		if(allErrValues.containsKey(roundedValue))
		{
			int number = allErrValues.get(roundedValue);
			allErrValues.put(roundedValue, number+weight);
		}
		else
		{
			allErrValues.put(roundedValue, weight);
		}
		savedValues = savedValues + weight;
	}


	public HashMap<Integer, String> indexTerms(HashMap<String, Integer> URItoID)
	{
		HashMap<Integer, String> IDtoURI = new HashMap<>();
		for(String t : URItoID.keySet())
		{
			IDtoURI.put(URItoID.get(t), t);
		}
		return IDtoURI;
	}








	/*public static void constructCommunitiesGraph(String dotOutput, String edgesOutput,  HashMap<String, GraphEdge> edges)
	{
		File f = openFile(dotOutput);
		File f1 = openFile(edgesOutput);
		try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			out.println("graph Communities { ");
			//out.println("edge [dir=none];");
			//out.println("node [shape = circle];");

			for(String id : edges.keySet())
			{
				GraphEdge e = edges.get(id);
				out.print("c_"+ e.community1 + " -- " + "c_"+ e.community2);
				out.println(" [label=\"" + e.statements.size() + "\"];" );
				writeEdgeStatements(f1, e);
			}			
			out.println("}");
		} 
		catch (IOException e) 
		{
			//exception handling left as an exercise for the reader
		}		
	}*/

	public static void addInternalLink(TreeMap<Integer, Integer> clustersInternalLinks, int clusterID, int weight)
	{
		if(clustersInternalLinks.containsKey(clusterID))
		{
			clustersInternalLinks.put(clusterID, clustersInternalLinks.get(clusterID)+weight);
		}
		else
		{
			clustersInternalLinks.put(clusterID, weight);
		}
	}

	public static void addInterCommunityEdge(TreeMap<String, Integer> interCommunityEdges, int one, int two, int weight)
	{
		String edgeID = one + " -- " + two;
		if(interCommunityEdges.containsKey(edgeID))
		{
			interCommunityEdges.put(edgeID, interCommunityEdges.get(edgeID)+weight);
		}
		else
		{
			interCommunityEdges.put(edgeID, weight);
		}
	}


	public static void constructSameAsNetwork(File fileInput,
			String dotOutput, 
			String closureID,
			HashMap<Integer, ArrayList<Integer>> clusters, 
			HashMap<Integer, Integer> termsToClusters, 
			TreeMap<Integer, String> IDtoURI)
	{
		TreeMap<Integer, Integer> clustersInternalLinks = new TreeMap<>();
		File f = openFile(dotOutput);
		//File f1 = openFile(edgesOutput);
		try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			out.println("graph Communities_" + closureID +" {");
			ArrayList<String> interCommEdges = new ArrayList<>();
			//out.println("node [shape=record];");
			try 
			{
				String thisLine = null;
				String[] splittedLine;
				int n1, n2, w;
				BufferedReader br = new BufferedReader(new FileReader(fileInput));
				while ((thisLine = br.readLine()) != null) 
				{
					splittedLine = thisLine.split("\t");
					n1 = Integer.parseInt(splittedLine[0]);
					n2 = Integer.parseInt(splittedLine[1]);     
					w = Integer.parseInt(splittedLine[2]);  
					int c1 = termsToClusters.get(n1);
					int c2 = termsToClusters.get(n2);
					if(c1 == c2)
					{
						//addInternalLink(clustersInternalLinks, c1, w);
						interCommEdges.add(n1 + " -- " + n2 + " [weight=\"" + w + "\", inter=\"yes\"];");
					}
					else
					{
						interCommEdges.add(n1 + " -- " + n2 + " [weight=\"" + w + "\", inter=\"no\"];");				
					}
				}
				br.close();
			} 
			catch(Exception e) 
			{
				e.printStackTrace();
			}
			// write the communities
			for(Integer clusterID : clusters.keySet())
			{
				for(Integer termID : clusters.get(clusterID))
				{
					String uri = IDtoURI.get(termID).replaceAll("\"", "") ;
					out.println(termID + " [label=\"" + uri 
							+"\", community=\"" + clusterID + "\"];");
					//out.println(termID + " [label=\"" + IDtoURI.get(termID) +"\"];");
				}
			}
			for(String e : interCommEdges)
			{
				out.println(e);
			}
			out.println("}");
			//fw.close();
		}
		catch (IOException e) 
		{
		}	
	}


	public static void constructTheCommunitiesGraph(File fileInput,
			String dotOutput, 
			String closureID,
			HashMap<Integer, ArrayList<Integer>> clusters, 
			HashMap<Integer, Integer> termsToClusters, 
			TreeMap<Integer, String> IDtoURI)
	{

		TreeMap<Integer, Integer> clustersInternalLinks = new TreeMap<>();
		TreeMap<String, Integer> interCommunityEdges = new TreeMap<>();
		File f = openFile(dotOutput);
		//File f1 = openFile(edgesOutput);
		try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			out.println("graph Communities_" + closureID +" {");
			out.println("node [shape=record];");
			try 
			{
				String thisLine = null;
				String[] splittedLine;
				int n1, n2, w;
				BufferedReader br = new BufferedReader(new FileReader(fileInput));
				while ((thisLine = br.readLine()) != null) 
				{
					splittedLine = thisLine.split("\t");
					n1 = Integer.parseInt(splittedLine[0]);
					n2 = Integer.parseInt(splittedLine[1]);     
					w = Integer.parseInt(splittedLine[2]);  
					int c1 = termsToClusters.get(n1);
					int c2 = termsToClusters.get(n2);
					if(c1 == c2)
					{
						addInternalLink(clustersInternalLinks, c1, w);
					}
					else
					{
						if(c1 < c2)
						{
							addInterCommunityEdge(interCommunityEdges, c1, c2, w);
						}
						else
						{
							addInterCommunityEdge(interCommunityEdges, c2, c1, w);
						}
						/*						out.print("n"+n1 + " -- " + "n"+n2);
						out.println(" [weight=\"" + w + "\"];" );	*/
					}
				}
				br.close();
			} 
			catch(Exception e) 
			{
				e.printStackTrace();
			}
			// write the communities
			for(Integer clusterID : clusters.keySet())
			{
				int clusterWeight = 0;
				if(clustersInternalLinks.get(clusterID) != null)
				{
					clusterWeight = clustersInternalLinks.get(clusterID);
				}
				/*out.print(clusterID + "[label=\"<f0> terms&#92;n" + clusters.get(clusterID).size());
				out.print(" |<f1> c_" + clusterID);
				out.print(" |<f2> int_links&#92;n" + clusterWeight + "\"];");*/


				out.print(clusterID + "[label=\"{# terms|" + clusters.get(clusterID).size() +"}");
				out.print(" |com(" + clusterID +")");
				out.print(" | {# int_links|" + clusterWeight + "}\"];");
				/*out.print(clusterID + " [label=\"c_" + clusterID + "\", ");
				out.println("int_links=\"" + clusterWeight + "\"];");*/
			}
			for(String e : interCommunityEdges.keySet())
			{
				out.println("	" + e + "	[label=\"" + interCommunityEdges.get(e) + "\", weight=\"" + interCommunityEdges.get(e) +"\"];");
			}
			out.println("}");
			//fw.close();
		}
		catch (IOException e) 
		{
		}	













		/*TreeMap<Integer, Integer> clustersInternalLinks = new TreeMap<>();
		File f = openFile(dotOutput);
		//File f1 = openFile(edgesOutput);
		try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			out.println("graph Communities_" + closureID +" {");
			try 
			{
				String thisLine = null;
				String[] splittedLine;
				int n1, n2, w;
				BufferedReader br = new BufferedReader(new FileReader(fileInput));
				while ((thisLine = br.readLine()) != null) 
				{
					splittedLine = thisLine.split("\t");
					n1 = Integer.parseInt(splittedLine[0]);
					n2 = Integer.parseInt(splittedLine[1]);     
					w = Integer.parseInt(splittedLine[2]);  
					int c1 = termsToClusters.get(n1);
					int c2 = termsToClusters.get(n2);
					if(c1 == c2)
					{
						addInternalLink(clustersInternalLinks, c1, w);
					}
					else
					{
						out.print("n"+n1 + " -- " + "n"+n2);
						out.println(" [weight=\"" + w + "\"];" );	
					}
				}
				br.close();
			} 
			catch(Exception e) 
			{
				e.printStackTrace();
			}
			// write the clusters
			int i = 0;
			for(Integer clusterID : clusters.keySet())
			{
				out.println("subgraph cluster_" + i + "{ ");
				out.println("style=filled;");
				out.println("color=lightgrey;");			
				int clusterWeight = 0;
				if(clustersInternalLinks.get(clusterID) != null)
				{
					clusterWeight = clustersInternalLinks.get(clusterID);
				}
				out.println("label=\"Community_" + clusterID + "\";");
				//out.println("int_links=\"" + clusterWeight + "\"];");
				ArrayList<Integer> terms = clusters.get(clusterID);
				for(Integer termID: terms)
				{
					//out.println("n"+termID + " [label=\"" + IDtoURI.get(termID) + "\"];");
					out.println("n"+termID + " [label=\"" + "bla" + "\"];");
				}
				out.println("}");
				i++;
			}
			out.println("}");
			//fw.close();
		}
		catch (IOException e) 
		{
		}	*/






		/*
		HashMap<String, GraphEdge> edges = new HashMap<>();
		String thisLine = null;
		String[] splittedLine;
		int n1, n2;
		try 
		{
			BufferedReader br = new BufferedReader(new FileReader(fileInput));
			while ((thisLine = br.readLine()) != null) 
			{
				splittedLine = thisLine.split("\t");
				n1 = Integer.parseInt(splittedLine[0]);
				n2 = Integer.parseInt(splittedLine[1]);          
				int c1 = termsToClusters.get(n1);
				int c2 = termsToClusters.get(n2);
				if(c1 != c2)
				{
					int[] sortedClusters = sort(c1, c2);
					String id = sortedClusters[0] + "-" + sortedClusters[1];
					if(edges.containsKey(id))
					{						
						edges.get(id).addStatement(terms.get(n1), terms.get(n2));
					}
					else
					{
						edges.put(id, new GraphEdge(id, sortedClusters[0], sortedClusters[1]));
						edges.get(id).addStatement(terms.get(n1), terms.get(n2));
					}
				}    
			}   
			br.close();
		} 
		catch(Exception e) 
		{
			e.printStackTrace();
		}
		return edges;*/	
	}


	/*	public static HashMap<String, GraphEdge> collectEdges(File fileInput, HashMap<Integer, Integer> termsToClusters, TreeMap<Integer, String> terms, HashMap<Integer, Integer> clustersInternalEdges)
	{
		HashMap<String, GraphEdge> edges = new HashMap<>();
		String thisLine = null;
		String[] splittedLine;
		int n1, n2;
		try 
		{
			BufferedReader br = new BufferedReader(new FileReader(fileInput));
			while ((thisLine = br.readLine()) != null) 
			{
				splittedLine = thisLine.split("\t");
				n1 = Integer.parseInt(splittedLine[0]);
				n2 = Integer.parseInt(splittedLine[1]);          
				int c1 = termsToClusters.get(n1);
				int c2 = termsToClusters.get(n2);
				if(c1 != c2)
				{
					int[] sortedClusters = sort(c1, c2);
					String id = sortedClusters[0] + "-" + sortedClusters[1];
					if(edges.containsKey(id))
					{						
						edges.get(id).addStatement(terms.get(n1), terms.get(n2));
					}
					else
					{
						edges.put(id, new GraphEdge(id, sortedClusters[0], sortedClusters[1]));
						edges.get(id).addStatement(terms.get(n1), terms.get(n2));
					}
				}    
			}   
			br.close();
		} 
		catch(Exception e) 
		{
			e.printStackTrace();
		}
		return edges;
	}*/

	public static int[] sort(int node1, int node2)
	{
		int[] nodes = new int[2];
		if (node1 - node2 < 0)
		{
			nodes[0] = node1;
			nodes[1] = node2;
		}
		else
		{
			nodes[0] = node2;
			nodes[1] = node1;
		}
		return nodes;
	}

	public static File openFile(String path)
	{
		File f = new File(path);
		try 
		{
			f.delete();
			f.createNewFile();
		} 
		catch (IOException e) {

			e.printStackTrace();
		} 
		return f;
	}


	public static void constructCommunitiesGraph(String dotOutput, String edgesOutput,  HashMap<String, GraphEdge> edges)
	{
		File f = openFile(dotOutput);
		File f1 = openFile(edgesOutput);
		try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			out.println("graph Communities { ");
			//out.println("edge [dir=none];");
			//out.println("node [shape = circle];");

			for(String id : edges.keySet())
			{
				GraphEdge e = edges.get(id);
				out.print("c_"+ e.community1 + " -- " + "c_"+ e.community2);
				out.println(" [label=\"" + e.statements.size() + "\"];" );
				writeEdgeStatements(f1, e);
			}			
			out.println("}");
		} 
		catch (IOException e) 
		{
			//exception handling left as an exercise for the reader
		}		
	}

	public static void writeEdgeStatements(File f, GraphEdge edge)
	{
		try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			String[] splittedStatement;
			out.println("Community " + edge.community1 + " <--> " + "Community " + edge.community2);
			out.println("-------------------");
			for(String statement : edge.statements)
			{
				splittedStatement = statement.split(":!:");
				out.println(splittedStatement[0] + " <owl:sameAs> " + splittedStatement[1]);
			}
			out.println("");
		}
		catch (IOException e1) 
		{
			//exception handling left as an exercise for the reader
		}	
	}

	public static void writeSVG(String dotFile)
	{

		GraphViz gv = new GraphViz();
		gv.readSource(dotFile);
		String graphType = "svg";
		File fOut = new File("data/GO." + graphType);
		gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), graphType, "dot" ), fOut );
	}

}
