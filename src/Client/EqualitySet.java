package Client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.TripleString;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;



public class EqualitySet {

	public String baseURL;
	public String identityClosureID; 
	public static String protocol = "http://";
	public static int allowedResults = 100;
	public static String maxResults = "page_size=" + allowedResults;
	public TreeMap<Integer, String> IDtoURI = new TreeMap<>();
	public TreeMap<String, Integer> URItoID = new TreeMap<>();
	public TreeMap<Integer, TreeMap<Integer,Integer>> edges = new TreeMap<>();
	public int statementsCounter = 0;
	public int distinctStatementsCounter = 0;
	public int reflexiveStatementsCounter = 0;
	public String toTestTerm = "";
	public String equalitySetID = "";
	public int termsCounter;
	public String term1, term2;
	public int term1ID, term2ID;

	/*public EqualitySet(String baseURL, String identityClosureID, String fileName) throws IOException, UnirestException, URISyntaxException
	{
		System.out.println("Getting the Explicit Statements of the Equality Set '" + identityClosureID + "'");
		System.out.println("......................");
		try {
			f = File.createTempFile(fileName, ".txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.baseURL = baseURL;
		this.identityClosureID = identityClosureID;
		//readAllIdentitySets("../Identity-Sets/sameAs-implicit/id_terms.dat/id_terms.dat");
		//HDT hdt = HDTManager.mapHDT("../Explicit-Identity-Graph/sameAs_lod", null);
		//searchFromHDT("", "", "http://zh.dbpedia.org/resource/Template:User_%E5%85%A8%E7%9B%98%E8%A5%BF%E5%8C%96", hdt);
		//getALLExplicitStatementsFromIdentityClosureID(this.identityClosureID, hdt);
	}*/

	public EqualitySet(String equalitySetID, String[] terms, HDT hdt)
	{
		this.equalitySetID = equalitySetID;

		constructExplicitIdentityGraph(terms, hdt);
	}

	public EqualitySet(String equalitySetID, String[] terms, HDT hdt, String term1, String term2)
	{
		this.equalitySetID = equalitySetID;
		this.term1 = term1;
		this.term2 = term2;		
		constructExplicitIdentityGraph(terms, hdt);
	}



	public void assignID(String term, int counter, boolean removeLast)
	{
		//term.replaceAll("\"", "&quote;");
		if(removeLast == false)
		{
			if(term.startsWith("<"))
			{
				term = term.substring(1);
				//term = term.substring(0, term.length()-1);
			}
			else
			{
				term = term + ">";
			}
		}
		else
		{
			if(term.startsWith("<"))
			{
				term = term.substring(1);
				term = term.substring(0, term.length()-1);
			}
		}
		URItoID.put(term, counter);
		IDtoURI.put(counter, term);
		
		// recall experiment
		if(term.equals(term1))
		{
			term1ID = counter;
		}
		if(term.equals(term2))
		{
			term2ID = counter;
		}		
	}


	// get all the explicit identity statements
	public void getHDTexplicit(String term, int subjectId, HDT hdt) throws IOException, URISyntaxException
	{		
		Iterator<TripleString> it;	
		int objectID;
		try {
			it = hdt.search(term, "", "");
			/*			if(term.equals("http://ru.dbpedia.org/resource/Родион_Як��влевич_Малиновский"))
			{
				System.out.println("OK1");
			}
			if(term.equals("http://ru.dbpedia.org/resource/Родион_Яковлевич_Малиновский"))
			{
				System.out.println("OK1");
			}*/
			//it = hdt.search("http://ru.dbpedia.org/resource/Родион_Як��влевич_Малиновский", "", "");
			//it = hdt.search("http://ru.dbpedia.org/resource/Родион_Яковлевич_Малиновский", "", "");
			while(it.hasNext()) {
				//explicitStatementsCounter++;
				TripleString t = it.next();
				String object = t.getObject().toString();
				objectID = getID(object);
				if(objectID != 1000000)
				{
					statementsCounter++;
					if(subjectId==objectID)
					{
						reflexiveStatementsCounter++;
					}
					else
					{
						addEdge(subjectId, objectID);
					}
				}
				else
				{
					System.out.println("Get HDT explicit: TERM NOT FOUND");
				}
			}
		} catch (NotFoundException e) {
			//e.printStackTrace();
		}
	}


