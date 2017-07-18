import java.io.*;
import java.util.*;


public class Acyclic {
	/* Stores nodes that are unvisited(White), currently being visited(Gray)
	 * and have been visited(Black).
	 * */
	private Stack<Integer> White, Gray, Black;
	
	public Acyclic(int adj_matrix[][]) {
		White = new Stack<Integer>();
		Gray = new Stack<Integer>();
		Black = new Stack<Integer>();
		// Initialize the "White" set(i.e. unvisited nodes).
		for (int n = 0;n < adj_matrix.length;n++){
			White.push(n);
		}
	}
	
	/*	This method checks the given graph if there are any cycles. It uses
	 *	the DFS algorithm to do this.
	 *
	 *	Input:	Adjacency matrix that stores the edges that connects each node.
	 *			Adj_matrix[i][j] returns the weight of the edge that connects
	 *			node 'i' to node 'j'. If the entry adj_matrix[i][j] = 0 then 
	 *			there is no edge that connects node 'i' to node 'j', otherwise
	 *			there is an edge that connects the two nodes.
	 *
	 *	Output:	Returns 0 if there are no cycles detected, otherwise 1. 
	 *	*/
	public int DFS(int adj_matrix[][]){
		int hasCycle = 0;
		
		while (!White.isEmpty()) {
			Gray.push(White.pop());
			if (DFS_helper(adj_matrix, Gray.peek()) == 1) {
				hasCycle = 1;
				return hasCycle;
			}
			// Mark the starting node as visited
			Black.push(Gray.pop());
		}
		
		return hasCycle;
	}
	
	/* Helper function for the DFS algorithm. This function implements the DFS
	 * algorithm.
	 * 
	 * Input:	Adjacency matrix(adj_matrix) that stores the edges of the graph 
	 * 			and the starting node(start).
	 * 
	 * Output:	Returns 0 if there are no cycles detected, otherwise 1.
	 * */
	public int DFS_helper(int adj_matrix[][],int start) {
		Stack<Integer> neighbors = new Stack<Integer>();
		int node = 0;
		
		// Get neighbors of the "start" node
		for (int i = 0;i < adj_matrix[start].length;i++) {
			if (adj_matrix[start][i] != 0) {
				neighbors.push(i);
			}
		}
		
		while (!neighbors.empty()) {
			node = neighbors.pop();
			if (Gray.contains(node)) {
				// There is a cycle
				return 1;
			}
			
			// Unvisited neighbor/node
			if (!Black.contains(node)) {
				// Add the node to currently being visited
				Gray.push(node);
				// There is a cycle
				if (DFS_helper(adj_matrix,node) == 1) {
					return 1;
				}
				// Mark node as visited(i.e. move from Gray to Black)
				node = Gray.pop();
				Black.push(node);
				// Remove node in the unvisited node
				White.removeElement(node);
			}
		}
		
		return 0;
	}
	
	public static void main(String[] args) throws IOException {
		int len = 0;
		int edge = 0;
		String line;
		BufferedReader inputStream = null;
		try {
			inputStream = new BufferedReader(new FileReader(args[0]));
			inputStream.mark(1000);
			while ((edge = inputStream.read()) != '\n') {
				if (edge != ' ')
					len++;
			}
			int[][] adj_matrix = new int[len][len];
			inputStream.reset();
			int i = 0;
			while ((line = inputStream.readLine()) != null) {
				for (int j = 0; j < line.length(); j++) {
					char c = line.charAt(j);
					if (Character.isDigit(c)) {
						adj_matrix[i][j/2] = Character.getNumericValue(c);
					}
				}
				i++;
			}
			
			Acyclic checkAcyclic = new Acyclic(adj_matrix);
			if (checkAcyclic.DFS(adj_matrix) == 1) {
				System.out.println("The graph is acyclic");
			}
			else {
				System.out.println("The graph is not acyclic");
			}
			
		}
		catch (FileNotFoundException e) {
			System.out.println("File " + args[0] + " was not found");
			System.out.println("or could not be opened.");
			System.exit(0);
		}
		
	}

}
