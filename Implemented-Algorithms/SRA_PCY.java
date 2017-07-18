import java.util.*;
import java.io.*;

public class SRA_PCY {
// Number of baskets
private static int numBaskets_whole = 0;
// Number of samples taken from the whole dataset
private static int numBaskets_samples = 0;
// Number of unique items
private static int numItems = 0;
// Itemset size
private static int itemsetSize = 2;
// Support threshold
private static int minSup;
// Probability of selecting a basket
private static double prob = 0.1;       // Default value of 10%
// Stores the subset of the whole dataset
private static List<String> sample_dataset = new ArrayList<String>();

public static void main(String[] args) {
        // Count of candidate singletons
        HashMap<Integer, Integer> count_singleton = new HashMap<Integer, Integer>();
        // Stores the frequent itemsets of size k
        HashMap<String, Integer> frequent_itemsets = new HashMap<String, Integer>();
        // Stores count of candidate pairs
        HashMap<String, Integer> candidate_itemsets = new HashMap<String, Integer>();

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            usage();
            System.exit(0);
        }

        try {
            File file = new File("output.txt");
            while(!file.createNewFile()) {
                file.delete();
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            String filename = args[0];

            // Read the entire file and count the number of baskets and unique items
            setup(args);

            // Stores the candidate pairs in pass 1 of PCY
            int[] candidate_pairs_count = new int[numItems];
            Arrays.fill(candidate_pairs_count,0); // Initialize the count of each candidate pairs to be 0

            long start = System.nanoTime();

            String currentLine = "";
            // First pass
            for (String basket : sample_dataset) {
                    String[] num_str_arr = basket.split(" ");
                    for (int i = 0; i < num_str_arr.length; i++) {
                            int num = Integer.parseInt(num_str_arr[i]);
                            // Count singletons(i.e. itemsets of size 1)
                            if (count_singleton.containsKey(num)) {
                                    if (count_singleton.get(num) < minSup) {
                                            count_singleton.put(num, count_singleton.get(num) + 1);
                                    }
                            } else {
                                    count_singleton.put(num, 1);
                            }
                            // Generate and count pairs in each basket and store the count in the bucket
                            for (int j = i + 1; j < num_str_arr.length; j++) {
                                    int k = (num + Integer.parseInt(num_str_arr[j])) % numItems; // Hash function to bucket k
                                    if (candidate_pairs_count[k] < minSup) candidate_pairs_count[k]++;
                            }
                    }
            }

            // Remove non-frequent singletons
            for (Integer key : count_singleton.keySet()) {
                    if (count_singleton.get(key) == minSup) {
                            frequent_itemsets.put(String.valueOf(key),0);
                            bw.write("[" + key + "]: " + count_singleton.get(key));
                            bw.newLine();
                            bw.flush();
                    }
            }
            count_singleton = null;

            System.err.println("Found " + frequent_itemsets.size() + " frequent itemsets of size 1");
            // Determine frequent buckets and convert to Bitmap
            boolean[] bitmap = new boolean[numItems];
            Arrays.fill(bitmap, false);
            for (int i = 0; i < numItems; i++) {
                    if (candidate_pairs_count[i] == minSup) {
                            bitmap[i] = true;
                    }
            }
            candidate_pairs_count = null;

            do {
                    String[] prevCands = frequent_itemsets.keySet().toArray(new String[frequent_itemsets.size()]);
                    // Generate new candidate itemsets of size n by combining pair of frequent itemsets that
                    // has the same first n-2 items.
                    for (int i = 0; i < prevCands.length; i++) {
                            int[] A = str2num(prevCands[i]);
                            for (int j = i + 1; j < prevCands.length; j++) {
                                    // Insert n-1 items from the identified frequent itemsets to the new candidate
                                    int[] newCand = Arrays.copyOf(A, itemsetSize);
                                    // Checks if the first n-2 items of the pair being examined are the same
                                    boolean isMatch = true;
                                    int[] B = str2num(prevCands[j]);
                                    for (int k = 0; k < B.length-1; k++) {
                                            if (A[k] != B[k]) {
                                                    isMatch = false;
                                                    break;
                                            }
                                    }
                                    // The first n-2 items of the pair are the same
                                    if (isMatch) {
                                            newCand[newCand.length-1] = B[B.length-1];
                                            Arrays.sort(newCand);
                                            if (itemsetSize == 2) { // Second pass
                                                    boolean frequent_bucket = bitmap[(newCand[0] + newCand[1]) % numItems];
                                                    if (frequent_bucket) {
                                                            candidate_itemsets.put(num2str(newCand), 0);
                                                    }
                                            } else { // Third pass and onwards
                                                    candidate_itemsets.put(num2str(newCand), 0);
                                            }
                                    }
                            }
                    }

                    frequent_itemsets.clear();
                    System.err.println("Generated " + candidate_itemsets.size() + " candidate itemsets of size " + itemsetSize);

                    // long start = System.nanoTime();

                    // Each passes
                    Set<String> keyString = candidate_itemsets.keySet();
                    for (String basket : sample_dataset) {
                            int[] basket_item = str2num(basket.replaceAll("\\s+", ","));
                            for (String cand : keyString) {
                                    // Array of items in the candidate itemset
                                    String[] items = cand.split(",");
                                    boolean foundItemset = false;
                                    for (int n = 0; n < items.length; n++) {
                                            // Check if the itemset is in the basket
                                            if (basket_item[0] <= Integer.parseInt(items[0]) && basket_item[basket_item.length-1] >= Integer.parseInt(items[items.length-1])) {
                                                    int match = Arrays.binarySearch(basket_item, Integer.parseInt(items[n]));
                                                    // Found the item in the basket where that item is in the candidate itemset
                                                    if (match >= 0) {
                                                            foundItemset = true;
                                                    } else {
                                                            foundItemset = false;
                                                            break;
                                                    }
                                            } else {
                                                    break;
                                            }
                                    }
                                    // The candidate itemset is in the current basket and so increment it's count
                                    if (foundItemset) {
                                            candidate_itemsets.put(cand, candidate_itemsets.get(cand) + 1);
                                    }
                            }
                    }

                    // Remove non-frequent pairs
                    for (String key : keyString) {
                            if (candidate_itemsets.get(key) >= minSup) {
                                    frequent_itemsets.put(key, 0);
                            }
                    }
                    System.err.println("Found " + frequent_itemsets.size() + " frequent itemsets of size " + itemsetSize);
                    keyString = frequent_itemsets.keySet();
                    for (String key : keyString) {
                            // System.out.println("[" + key + "]: " + candidate_itemsets.get(key));
                            bw.write("[" + key + "]: " + candidate_itemsets.get(key));
                            bw.newLine();
                            bw.flush();
                    }

                    candidate_itemsets.clear();
                    itemsetSize++;
            } while(itemsetSize <= 4);

            long end = System.nanoTime();
            System.err.println("Execution time: " + ((double)end-(double)start)/1000000000 + "s");
            // } while(frequent_itemsets.size() > 0);
        } catch (IOException e) {
            System.err.println("Can't create file");
        }

}

// Set the support threshold to either the default value or the given value
public static void setup(String[] args) {
        try {
                String currentLine = "";
                BufferedReader br = new BufferedReader(new FileReader(args[0]));
                // Set the fraction of samples to be taken from the whole dataset
                if (args.length == 3) {
                        prob = Double.parseDouble(args[2]);
                }

                // Determine how many baskets and unique items are there
                while ((currentLine = br.readLine()) != null) {
                        // Take a sample of the whole dataset
                        if (Math.random() <= prob) {
                                sample_dataset.add(currentLine);
                                numBaskets_samples++;
                                String[] num_str_arr = currentLine.split(" ");
                                int itemNumber = Integer.parseInt(num_str_arr[num_str_arr.length-1]);
                                if (numItems < itemNumber) numItems = itemNumber;
                        }
                        numBaskets_whole++;
                }

                // Set default value of the support threshold to be 80% of the total number of baskets
                // minSup = Double.valueOf(0.8 * numBaskets_samples * 1/(prob * 10000)).intValue();
                minSup = Double.valueOf(0.8 * numBaskets_samples).intValue();
                if (args.length >= 2) {
                        try {
                                double percentage = Double.parseDouble(args[1]);
                                if (percentage <= 1 && percentage > 0) {
                                        // minSup = Double.valueOf(Double.parseDouble(args[1]) * numBaskets_samples * 1/(prob * 10000)).intValue();
                                        minSup = Double.valueOf(Double.parseDouble(args[1]) * numBaskets_samples).intValue();
                                } else {
                                        System.err.println("Support threshold must be 0 < [support_threshold] <= 1");
                                        System.exit(0);
                                }
                        } catch (NumberFormatException e) {
                                System.err.println("The support threshold is not a valid number. A valid support threshold must be between 0 and 1");
                                System.exit(0);
                        }
                }

                System.err.println("Taken " + sample_dataset.size() + " samples from a dataset with " + numBaskets_whole + " samples");
                System.err.println("Finding frequent itemsets with support threshold of " + minSup + " (i.e. " + ((double)minSup/(double)numBaskets_samples*100) + "% of the samples)");
                System.err.println("Number of baskets in the whole dataset: " + numBaskets_whole);
                System.err.println("Number of items: " + numItems);
        } catch (IOException e) {
                System.err.println("Can't read file " + args[0]);
        }
}

// Convert an array of ints into a string of ints separated by a comma
public static String num2str(int[] num) {
        String str = "";
        for (int i = 0; i < num.length; i++) {
                str += String.valueOf(num[i]) + ((i + 1 < num.length) ? "," : "");
        }

        return str;
}

// Convert a string of numbers separated by comma into an array of numbers
public static int[] str2num(String str) {
        String[] str_num = str.split(",");
        int[] num = new int[str_num.length];

        for (int i = 0; i < str_num.length; i++) {
                num[i] = Integer.parseInt(str_num[i]);
        }

        return num;
}

public static void usage() {
        System.err.println(" __________________________________________________________________ ");
        System.err.println("|                           HOW TO USE                             |");
        System.err.println("|__________________________________________________________________|");
        System.err.println("|                                                                  |");
        System.err.println("| java SRA_PCY [file]                                              |");
        System.err.println("| java SRA_PCY [file] [support]                                    |");
        System.err.println("| java SRA_PCY [file] [support] [samples]                          |");
        System.err.println("|__________________________________________________________________|");
}
}
