package Client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import com.mashape.unirest.http.exceptions.UnirestException;

import Communities.GraphViz;
import RocksDB.RocksDBFunctions;

public class DatasetsRelations {

	public String baseURL;
	public String identityClosureID; 
	public File f;
	public static String protocol = "http://";
	public static int allowedResults = 100;
	public static String maxResults = "page_size=" + allowedResults;
	public HashMap<String, Integer> terms = new HashMap<>();
	public static int termsCounter = 0;
	public int statementsCounter = 0;
	private static RocksDB ROCKS_DB_Dataset_Size = null;
	private static RocksDB ROCKS_DB_Dataset_Relations = null;
	private static RocksDB ROCKS_DB_Dataset_Edges= null;
	public static RocksDBFunctions MyDB;
	public String graphOutput;


	public DatasetsRelations(String baseURL, String identityClosureID, String graphOutput) throws IOException, UnirestException, URISyntaxException
	{
		System.out.println("Getting the Relations between the Datasets");
		System.out.println("......................");

		this.graphOutput = graphOutput;
		this.baseURL = baseURL;
		this.identityClosureID = identityClosureID;
		MyDB = new RocksDBFunctions("data/Datasets_Relations/");
		//MyDB.deleteRocksDB();
		//MyDB.openRocksDB();
		ROCKS_DB_Dataset_Size = MyDB.opendb("ROCKS_DB_Dataset_Size");
		ROCKS_DB_Dataset_Relations = MyDB.opendb("ROCKS_DB_Dataset_Relations");
		ROCKS_DB_Dataset_Edges = MyDB.opendb("ROCKS_DB_Term2Community");		
		analyzeData();
		//writeSVG("data/TEST.dot");
		//getALLExplicitStatementsFromHDT();
	}


	// get all the explicit identity statements
	public void getALLExplicitStatementsFromHDT() throws IOException, UnirestException, URISyntaxException
	{	
		long startTime = System.currentTimeMillis();
		Integer totalNbStatements = 558943116;
		HDT hdt = HDTManager.mapHDT("data/sameAs_lod", null);
		// Search pattern: Empty string means "any"
		IteratorTripleString it;
		Integer explicitStatementsCounter =0;
		try {
			it = hdt.search("", "", "");
			while(it.hasNext()) {
				explicitStatementsCounter++;
				TripleString t = it.next();
				if(explicitStatementsCounter % 100000 == 0) 
				{
					System.out.print("("+explicitStatementsCounter+") ");
					Double pourcent = Double.valueOf(explicitStatementsCounter.doubleValue()/totalNbStatements.doubleValue() * 100);
					System.out.print("\r" + pourcent + " % ");
					Long endTime = System.currentTimeMillis();
					Long totalTimeMs = (endTime - startTime) ;
					System.out.println(String.format("%d min, %d sec", 
							TimeUnit.MILLISECONDS.toMinutes(totalTimeMs),
							TimeUnit.MILLISECONDS.toSeconds(totalTimeMs) - 
							TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTimeMs))));
				}
				//	System.out.println(t);
				String subjectHost = getHost(t.getSubject().toString());
				String objectHost = getHost(t.getObject().toString());			
				if(subjectHost.equals(objectHost))
				{
					byte[] key = subjectHost.getBytes();				
					byte[] size = MyDB.getValueFromDB(key, ROCKS_DB_Dataset_Size);
					if(size == null)
					{
						MyDB.addToDB(key, "1".getBytes(), ROCKS_DB_Dataset_Size);
					}
					else
					{
						int thisSize = (Integer.parseInt(new String(size, StandardCharsets.UTF_8)))+1;
						MyDB.addToDB(key, Integer.toString(thisSize).getBytes(), ROCKS_DB_Dataset_Size);
					}					
				}
				else
				{
					if(subjectHost.compareTo(objectHost)<0)
					{
						addHostRelations(subjectHost, objectHost);
					}
					else
					{
						addHostRelations(objectHost, subjectHost);
					}
				}
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		hdt.close();
	}


