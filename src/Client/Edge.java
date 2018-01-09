package Client;

import java.io.Serializable;

public class Edge implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String one, two, id;

	public Edge(String one, String two, String id) 
	{
		this.one = one;
		this.two = two;
		this.id = id;
	}
}
