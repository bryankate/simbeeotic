package harvard.robobees.simbeeotic.environment.tools.graph;


/**
 * @author chartier
 */
public class Edge {

	private int source;
	private int dest;
	
	public Edge(int source, int dest)
	{
		this.source = source;
		this.dest = dest;
	}
	
	public int getSource()
	{
		return source;
	}
	
	public int getDest()
	{
		return dest;
	}
	
	public String toString()
	{
		return "(" + source + ", " + dest + ")";
	}
}