	/*public IGraph analyzeData()
	{
		HashMap<String, INode> datasets = new HashMap<>();
		GraphComponent graphComponent = new GraphComponent();
		IGraph graph = graphComponent.getGraph();
		try (final RocksIterator iterator = ROCKS_DB_Dataset_Relations.newIterator()) {			
			iterator.seekToFirst();
			while(iterator.isValid())
			{
				//System.out.print(new String(iterator.key()));
				try {
					byte[] edgeByte = ROCKS_DB_Dataset_Edges.get(iterator.key());
					Edge e = (Edge)MyDB.deSerializeObject(edgeByte);
					INode node1 = null;
					INode node2 = null;
					node1 = datasets.get(e.one);
					if(node1 == null)
					{
						node1 = graph.createNode(new PointD(50, 50));
						graph.addLabel(node1, e.one);
						datasets.put(e.one, node1);
					}
					node2 = datasets.get(e.two);
					if(node2 == null)
					{
						node2 = graph.createNode(new PointD(50, 50));
						graph.addLabel(node1, e.two);
						datasets.put(e.two, node2);
					}
					IEdge edge = graph.createEdge(node1, node2);
					graph.addLabel(edge, new String(iterator.value()));
				} catch (RocksDBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				iterator.next();
			}		
			//EventQueue.invokeLater(() -> new SampleApplication("Step 2 - Creating Graph Elements").initialize(graph));
		}catch (Exception e) {

		}
		return graph;
	}*/

	
	public void analyzeData()
	{
		HashMap<Integer, String> datasets = new HashMap<>();
		HashMap<Integer, Integer> datasets2 = new HashMap<>();
		HashMap<Integer, String> datasets3 = new HashMap<>();
		File f = openFile(graphOutput);
		try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			out.println("graph LOD_ID { ");
			try (final RocksIterator iterator = ROCKS_DB_Dataset_Relations.newIterator()) {			
				iterator.seekToFirst();
				while(iterator.isValid())
				{
					//System.out.print(new String(iterator.key()));
					try {
						byte[] edgeByte = ROCKS_DB_Dataset_Edges.get(iterator.key());
						Edge e = (Edge)MyDB.deSerializeObject(edgeByte);
						int hash1 = e.one.hashCode();
						if(!datasets.containsKey(hash1))
						{
							datasets.put(hash1, e.one);
						}
						int hash2 = e.two.hashCode();
						if(!datasets.containsKey(hash2))
						{
							datasets.put(hash2, e.two);
						}
						out.print(fixHash(e.one.hashCode()) + " -- " + fixHash(e.two.hashCode()));
						out.println(" [weight=\"" + new String(iterator.value()) + "\"];" );				
					} catch (RocksDBException e) {
						e.printStackTrace();
					}
					iterator.next();
				}			
			}
			try (final RocksIterator iterator2 = ROCKS_DB_Dataset_Size.newIterator()) {			
				iterator2.seekToFirst();
				while(iterator2.isValid())
				{					

					String node = new String(iterator2.key());
					int val = Integer.parseInt(new String(iterator2.value()));
					datasets2.put(node.hashCode(), val);
					datasets3.put(node.hashCode(), node);
					iterator2.next();
				}
			}
			for(Integer nHash : datasets.keySet())
			{
				int size = 0;
				if(datasets2.containsKey(nHash))
				{
					size = datasets2.get(nHash);
				}
				out.println(fixHash(nHash) + " [label=\"" + datasets.get(nHash) + "\", int_links=\"" + size + "\"];");
			}
			for(Integer nHash : datasets2.keySet())
			{
				if(!datasets.containsKey(nHash))
				{
					int size = datasets2.get(nHash);
					out.println(fixHash(nHash) + " [label=\"" + datasets3.get(nHash) + "\", int_links=\"" + size + "\"];");
				}						
			}
			out.println("}");
		} 
		catch (IOException e) 
		{
			
		}		
	}
	
	public String fixHash(int hash)
	{
		String result = Integer.toString(hash);
		return result.replaceFirst("-", "x");
	}

	public String fixHash(String hash)
	{
		return hash.replaceFirst("-", "x");
	}
	