	public void constructExplicitIdentityGraph(String[] terms, HDT hdt)
	{		
		int size = terms.length;
		for (String term:terms) {
			if(termsCounter == size-1)
			{
				assignID(term, termsCounter, true);
			}
			else
			{
				assignID(term, termsCounter, false);
			}
			termsCounter++;
		}

		for(Entry<Integer, String> entry : IDtoURI.entrySet())
		{
			try {
				/*if(equalitySetID.equals("34283264"))
				{
					termsCounter = 1;
					for (String term:terms) {
						getHDTexplicit(term,termsCounter, hdt);
						termsCounter++;
					}
				}
				else
				{*/
				getHDTexplicit(entry.getValue(),entry.getKey(), hdt);
				//}
			} catch (IOException  | URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


		/*
		termsCounter = 0;
		for (String term:terms) {
			if(termsCounter!=0)
			{
				try {
					getHDTexplicit(term, termsCounter-1, hdt);
				} catch (IOException | UnirestException | URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			termsCounter++;
		}*/
		//writeExplicitGraph();
	}

	public void readAllIdentitySets(String filePath)
	{
		FileInputStream inputStream = null;
		Scanner sc = null;
		try {
			try {
				inputStream = new FileInputStream(filePath);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sc = new Scanner(inputStream, "UTF-8");
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				String[] dataArray = line.split(" ");
				for (String item:dataArray) { 
					System.out.print(item + "  "); 
				}
				System.out.println(line);
			}
			// note that Scanner suppresses exceptions
			if (sc.ioException() != null) {
				try {
					throw sc.ioException();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
			if (sc != null) {
				sc.close();
			}
		}
	}


	/*	// get the explicit identity statements for each term in this identity closure
	public void getALLExplicitStatementsFromIdentityClosureID(String identityClosureID, HDT hdt) throws IOException, UnirestException, URISyntaxException
	{	
		Boolean lastTerm = false;
		String requestURL = "";
		long startIndex = 0;
		int counter = 0;
		while(lastTerm != true)
		{
			startIndex++;
			String startAt = "page=" + startIndex;
			requestURL = baseURL + "term" + "?" + startAt + "&" + maxResults + "&id=" + identityClosureID;
			HttpResponse<JsonNode> jsonResponse = Unirest.get(protocol+requestURL)
					.header("accept", "application/json")
					.asJson();
			lastTerm = isLastTerm(jsonResponse.getHeaders().get("Link").get(0));
			JSONArray allobjects = jsonResponse.getBody().getArray();
			Iterator<?> iti = allobjects.iterator();
			while (iti.hasNext()) 
			{
				counter++;
				String term = (String) iti.next();
				if(term.startsWith("<"))
				{
					term = term.substring(1);
					if(term.endsWith(">"))
					{
						term = term.substring(0, term.length()-1);
					}
				}	
				//System.out.println(counter + ") " + term);
				System.out.println("#" + counter + " (id=" + getIndexedTerm(term) + ") " + term);				
				getExplicitStatementsFromHDT(term, hdt);
			}
		}
		hdt.close();
		Unirest.shutdown();
	}*/

//	// get the explicit identity statements for each term in this identity closure
//	public void getALLExplicitStatementsFromIdentityClosureID(String identityClosureID, HDT hdt) throws IOException, URISyntaxException
//	{	
//		Boolean lastTerm = false;
//		String requestURL = "";
//		long startIndex = 0;
//		int counter = 0;
//		while(lastTerm != true)
//		{
//			startIndex++;
//			String startAt = "page=" + startIndex;
//			requestURL = baseURL + "term" + "?" + startAt + "&" + maxResults + "&id=" + identityClosureID;
//			HttpResponse<JsonNode> jsonResponse = Unirest.get(protocol+requestURL)
//					.header("accept", "application/json;charset=ISO-8859-1")
//					.asJson();
//			lastTerm = isLastTerm(jsonResponse.getHeaders().get("Link").get(0));
//			JSONArray allobjects = jsonResponse.getBody().getArray();
//			Iterator<?> iti = allobjects.iterator();
//			while (iti.hasNext()) 
//			{
//				String term =  (String) iti.next();	
//				/*if(term.startsWith("<"))
//				{
//					term = term.substring(1);
//					if(term.endsWith(">"))
//					{
//						term = term.substring(0, term.length()-1);
//					}
//				}	*/
//				indexTerm(counter, term);
//				System.out.println("# id=" + counter + ": " + term);
//				counter++;
//				//getExplicitStatementsFromHDT(term, hdt);
//			}
//		}
//		for(Integer id : IDtoURI.keySet())
//		{
//			String uri = IDtoURI.get(id);
//			System.out.println("#" + id + " " + uri);
//			//getExplicitStatementsFromHDT(uri, id, hdt);
//			getExplicitStatementsFromAPI(IDtoURI.get(id), id);
//		}
//		//writeExplicitGraph();
//		hdt.close();
//		Unirest.shutdown();
//	}


	// get all the explicit identity statements
	public void searchFromHDT(String subject, String predicate, String object, HDT hdt) throws IOException, URISyntaxException
	{		
		// Search pattern: Empty string means "any"	
		Iterator<TripleString> it;	
		//int explicitStatementsCounter =0;
		//int objectID;
		try {
			it = hdt.search(subject, predicate, object);
			while(it.hasNext()) {
				//explicitStatementsCounter++;
				TripleString t = it.next();
				System.out.println(t);

				//System.out.println(explicitStatementsCounter + "- " + term + "(" + subjectId + ") -- "  + object + "(" + objectID + ")");
			}
		} catch (NotFoundException e) {
			//e.printStackTrace();
		}
	}

	// get all the explicit identity statements
	public void getExplicitStatementsFromHDT(String term, int subjectId, HDT hdt) throws IOException, URISyntaxException
	{		
		// Search pattern: Empty string means "any"	
		Iterator<TripleString> it;	
		//int explicitStatementsCounter =0;
		int objectID;
		try {
			it = hdt.search(term, "", "");
			while(it.hasNext()) {
				//explicitStatementsCounter++;
				TripleString t = it.next();
				String object = t.getObject().toString();
				objectID = getID(object);
				if(objectID != -1)
				{
					if(subjectId < objectID)
					{
						addEdge(subjectId, objectID);
					}
					else
					{
						addEdge(objectID,subjectId);
					}
					statementsCounter++;
					//System.out.println(explicitStatementsCounter + "- " + term + "(" + subjectId + ") -- "  + object + "(" + objectID + ")");
				}
				//addDataToFile(f, s, o);

			}
		} catch (NotFoundException e) {
			//e.printStackTrace();
		}
	}

//	// get the explicit identity statements in which this term is subject
//	public void getExplicitStatementsFromAPI(String term, int subjectId) throws IOException, URISyntaxException
//	{	
//		Boolean lastTerm = false;
//		String requestURL = "";
//		long startIndex = 0;
//		int objectID;
//		while(lastTerm != true)
//		{
//			startIndex++;
//			String startAt = "page=" + startIndex;
//			String encodedTerm = java.net.URLEncoder.encode(term, "UTF-8");
//			requestURL = baseURL + "triple" + "?" + startAt + "&" + maxResults + "&subject=" + encodedTerm;	
//			HttpResponse<InputStream> triples = Unirest.get(protocol+requestURL)
//					.header("accept", "application/n-triples")
//					.asBinary();
//			lastTerm = isLastTerm(triples.getHeaders().get("Link").get(0));
//			Iterator<Triple> iti =  RDFDataMgr.createIteratorTriples(triples.getBody(), Lang.NTRIPLES,  null);			
//			while (iti.hasNext()) 
//			{
//				Triple t = iti.next();
//				/*String subject = "<" + t.getSubject().toString() + ">";
//				String s = getID(subject);*/
//				String object = "<" + t.getObject().toString() + ">";
//				objectID = getID(object);
//				if(objectID != -1)
//				{
//					if(subjectId < objectID)
//					{
//						addEdge(subjectId, objectID);
//					}
//					else
//					{
//						addEdge(objectID,subjectId);
//					}
//					statementsCounter++;
//					//System.out.println(explicitStatementsCounter + "- " + term + "(" + subjectId + ") -- "  + object + "(" + objectID + ")");
//				}
//				else
//				{
//
//				}
//				//addDataToFile(f, s, o);
//
//			}
//		}
//	}

	public void addEdge(int one, int two)
	{
		if(one > two)
		{
			int three = one;
			one = two;
			two = three; 
		}
		if(edges.containsKey(one))
		{	
			if(edges.get(one).containsKey(two))
			{
				edges.get(one).put(two, edges.get(one).get(two)+1);
			}
			else
			{
				edges.get(one).put(two, 1);
				distinctStatementsCounter++;
			}
		}
		else
		{
			TreeMap<Integer, Integer> hmp = new TreeMap<>();
			hmp.put(two, 1);
			edges.put(one, hmp);
			distinctStatementsCounter++;
		}
	}

	/*public void getALLExplicitStatementsFromIdentityClosureID(String identityClosureID) throws IOException
	{	
		Boolean lastTerm = false;
		String requestURL = "";
		long startIndex = 0;
		int counter = 0;
		while(lastTerm != true)
		{
			startIndex++;
			String startAt = "page=" + startIndex;
			requestURL = baseURL + "term" + "?" + startAt + "&" + maxResults + "&id=" + identityClosureID;
			IRIFactory factory = IRIFactory.uriImplementation();
			IRI iri = factory.construct(protocol+ requestURL);	 
			URL url = new URL(iri.toASCIIString());			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			conn.setRequestProperty("charset", "UTF-8");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			conn.setRequestProperty("Accept", requestProperty);
			if (conn.getResponseCode() != 200) 
			{
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			lastTerm = isLastTerm(conn.getHeaderField("Link"));
			try
			{
				InputStream content = conn.getInputStream();
				BufferedReader in = new BufferedReader(new InputStreamReader(content, "UTF-8"));
				JSONParser parser = new JSONParser();
				JSONArray response = (JSONArray) parser.parse(in);
				Iterator<?> iti = response.iterator();
				while (iti.hasNext()) 
				{
					String t = (String) iti.next();
					counter ++;
					IRI new_iri = null;
					try
					{
						new_iri = factory.construct(t);
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
					if(new_iri!= null)
					{
						//String encoded_iri = new_iri.toASCIIString(); 
						System.out.println("#" + counter + " (id=" + getIndexedTerm(t) + ") " + t);
						getExplicitRequest(baseURL, "subject="+t, "application/n-triples; charset=UTF-8", f);
					//}
				}
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}	
			conn.disconnect();
		}
	}*/

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


	/*// get the explicit statements of a term
	public static void getExplicitRequest(String baseURL, String term, String requestProperty, File f) throws IOException, URISyntaxException
	{	
		Boolean lastTerm = false;
		String requestURL = "";
		long startIndex = 0;
		while(lastTerm != true)
		{
			startIndex++;
			String startAt = "page=" + startIndex;
			requestURL = baseURL + "explicit" + "?" + startAt + "&" + maxResults + "&" + term;
			//IRIFactory factory = IRIFactory.uriImplementation();
			IRI iri = factory.construct(protocol+ requestURL);	 
				URL url = new URL(iri.toASCIIString());
			URL url = new URL(protocol + requestURL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			conn.setRequestProperty("charset", "UTF-8");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			conn.setRequestProperty("Accept", requestProperty);
			if (conn.getResponseCode() != 200) 
			{
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			lastTerm = isLastTerm(conn.getHeaderField("Link"));
			try
			{
				InputStream content = conn.getInputStream();						
				Iterator<Triple> iti =  RDFDataMgr.createIteratorTriples(content, Lang.NTRIPLES,  null);
				IRIFactory factory = IRIFactory.uriImplementation();
				while (iti.hasNext()) 
				{
					Triple t = iti.next();					
					IRI subjectIRI = null;
					try
					{
						subjectIRI = factory.construct(t.getSubject().getURI());
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
					IRI objectIRI = null;
					try
					{
						objectIRI = factory.construct(t.getObject().getURI());
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
					if(subjectIRI!= null && objectIRI!=null)
					{
						String encoded_subject = subjectIRI.toASCIIString();
						String s = getIndexedTerm(encoded_subject);
						String encoded_object = objectIRI.toASCIIString();
						String o = getIndexedTerm(encoded_object);
						addDataToFile(f, s, o);
					}	
					//System.out.println("   --> " + t.getSubject() + " " + t.getPredicate() + " " + t.getObject());						
				}
			}

			catch (Exception e) 
			{
				e.printStackTrace();
			}	
			conn.disconnect();
		}
	}*/

	public void indexTerm(int counter, String term)
	{
		/*term.replaceAll("\"", "&quote;");
		URItoID.put(term, counter);
		IDtoURI.put(counter, term);*/
	}

	public int returnTermID(String term, int counter)
	{
		term.replaceAll("\"", "&quote;");
		if(URItoID.containsKey(term))
		{
			return URItoID.get(term);
		}
		else
		{
			URItoID.put(term, counter);
			IDtoURI.put(counter, term);
			return counter;
		}
	}


	/*public String getIndexedTerm(String term)
	{
		if(terms.containsKey(term))
		{
			return terms.get(term).toString();
		}
		else
		{
			terms.put(term, termsCounter);
			termsCounter++;
			return Integer.toString(termsCounter-1);
		}
	}
	 */
	public int getID(CharSequence term)
	{
		//((String) term).replaceAll("\"", "&quote;");
		if(URItoID.containsKey(term))
		{
			return URItoID.get(term);
		}
		else
		{
			float max=0;
			System.out.println(this.equalitySetID);
			int maxID=1000000;
			for(String s: URItoID.keySet())
			{
				StringMetric sm = StringMetrics.levenshtein();
				float res = sm.compare(term.toString(), s);
				if(res>max)
				{
					max = res;
					maxID = URItoID.get(s);
				}
			}	
			System.out.println("Get ID: TERM NOT FOUND " + term);
			return maxID;
		}
	}

	public void writeExplicitGraph(File f)
	{
		/*try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			for(String edge : edges.keySet())
			{
				out.print(edge);
				out.println("\t"+edges.get(edge));
				distinctStatementsCounter++;
			}
		}
		catch (IOException e) 
		{			

		}*/
	}


	/*	public void addDataToFile(File f, String subject, String object)
	{
		try(FileWriter fw = new FileWriter(f, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			out.print(subject+"\t");
			out.println(object);
			statementsCounter ++;
		} 
		catch (IOException e) 
		{			

		}		
	}*/


}
