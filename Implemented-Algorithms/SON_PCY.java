import java.util.*;
import java.io.*;

public class SON_PCY {
// Number of baskets/transactions
private static int numBaskets = 0;
// Number of unique items
private static int numItems = 0;
// Itemset size
private static int itemsetSize = 2;
// Support threshold
private static int minSup_whole;  // Whole dataset
private static int minSup_chunk;  // Chunk
// Stores the subset of the whole dataset
private static List<String> sample_dataset = new ArrayList<String>();
// Percentage of the whole file to be processed individually
private static double fraction_per_chunk = 0.1; // Default value of 10%
// Number of sample dataset in a single chunk
private static int chunk_size;

public static void main(String[] args) {
    // Read the entire file and count the number of baskets and unique items
    setup(args);
    // int tempCount = 0;
    // Name of  a file where the frequent itemsets are stored for each chunk
    int numberOfFiles = 1;
    String currentLine = "";
    try {
        long start = System.nanoTime();
        BufferedReader br = new BufferedReader(new FileReader(args[0]));

        /**** PASS 1 ****/
        // Determine the frequent itemsets on each chunk
        while ((currentLine = br.readLine()) != null) {
            // Take a sample of the whole dataset
            sample_dataset.add(currentLine);
            if (sample_dataset.size() == chunk_size) {
                System.err.println("=======================================================");
                System.err.println("Processing chunk " + numberOfFiles);
                start_PCY(String.valueOf(numberOfFiles++)); // Run the PCY algorithm on one of the chunks
                sample_dataset.clear();
                itemsetSize = 2; // Reset the candidate itemset size to be searched for
                System.err.println("DONE");
            }
        }
        numberOfFiles--;
        // If the last chunk has a size less than the variable chunk_size
        if (sample_dataset.size() != 0) {
            numberOfFiles++;
            System.err.println("=======================================================");
            System.err.println("Processing chunk " + numberOfFiles);
            start_PCY(String.valueOf(numberOfFiles));
            sample_dataset.clear();
            System.err.println("DONE");
        }

        // br.close();

        /**** IN BETWEEN PASS ****/
        System.err.println("=======================================================");
        System.err.println("Taking the union of all found itemsets");
        join_itemsets(numberOfFiles);
        System.err.println("DONE");

        /**** PASS 2 ****/
        /**** Determine the itemsets that are really frequent ****/
        System.err.println("=======================================================");
        System.err.println("PASS 2");
        br = new BufferedReader(new FileReader(args[0]));
        // Create two files to store the temporary count and a file to store the final count
        File fileTemp1 = new File("temp1Count.txt");
        File fileTemp2 = new File("temp2Count.txt");
        File finalCount = new File("finalCount.txt");
        while(!fileTemp1.createNewFile()) {
            fileTemp1.delete();
        }
        while(!fileTemp2.createNewFile()) {
            fileTemp2.delete();
        }
        while(!finalCount.createNewFile()) {
            finalCount.delete();
        }

        boolean writeTemp1 = true;
        boolean lastBasket = false;
        boolean firstBasket = true;
        String currentBasket, cand;
        BufferedWriter bwCount = new BufferedWriter(new FileWriter(fileTemp1, true));
        BufferedReader brCount = new BufferedReader(new FileReader(fileTemp2));
        System.err.println("Taking frequent itemsets with support of atleast " + minSup_whole);
        while ((currentBasket = br.readLine()) != null) {
            BufferedReader brCand = new BufferedReader(new FileReader("temp" + (numberOfFiles-1) + ".txt"));
            // Checks if we are reading the last basket/transaction
            br.mark(100000);
            if (br.readLine() == null) {
                lastBasket = true;
            }
            br.reset();
            // Read and Write file configuration
            if (lastBasket) { // Last basket
                bwCount = new BufferedWriter(new FileWriter(finalCount, true));
                if (writeTemp1) {
                    brCount = new BufferedReader(new FileReader(fileTemp2));
                } else {
                    brCount = new BufferedReader(new FileReader(fileTemp1));
                }
            } else if (writeTemp1) {    // Read from temp2Count.txt and write onto temp1Count.txt
                while(!fileTemp1.createNewFile()) { // First delete the file
                    fileTemp1.delete();
                }
                bwCount = new BufferedWriter(new FileWriter(fileTemp1, true));
                brCount = new BufferedReader(new FileReader(fileTemp2));
            } else {  // Write onto temp2Count.txt and read from temp1Count.txt
                while(!fileTemp2.createNewFile()) {
                    fileTemp2.delete();
                }
                bwCount = new BufferedWriter(new FileWriter(fileTemp2, true));
                brCount = new BufferedReader(new FileReader(fileTemp1));
            }

            int[] basket_item = str2num(currentBasket.replaceAll("\\s+", ","));
            while ((cand = brCand.readLine()) != null) {
                // Array of items in the candidate itemset
                int[] items = str2num(cand);
                boolean foundItemset = false;
                if (basket_item[0] <= items[0] && basket_item[basket_item.length-1] >= items[items.length-1]) {
                    // Check if the itemset is in the basket
                    for (int n = 0; n < items.length; n++) {
                        int match = Arrays.binarySearch(basket_item, items[n]);
                        // Found the item in the basket where that item is in the candidate itemset
                        if (match >= 0) {
                            foundItemset = true;
                        } else {
                            foundItemset = false;
                            break;
                        }
                    }
                }
                // The candidate itemset is in the current basket and so increment it's count
                if (foundItemset) {
                    if (firstBasket) {
                        bwCount.write("1");
                        bwCount.newLine();
                        bwCount.flush();
                    } else {
                        int prevCount;
                        // Read the previous count, increment it and write the new count into the file
                        prevCount = Integer.parseInt(brCount.readLine().trim());
                        bwCount.write(String.valueOf(++prevCount));
                        bwCount.newLine();
                        bwCount.flush();
                    }
                } else {
                    if (firstBasket) {
                        bwCount.write("0");
                        bwCount.newLine();
                        bwCount.flush();
                    } else {
                        // Read the previous count and write it into the file
                        bwCount.write(brCount.readLine().trim());
                        bwCount.newLine();
                        bwCount.flush();
                    }
                }
            }
            bwCount.close();
            brCount.close();
            brCand.close();
            writeTemp1 = !writeTemp1;
            firstBasket = false;
        }
        // Remove non-frequent itemsets and retain the frequent
        File file = new File("freq_itemset.txt");
        while(!file.createNewFile()) {
            file.delete();
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
        // Read the found candidates and their count
        BufferedReader brItemset = new BufferedReader(new FileReader("temp" + (numberOfFiles-1) + ".txt"));
        brCount = new BufferedReader(new FileReader(finalCount));
        String itemset;
        String count;
        int x = 0;
        int currentItemsetSize = 1;
        List<Integer> numberOfFreqItemset = new ArrayList<Integer>();
        while ((count = brCount.readLine()) != null && (itemset = brItemset.readLine()) != null) {
            // If an item is found frequent then write it onto the file
            if (Integer.parseInt(count) >= minSup_whole) {
                bw.write("[" + itemset + "]: " + count);
                bw.newLine();
                bw.flush();
                if (itemset.split(",").length == currentItemsetSize) {
                    x++;
                } else {
                    numberOfFreqItemset.add(x);
                    currentItemsetSize++;
                    x = 1;
                }
            }
        }
        long end = System.nanoTime();
        System.err.println("Execution time: " + ((double)end-(double)start)/1000000000 + "s");
        numberOfFreqItemset.add(x);
        currentItemsetSize = 1;
        for (Integer i: numberOfFreqItemset) {
            System.err.println("Found " + i + " frequent itemset of size " + (currentItemsetSize++));
        }
        // Housekeeping
        while(!fileTemp1.delete()){}
        while(!fileTemp2.delete()){}
        while(!finalCount.delete()){}

    } catch (FileNotFoundException e) {
        System.err.println("Error: " + e);
    } catch (IOException e) {
        System.err.println("Error: " + e);
    }
}

// Run the PCY algorithm on a dataset and write the output on a file provided in the argument
public static void start_PCY(String filename) {
    // Count of candidate singletons
    HashMap<Integer, Integer> count_singleton = new HashMap<Integer, Integer>();
    // Stores the frequent itemsets of size k
    HashMap<String, Integer> frequent_itemsets = new HashMap<String, Integer>();
    // Stores count of candidate pairs
    HashMap<String, Integer> candidate_itemsets = new HashMap<String, Integer>();

    File file = new File(filename + ".txt");
    try { // Creates an output file
            while (!file.createNewFile()) {
                    file.delete();
            }

            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            // Stores the candidate pairs in pass 1 of PCY
            int[] candidate_pairs_count = new int[numItems];
            Arrays.fill(candidate_pairs_count,0); // Initialize the count of each candidate pairs to be 0

            String currentLine = "";
            // First pass
            for (String basket : sample_dataset) {
                String[] num_str_arr = basket.split(" ");
                for (int i = 0; i < num_str_arr.length; i++) {
                    int num = Integer.parseInt(num_str_arr[i]);
                    // Count singletons(i.e. itemsets of size 1)
                    if (count_singleton.containsKey(num)) {
                        if (count_singleton.get(num) < minSup_chunk) {
                            count_singleton.put(num, count_singleton.get(num) + 1);
                        }
                    } else {
                        count_singleton.put(num, 1);
                    }
                    // Generate and count pairs in each basket and store the count in the bucket
                    for (int j = i + 1; j < num_str_arr.length; j++) {
                        int k = (num + Integer.parseInt(num_str_arr[j])) % numItems; // Hash function
                        if (candidate_pairs_count[k] < minSup_chunk) candidate_pairs_count[k]++;
                    }
                }
            }

            // Remove non-frequent singletons
            for (Integer key : count_singleton.keySet()) {
                if (count_singleton.get(key) == minSup_chunk) {
                    frequent_itemsets.put(String.valueOf(key),0);
                    // System.out.println("[" + key + "]: " + count_singleton.get(key));
                    String freq_singleton = String.valueOf(key);
                    // Write frequent singletons on file
                    bw.write(freq_singleton, 0, freq_singleton.length());
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
                    if (candidate_pairs_count[i] == minSup_chunk) {
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
                                    boolean isMatch = true;
                                    int[] B = str2num(prevCands[j]);
                                    // Checks if the first n-2 items of the pair being examined are the same
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
                    // long end = System.nanoTime();
                    // System.out.println((end-start)/1000000);

                    // Remove non-frequent pairs
                    for (String key : keyString) {
                            if (candidate_itemsets.get(key) >= minSup_chunk) {
                                    frequent_itemsets.put(key, 0);
                            }
                    }
                    System.err.println("Found " + frequent_itemsets.size() + " frequent itemsets of size " + itemsetSize);
                    keyString = frequent_itemsets.keySet();
                    for (String key : keyString) {
                            // System.out.println("[" + key + "]: " + candidate_itemsets.get(key));
                            String freq_itemset = key;
                            // Write frequent itemsets on file
                            bw.write(freq_itemset, 0, freq_itemset.length());
                            bw.newLine();
                            bw.flush();
                    }

                    candidate_itemsets.clear();
                    itemsetSize++;
            } while(itemsetSize <= 4);
            // } while(frequent_itemsets.size() > 0);

            bw.close();
    } catch (IOException e) {
            System.err.println("Can't create output file");
            System.exit(0);
    }
}

public static void join_itemsets(int numberOfFiles) {
    int temp_file_num = 1;
    // Take union of the found frequent itemsets on each chunk
    try {
        BufferedReader br1 = new BufferedReader(new FileReader("1.txt"));
        for (int i = 2; i <= numberOfFiles; i++) {
            File file = new File("temp" + temp_file_num + ".txt");
            while (!file.createNewFile()) {
                    file.delete();
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            String currentLine1, currentLine2;
            BufferedReader br2 = new BufferedReader(new FileReader(String.valueOf(i) + ".txt"));
            List<String> set1 = new ArrayList<String>();
            List<String> set2 = new ArrayList<String>();
            int currItemsetSize = 1;
            while (true) {
                // Read from each file the itemsets with the same size
                while ((currentLine1 = br1.readLine()) != null) {
                    if (currentLine1.split(",").length != currItemsetSize) {
                        break;
                    }
                    set1.add(currentLine1);
                }
                while ((currentLine2 = br2.readLine()) != null) {
                    if (currentLine2.split(",").length != currItemsetSize) {
                        break;
                    }
                    set2.add(currentLine2);
                }

                // Join the two sets
                for (int j = 0; j < set1.size(); j++) {
                    String item = set1.get(j);
                    // Write onto the file all items in set1
                    bw.write(item);
                    bw.newLine();
                    bw.flush();
                    for (int k = 0; k < set2.size(); k++) {
                        // If an item in set1 is in set2 then delete that item in set2
                        if (item.equals(set2.get(k))) {
                            set2.remove(k);
                            break; // because we have found all items in set1 that are in set2
                        }
                    }
                }
                // If not all items in set2 are in set1
                for (int j = 0; j < set2.size(); j++) {
                    bw.write(set2.get(j));
                    bw.newLine();
                    bw.flush();
                }
                currItemsetSize++;
                set1.clear();
                set2.clear();

                if (currentLine1 == null) {
                    if (currentLine2 == null) {
                        break;
                    }
                    set2.add(currentLine2);
                    continue;
                }
                set1.add(currentLine1);

            }
            bw.close();
            br2.close();
            br1.close();
            // Ready the newly created file to be read
            br1 = new BufferedReader(new FileReader("temp" + temp_file_num + ".txt"));

            // Housekeeping
            new File(String.valueOf(i) + ".txt").delete();
            if (i == 2) {
                new File("1.txt").delete();
            } else {
                new File("temp" + (temp_file_num - 1) + ".txt").delete();
            }
            temp_file_num++;
        }
    } catch (IOException e) {
        System.err.println("File can't be read or written");
    }
}

// Set the support threshold to either the default value or the given value
public static void setup(String[] args) {
    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
        usage();
        System.exit(0);
    }

    try {
        String currentLine = "";
        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        // Set the fraction of samples to be taken from the whole dataset
        if (args.length == 3) {
            fraction_per_chunk = Double.parseDouble(args[2]);
        }

        // Determine how many baskets and unique items are there
        while ((currentLine = br.readLine()) != null) {
            String[] num_str_arr = currentLine.split(" ");
            int itemNumber = Integer.parseInt(num_str_arr[num_str_arr.length-1]);
            if (numItems < itemNumber) numItems = itemNumber;
            numBaskets++;
        }

        chunk_size = Double.valueOf(numBaskets * fraction_per_chunk + 1).intValue();
        // Set default value of the support threshold to be 80% of the total number of baskets
        minSup_whole = Double.valueOf(0.8 * numBaskets + 1).intValue();
        minSup_chunk = Double.valueOf(0.8 * chunk_size + 1).intValue();
        if (args.length >= 2) {
            try {
                double percentage = Double.parseDouble(args[1]);
                if (percentage <= 1 && percentage > 0) {
                    minSup_whole = Double.valueOf(percentage * numBaskets).intValue();
                    minSup_chunk = Double.valueOf(percentage * chunk_size).intValue();
                } else {
                    System.err.println("Support threshold must be 0 < [support_threshold] <= 1");
                    System.exit(0);
                }
            } catch (NumberFormatException e) {
                System.err.println("The support threshold is not a valid number. A valid support threshold must be between 0 and 1");
                System.exit(0);
            }
        }

        br.close();
        System.err.println("Processing each chunk of size " + chunk_size + " from a dataset with " + numBaskets + " samples");
        System.err.println("Finding frequent itemsets with support threshold of " + minSup_chunk);
        // System.out.println("Number of baskets in the whole dataset: " + numBaskets);
        // System.out.println("Number of items: " + numItems);
    } catch (IOException e) {
        System.out.println("Can't read file " + args[0]);
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
        System.err.println("| java SON_PCY [file]                                              |");
        System.err.println("| java SON_PCY [file] [support]                                    |");
        System.err.println("| java SON_PCY [file] [support] [chunks_fraction]                  |");
        System.err.println("|__________________________________________________________________|");
}
}