/*	public void analyzeData()
	{
		HashMap<Integer, String> datasets = new HashMap<>();
		HashMap<Integer, Integer> datasets2 = new HashMap<>();
		HashMap<Integer, String> datasets3 = new HashMap<>();
		File f = openFile(graphOutput);
		try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			out.println("graph LOD_ID { ");
			//out.println("edge [dir=none];");
			//out.println("node [shape = circle];");
			try (final RocksIterator iterator = ROCKS_DB_Dataset_Relations.newIterator()) {			
				iterator.seekToFirst();
				while(iterator.isValid())
				{
					//System.out.print(new String(iterator.key()));
					try {
						byte[] edgeByte = ROCKS_DB_Dataset_Edges.get(iterator.key());
						Edge e = (Edge)MyDB.deSerializeObject(edgeByte);
						int hash1 = e.one.hashCode();
						if(!datasets.containsKey(hash1))
						{
							datasets.put(hash1, e.one);
							//out.println(hash1 + " [ label = \"" + e.one + "\" ] "  );
						}
						int hash2 = e.two.hashCode();
						if(!datasets.containsKey(hash2))
						{
							datasets.put(hash2, e.two);
							//out.println(hash2 + " [ label = \"" + e.two + "\" ] "  );
						}
						out.print(e.one.hashCode() + " -- " + e.two.hashCode());
						out.println(" [ label = \"" + new String(iterator.value()) + "\" ] " );				
					} catch (RocksDBException e) {
						e.printStackTrace();
					}
					iterator.next();
				}			
			}
			int min=1;
			int max =0;
			try (final RocksIterator iterator2 = ROCKS_DB_Dataset_Size.newIterator()) {			
				iterator2.seekToFirst();
				while(iterator2.isValid())
				{					

					String node = new String(iterator2.key());
					int val = Integer.parseInt(new String(iterator2.value()));
					if(val< min)
					{
						min = val;
					}
					if(val > max)
					{
						max = val;
					}
					datasets2.put(node.hashCode(), val);
					datasets3.put(node.hashCode(), node);
					iterator2.next();
				}
			}
			for(Integer nHash : datasets.keySet())
			{
				int size = 0;
				if(datasets2.containsKey(nHash))
				{
					size = datasets2.get(nHash);
				}
				float percSize = 1 + (float)(size * 4)/max;
				out.println(nHash + " [ label = \"" + datasets.get(nHash) + "\", width = \"" + percSize + "\" ] "  );
			}
			for(Integer nHash : datasets2.keySet())
			{
				if(!datasets.containsKey(nHash))
				{
					int size = datasets2.get(nHash);
					float percSize = 1 + (float)(size * 4)/max;
					out.println(nHash + " [ label = \"" + datasets3.get(nHash) + "\", width = \"" + percSize + "\" ] "  );
				}						
			}
			out.println("}");
		} 
		catch (IOException e) 
		{
			
		}		
	}*/


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


	public static void writeSVG(String dotFile)
	{
		GraphViz gv = new GraphViz();
		gv.readSource(dotFile);
		String graphType = "svg";
		File fOut = new File("data/Datasets_Graph." + graphType);
		gv.writeGraphToFile( gv.getGraph(gv.getDotSource(), graphType, "dot" ), fOut );
	}

	/*// get all the explicit identity statements from Laurens API
	public void getALLExplicitStatements(String identityClosureID) throws IOException, UnirestException, URISyntaxException
	{	
		long startTime = System.currentTimeMillis();
		Integer totalNbStatements = 558943116;
		Boolean lastTerm = false;
		String requestURL = "";
		long offset = -50;
		Integer explicitStatementsCounter =0;
		while(lastTerm != true)
		{
			offset = offset+50;
			//requestURL = baseURL + "triple" + "?" + startAt + "&" + maxResults;
			requestURL = "https://api.krr.triply.cc/datasets/MaestroGraph/identity-extension/statements.nt?fi=0&ofs="+offset+"&fw=1";			
			HttpResponse<InputStream> triples = Unirest.get(requestURL)
					.asBinary();
			lastTerm = isLastTerm(triples.getHeaders().get("Link").get(0));
			Iterator<Quad> iti =  RDFDataMgr.createIteratorQuads(triples.getBody(), Lang.NQUADS,  null);
			if(explicitStatementsCounter % 10000 == 0) 
			{
				System.out.print("("+explicitStatementsCounter+") ");
				Double pourcent = Double.valueOf(explicitStatementsCounter.doubleValue()/totalNbStatements.doubleValue() * 100);
				System.out.print("\r" + pourcent + " % ");
				Long endTime = System.currentTimeMillis();
				Long totalTimeMs = (endTime - startTime) ;
				System.out.println(String.format("%d min, %d sec", 
						TimeUnit.MILLISECONDS.toMinutes(totalTimeMs),
						TimeUnit.MILLISECONDS.toSeconds(totalTimeMs) - 
						TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTimeMs))));
			}
			while (iti.hasNext()) 
			{
				explicitStatementsCounter++;	
				Quad t = iti.next();
				String subjectHost = getHost(t.getSubject().toString());
				String objectHost = getHost(t.getObject().toString());
			//	System.out.println(t.getSubject() + "(" + subjectHost + ") -- " + t.getObject() + "(" + objectHost + ")");
				if(subjectHost.equals(objectHost))
				{
					byte[] key = subjectHost.getBytes();				
					byte[] size = MyDB.getValueFromDB(key, ROCKS_DB_Dataset_Size);
					if(size == null)
					{
						MyDB.addToDB(key, "1".getBytes(), ROCKS_DB_Dataset_Size);
					}
					else
					{
						int thisSize = (Integer.parseInt(new String(size, StandardCharsets.UTF_8)))+1;
						MyDB.addToDB(key, Integer.toString(thisSize).getBytes(), ROCKS_DB_Dataset_Size);
					}					
				}
				else
				{
					if(subjectHost.compareTo(objectHost)<0)
					{
						addHostRelations(subjectHost, objectHost);
					}
					else
					{
						addHostRelations(objectHost, subjectHost);
					}
				}
			}
		}
		Unirest.shutdown();
	}*/



	/*	// get all the explicit identity statements
	public void getALLExplicitStatements(String identityClosureID) throws IOException, UnirestException, URISyntaxException
	{	
		IRIFactory factory = IRIFactory.uriImplementation();
		long startTime = System.currentTimeMillis();
		Integer totalNbStatements = 558943116;
		Boolean lastTerm = false;
		String requestURL = "";
		long startIndex = 0;
		Integer explicitStatementsCounter =0;
		while(lastTerm != true)
		{
			startIndex++;
			requestURL = "https://api.krr.triply.cc/datasets/MaestroGraph/identity-extension/statements.nt";
			//IRIFactory factory = IRIFactory.uriImplementation();

			IRI iri = factory.construct(requestURL);	 
			URL url = new URL(iri.toASCIIString());
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("charset", "UTF-8");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			if (conn.getResponseCode() != 200) 
			{
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			lastTerm = isLastTerm(conn.getHeaderField("Link"));
			InputStream content = conn.getInputStream();						
			Iterator<Quad> iti =  RDFDataMgr.createIteratorQuads(content, Lang.NQUADS,  null);
			while (iti.hasNext()) 
			{
				//System.out.println(explicitStatementsCounter++);
				explicitStatementsCounter++;	
				Quad t = iti.next();
				String subjectHost = getHost(t.getSubject().toString());
				String objectHost = getHost(t.getObject().toString());
				if(subjectHost.equals(objectHost))
				{
					byte[] key = subjectHost.getBytes();				
					byte[] size = MyDB.getValueFromDB(key, ROCKS_DB_Dataset_Size);
					if(size == null)
					{
						MyDB.addToDB(key, "1".getBytes(), ROCKS_DB_Dataset_Size);
					}
					else
					{
						int thisSize = (Integer.parseInt(new String(size, StandardCharsets.UTF_8)))+1;
						MyDB.addToDB(key, Integer.toString(thisSize).getBytes(), ROCKS_DB_Dataset_Size);
					}					
				}
				else
				{
					if(subjectHost.compareTo(objectHost)<0)
					{
						addHostRelations(subjectHost, objectHost);
					}
					else
					{
						addHostRelations(objectHost, subjectHost);
					}
				}
			}
		}
	}*/




	public void addHostRelations(String subjectHost, String objectHost)
	{
		String id = Integer.toString((subjectHost+objectHost).hashCode());
		byte[] key = id.getBytes();
		byte[] nbRelations = MyDB.getValueFromDB(id.getBytes(), ROCKS_DB_Dataset_Relations);
		if(nbRelations == null)
		{
			MyDB.addToDB(key, "1".getBytes(), ROCKS_DB_Dataset_Relations);
			Edge e = new Edge(subjectHost, objectHost, id);						
			MyDB.addToDB(key, MyDB.serializeObject(e), ROCKS_DB_Dataset_Edges);
		}
		else
		{
			int thisNbRelations = (Integer.parseInt(new String(nbRelations, StandardCharsets.UTF_8)))+1;
			MyDB.addToDB(key, Integer.toString(thisNbRelations).getBytes(), ROCKS_DB_Dataset_Relations);
			/*			byte[] val = MyDB.getValueFromDB(key, ROCKS_DB_Dataset_Edges);
			if(val != null)
			{
				Edge e = (Edge)MyDB.deSerializeObject(val);
				System.out.println(e.id);
			}*/
		}
	}

	public void checkRemainingTime(Integer explicitStatementsCounter, Integer totalNbStatements)
	{
		Double pourcent = Double.valueOf(explicitStatementsCounter.doubleValue()/totalNbStatements.doubleValue() * 100);
		System.out.print("\r" + pourcent + " % ");
	}


	public String getHost(String term)
	{
		IRIFactory factory = IRIFactory.uriImplementation();
		if(term.startsWith("<"))
		{
			term = term.substring(1);
			if(term.endsWith(">"))
			{
				term = term.substring(0, term.length()-1);
			}
		}	
		try
		{
			IRI iri = factory.construct(term);
			String result = iri.getRawHost();
			if(result != null && !result.isEmpty())
			{
				return result;
			}
			else
			{
				result = iri.getScheme();
				if(result != null && !result.isEmpty())
				{
					return result;
				}
				else
				{
					return "null";
				}			
			}
		} catch (Exception ex)
		{
			return "null";
		}
	}


	public static Boolean isLastTerm(String link)
	{
		Boolean lastTerm = true;
		Pattern linkPattern = Pattern.compile("\\s*<(\\S+)>\\s*;\\s*rel=\"(\\S+)\",?");
		if (link != null && !"".equals(link)) 
		{
			Matcher matcher = linkPattern.matcher(link);
			while (matcher.find()) 
			{
				String foundRel = matcher.group(2);
				if(foundRel.equals("next"))
				{
					lastTerm = false;
					break;
				}
			}
		}
		return lastTerm;
	}
}

