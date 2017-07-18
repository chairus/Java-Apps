import java.io.*;
import java.util.*;

public class SCC {
	/* 
	 * Stores nodes that are unvisited(White), currently being visited(Gray)
	 * and have been visited(Black).
	 * */
	private Stack<Integer> White, Gray, Black;
	
	// Constructor
	public SCC(int adj_matrix[][]) {
		White = new Stack<Integer>();
		Gray = new Stack<Integer>();
		Black = new Stack<Integer>();
		// Initialize the "White" set(i.e. unvisited nodes).
		for (int n = 0;n < adj_matrix.length;n++){
			White.push(n);
		}
	}
	
	// Constructor
	public SCC(int[] arr) {
		White = new Stack<Integer>();
		Gray = new Stack<Integer>();
		Black = new Stack<Integer>();
		
		// Initialize the "White" set(i.e. unvisited nodes).
		for (int i = 0;i < arr.length;i++) {
			White.push(arr[i]);
		}
	}
	
	/*	
	 * This method computes the transpose of a graph G represented as an adjacency
	 * matrix.
	 * 
	 * Input:	Adjacency matrix that stores the edges that connects each node.
	 *			Adj_matrix[i][j] returns the weight of the edge that connects
	 *			node 'i' to node 'j'. If the entry adj_matrix[i][j] = 0 then 
	 *			there is no edge that connects node 'i' to node 'j', otherwise
	 *			there is an edge that connects the two nodes.
	 *
	 *	Output:	Returns a 2D matrix which is the transpose of the input 2D matrix. 
	 **/
	
	public static int[][] compute_transpose(int adj_matrix[][]) {
		int len = adj_matrix.length;
		for (int j = 1;j < len;j++) {
			for (int k = 0;k < j;k++) {
				int temp = adj_matrix[j][k];
				adj_matrix[j][k] = adj_matrix[k][j];
				adj_matrix[k][j] = temp;
			}
		}
		
		return adj_matrix;
	}
	
	/*	
	 * This method finds all the strongly connected components of the given
	 * graph. It uses the DFS algorithm to do this.
	 * 
	 * Input:	Adjacency matrix that stores the edges that connects each node.
	 *			Adj_matrix[i][j] returns the weight of the edge that connects
	 *			node 'i' to node 'j'. If the entry adj_matrix[i][j] = 0 then 
	 *			there is no edge that connects node 'i' to node 'j', otherwise
	 *			there is an edge that connects the two nodes.
	 *
	 *	Output:	Returns an array of integer which contains the sizes of all the
	 *			strongly connected components of the graph. 
	 **/
	public Vector<Integer> DFS(int adj_matrix[][]){
		Vector<Integer> SCCsizes = new Vector<Integer>();

		while (!White.isEmpty()) {
			// Mark the starting node as currently being visited
			Gray.push(White.pop());
			DFS_helper(adj_matrix,Gray.peek(),SCCsizes);
			// Mark the starting node as visited
			Black.push(Gray.pop());
		}
		
		return SCCsizes;
	}
	
	/* Helper function for the DFS algorithm. This function implements the DFS
	 * algorithm.
	 * 
	 * Input:	Adjacency matrix(adj_matrix) that stores the edges of the graph 
	 * 			and the starting node(start).
	 * 
	 * Output:	Returns a vector with the sizes of the SCC's in the graph.
	 * */
	public void DFS_helper(int adj_matrix[][],int start,Vector<Integer> SCCsizes) {
		Stack<Integer> neighbors = new Stack<Integer>();
		int node = 0;
		
		// Get neighbors of the "start" node
		for (int i = 0;i < adj_matrix[start].length;i++) {
			if (adj_matrix[start][i] != 0) {
				neighbors.push(i);
			}
		}
		
		// Found an SCC
		if (Gray.size() == 1) {
			// If this is the first component discovered in an SCC
			SCCsizes.addElement(1);
		}
		else {
			// If this component is part of an already discovered SCC with multiple components
			SCCsizes.setElementAt(SCCsizes.lastElement()+1, SCCsizes.size()-1);
		}
		
		while (!neighbors.isEmpty()) {
			node = neighbors.pop();
			if (Gray.contains(node)) {
				// Do nothing
				continue;
			}	
			
			// Unvisited neighbor/node
			if (!Black.contains(node)) {
				// Add the node to currently being visited
				Gray.push(node);
				DFS_helper(adj_matrix,node,SCCsizes);
				// Mark node as visited(i.e. move from Gray to Black)
				if (!Gray.isEmpty()) {
					node = Gray.pop();
					Black.push(node);
					// Remove node in the unvisited node
					White.removeElement(node);
				}
			}
		}
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
				if (Character.isDigit(edge))
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
			int[] finishedNodes = new int[len];
			if (checkAcyclic.DFS(adj_matrix) == 1) { // It has cycles so has SCC's with size >= 1
				finishedNodes = checkAcyclic.getNodeFinishedTime(finishedNodes);
				SCC findSCC = new SCC(finishedNodes);
				
				// Compute transpose of the graph
				adj_matrix = compute_transpose(adj_matrix);
				
				Vector<Integer> SCCsizes = findSCC.DFS(adj_matrix);
				Collections.sort(SCCsizes);
				int SCClen = SCCsizes.size();
				while (SCClen > 0) {
					System.out.print(SCCsizes.elementAt(SCClen-1));
					SCClen--;
					if (SCClen != 0)
						System.out.print(',');
				}
				System.out.println();
			}
			else { // The graph is acyclic(i.e. no cycles) so all SCC has size 1
				for (int n = 0;n < len;n++) {
					System.out.print(1);
					if (n != len -1) {
						System.out.print(',');
					}
				}
				System.out.println();
			}
		}
		catch (FileNotFoundException e) {
			System.out.println("File " + args[0] + " was not found");
			System.out.println("or could not be opened.");
			System.exit(0);
		}
	}
}
