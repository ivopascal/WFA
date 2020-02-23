import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.*; 
import java.util.stream.IntStream;

/**
* The Word class represents how a word may exist in the WordList that we scan for
* Each class therefore has a phrase (which may be multiple words)
* As well as how this phrase is rated for exploitation and exploration/
*
* This data is extracted from a CSV line given to the constructor.
*/
class Word{
	private String phrase;
	private float exploitScore;
	private float exploreScore;

	public Word(String line){
		String[] components = line.split(",");
		this.phrase = components[0].toLowerCase().replace("/^\\p{L}+$/u", "");
		this.exploreScore = Float.parseFloat(components[1]);
		this.exploitScore = Float.parseFloat(components[2]);
	}

	public String getPhrase(){
		return this.phrase;
	}

	public float getExploitScore(){
		return this.exploitScore;
	}

	public float getExploreScore(){
		return this.exploreScore;
	}
}

/** 
*	ReportResult holds the results from analyzing a textfile (in this case annual reports)
*	This holds the path to the file, the file itself
*	A dictionary of word counts (initialized empty)
*	Scores for the textfile in terms of exploitation and exploration (as well as their ratio)
*	
*	File and the path are extracted at constructor
*	Word frequencies and scores are determined at determineScore
*		which counts the occurances of a set of words given by the wordlist
*		and calculates the summed score
*/
class ReportResult{
	private String path;
	private Map<String, Integer> results = new HashMap<String, Integer>();
	private File f;
	private float exploitScore = 0;
	private float exploreScore = 0;
	private float exploit_explore_ratio;
	private int number_words = 0;

	public ReportResult(File f){
		this.f = f;
		this.path = f.getAbsolutePath();
	}

	public void determineScore(ArrayList<Word> wordlist){
		try(BufferedReader br = new BufferedReader(new FileReader(f))){
			// Go over all the lines in the text file
			String line;
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line);
				// Go over all the words in each line
				while(st.hasMoreTokens()) {
					String token = st.nextToken().toLowerCase().replace("/^\\p{L}+$/u", "");
					this.number_words += 1;
					//Go over all the words you want to scan for
					IntStream.range(0, wordlist.size()).parallel().forEach(i -> {
						Word word = wordlist.get(i);
						String w = word.getPhrase();

						// w may store multiple words, we want to check them starting at the first
						StringTokenizer wst = new StringTokenizer(w);
						boolean match = true;
						// Keep going until we run out of words, or we encounter a mismatch
						while(wst.hasMoreTokens() && match){
							if(!wst.nextToken().equals(token)){
								match = false;
							}
						}
						// If we find a match
						if(match){
							//Find the entry
							Integer count = results.get(w);

							//If it didn't exist create one at count 1
							if(count == null){
								count = 1;
							}else{
								// If it did exist we increase count by 1
								count = count + 1;
							}

							//Put the updated count back in the Results
							results.put(w, count);
							this.exploitScore += word.getExploitScore();
							this.exploreScore += word.getExploreScore();
						}
					});
				}
			}
		}catch (IOException e){
			e.printStackTrace();
		}
		this.exploit_explore_ratio = this.exploitScore / exploreScore;
	}



	public String getPath(){
		return this.path;
	}

	public Map getDict(){
		return this.results;
	}

	public float getExploreScore(){
		return this.exploreScore;
	}

	public float getExploitScore(){
		return this.exploitScore;
	}

	public float getRatio(){
		return this.exploit_explore_ratio;
	}

	public int getNumber_words(){
		return this.number_words;
	}
}

class WordScore{
	public static void main(String[] args){

		ArrayList<Word> wordScores = new ArrayList<Word>();
		try (BufferedReader br = new BufferedReader(new FileReader("./wordlist.csv"))) {
			// Create a Word object from each line.
			// Each Word object extracts the word(s), exploitscore and explorescore
			String line;
			while ((line = br.readLine()) != null) {
	            wordScores.add(new Word(line));
	        }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }


		File reportFolder = new File("./Textfiles");
		File[] reports = reportFolder.listFiles();
		ReportResult[] results = new ReportResult[reports.length];

		// for(int i = 0; i< reports.length; i++){
		// 	results[i] = new ReportResult(reports[i]);
		// 	results[i].determineScore(wordScores);
		// }
		IntStream.range(0, reports.length).parallel().forEach(i -> {
			// For each (annual)report create a ReportResult object
			// Each such object will have a file(path), 
				// an (empty) dictionary of words and occurances,
				// an (empty) explore and exploit score
			results[i] = new ReportResult(reports[i]);

			// DetermineScore tells the object to count the occurances of the wordScore words
			// And compute the report's exploit and explore score
			results[i].determineScore(wordScores);
		});
		printResults(results);
	}

	static void printResults(ReportResult[] results){
		for(ReportResult result : results){
			System.out.println(result.getPath());
			System.out.println("ExploreScore: " + result.getExploreScore());
			System.out.println("ExploitScore: " + result.getExploitScore());
			System.out.println("Exploit:Explore ratio " + result.getRatio() + "\n");

			for (Object key: result.getDict().keySet()){
	            String name = key.toString();
	            Object value = result.getDict().get(name);  
	            System.out.println(key + ": " + (Integer) value / (float) result.getNumber_words() + " percent");  
			} 
			System.out.println("\n");
		}
	}
}