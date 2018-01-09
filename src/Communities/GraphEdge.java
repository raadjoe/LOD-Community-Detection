package Communities;
import java.util.ArrayList;

public class GraphEdge 
{
	String id;
	int community1;
	int community2;
	ArrayList<String> statements;
	
	public GraphEdge(String id, int community1, int community2)
	{
		this.id = id;
		this.community1 = community1;
		this.community2 = community2;
		statements = new ArrayList<>();
	}
	
	public void addStatement(String resource1, String resource2)
	{
		String s = generateStatement(resource1, resource2);
		if(!statements.contains(s))
		{
			statements.add(s);
		}
	}
	
	public static String generateStatement(String resource1, String resource2)
	{
		String statement = resource1 + ":!:" + resource2;
		/*if (resource1.compareTo(resource2) < 0)
		{
			statement = resource1 + ":!:" + resource2;
		}
		else
		{
			statement = resource2 + ":!:" + resource1;
		}*/
		return statement;
	}
	
}
