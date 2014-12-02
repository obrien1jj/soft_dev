package main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
/**
 * Converts MusicFile objects to SpectrogramImage objects,
 *  and performs comparisons.
 * 
 * @author Ariel Winton, Jansen Kantor, Nnamdi Okeke, Rani Aljondi
 *
 */
public class SignalMatcher {
	private static File temp1 = new File("/tmp/SignalMatcher/D1");
	private static File temp2 = new File("/tmp/SignalMatcher/D2");
	private static int fileNumber = 0;
	private static int EUCLIDIAN_DISTANCE_MAX = 500;
	//private static int EUCLIDIAN_DISTANCE_MAX = 3000;
	private static final int ACCEPTABLE_HASH_RANGE = 300;
	//private static final int ACCEPTABLE_HASH_RANGE = 1000;

	/**
	 * Converts MusicFile objects to SpectrogramImage objects, 
	 * and performs comparisons.
	 * 
	 * @param args MusicFiles to be compared
	 */
	public static void main(String[] args){
		// Checks that the given command line arguments are valid
		checkArgs(args);
		temp1.mkdirs();
		temp2.mkdirs();
		ArrayList<CanonicalFile> f1 = 
				createCanonicalFiles(args[0], args[1], temp1);
		ArrayList<CanonicalFile> f2 = createCanonicalFiles(args[2], 
				args[3], temp2);
		//HashMap<Integer,Fingerprint> h2
		//Location of all fingerprints for
		//This could be it.
		HashMap<Integer,Fingerprint> h1 = 
				new HashMap<Integer,Fingerprint>();
		/*
		for(CanonicalFile cf : f1) {
			Fingerprint[] fingerprints = cf.fingerprintFile();
			try {
			for(int i = 0; i < fingerprints.length; i++){
				h1.put(
						fingerprints[i].hashCode(), 
						fingerprints[i]);
			}
			}
			catch(NullPointerException e) {
				System.out.println(e);
				break;
			}

		}
		 */
		int right_count = 0;
		for(CanonicalFile cf : f1) {
			Fingerprint[] fingerprints = cf.fingerprintFile();
				for(int i = 0; i < fingerprints.length; i++){
					h1.put(
							fingerprints[i].hashCode(), 
							fingerprints[i]);
				}
				for(CanonicalFile cf2: f2){
					if(cf2.fingerprintFile()==null)
						continue;
					Fingerprint[] fingerprints2 = cf2.fingerprintFile();
					findMatches(h1, fingerprints2);

				}
				h1.clear();

		}

		/*
		for(CanonicalFile cf: f2){
			if(cf.fingerprintFile()==null)
				System.out.println();
			Fingerprint[] fingerprints2 = cf.fingerprintFile();
			System.out.println(h1.size());
			findMatches(h1, fingerprints2);
		}
		 */
		temp1.delete();
		temp2.delete();
		System.exit(0);
	}

	private static void 
	findMatches(HashMap<Integer, Fingerprint> map,
			Fingerprint[] fingerprints){
		//System.out.println("Number of fingerprints :" + fingerprints.length);
		//System.out.println("Finding matches for " + fingerprints[0].getName());
		ArrayList<String> matches = new ArrayList<String>();
		//HashMap<Integer,Boolean> covered_times = new HashMap<Integer,Boolean>
		//interesting the possibilities of this mechanic. It could make traversing it easier.
		//911 Test scaffolding: to be removed later
		int total = 0;
		int hash_misses = 0;
		int euclid_misses = 0;
		int chain_misses = 0;
		int hits = 0;
		int right_hits = 0;
		//911 End of Test Scaffolding 


		for(int i = 0; i < fingerprints.length; i++){
			ArrayList<Fingerprint> validMatches = 
					scanMap(map, fingerprints[i].hashCode());

			if(validMatches == null) {
				hash_misses += 1;
				continue;
			}
			//danger zone: so many embedded if statements and for loops.
			for(Fingerprint match : validMatches){
				total+=1;
				if(!hashAcceptable(match.hashCode(),fingerprints[i].hashCode())) {
					hash_misses += 1;
					continue;
				}

				if(compareFingerprints(match, fingerprints[i])) {
					//System.out.println(match.hashCode() + " is compared to " + fingerprints[i].hashCode());
				}
				else {
					euclid_misses += 1;
					continue;
				}
				if(!matches.contains(match.getName() + fingerprints[i].getName()) 
						&& chainCompare(match,fingerprints[i])) {

					/*
						if((catchResultF1(match, fingerprints[i])&&catchResultF2(match, fingerprints[i])))
							right_hits++;
						else hits+=1;
					 */
					//probably adds to our time effeciency.
					matches.add(match.getName() + fingerprints[i].getName());

					System.out.println("MATCH " +
							match.getName() + " " +  
							fingerprints[i].getName() 
							+ " " +
							Fingerprint.findTimeInFile(match) 
							+ " " +
							Fingerprint.findTimeInFile(
									fingerprints[i]));

					//if((catchResultF1(match, fingerprints[i])&&catchResultF2(match, fingerprints[i])))
					//     right_hits +=1;

					//}
				}
				else chain_misses += 1;

			}
		}
		/*
		System.out.println("Against song :" + fingerprints[0].getName() + " : " + right_hits +" right hits: " + "out of "+ + hits + " hits: " + " out of total: " + total);
        System.out.println(chain_misses + " misses, out of: " + total);
        System.out.println(hash_misses + " hash misses, out of: " + fingerprints.length);
        System.out.println(euclid_misses + "euclid misses, out of " + total);
		 */


	}
	/*
	private static boolean catchResultF2(Fingerprint f1, Fingerprint f2) {
		HashMap<String, Double> correct = new HashMap<String, Double>();
		correct.put("mMbm.wav bad_guy_in_yer_bar.wav", 18.0);
		correct.put("mmsm.wav Sor3508.wav", 36.8);
		correct.put("MMw.wav WhoopeeTiYiYo.wav", 39.9);
		correct.put("Mpmm.wav Piste1.wav", 101.0);

		String key = f1.getName() + " " + f2.getName();
		if(correct.get(key) == null) {
			return false;

		}
		else {
			return (Math.abs(Fingerprint.findTimeInFileDouble(f2) - correct.get(key)) <= 2);         
		}
	}
	 */
	/*
	private static boolean catchResultF1(Fingerprint f1, Fingerprint f2    ) {
		HashMap<String, Double> correct = new HashMap<String, Double>();
		correct.put("mMbm.wav bad_guy_in_yer_bar.wav", 53.5);
		correct.put("mmsm.wav Sor3508.wav", 32.0);
		correct.put("MMw.wav WhoopeeTiYiYo.wav", 74.7);
		correct.put("Mpmm.wav Piste1.wav", 37.4);

		String key = f1.getName() + " " + f2.getName();
		if(correct.get(key) == null) {
			return false;

		}
		else {
			return (Math.abs(Fingerprint.findTimeInFileDouble(f1) - correct.get(key)) <= 2);	      
		}
	}
	 */

	private static boolean hashAcceptable(int h1, int h2){

		return Math.abs(h1 - h2) <= ACCEPTABLE_HASH_RANGE;
	}

	private static boolean hashEncountered(int hash, ArrayList<Fingerprint> prevMatches){
		for(Fingerprint a: prevMatches)
			if(hashAcceptable(a.hashCode(),hash))
				return true;
			else continue;
		return false;
	}

	//checks map to see whether a range of keys exist for a given hash key.
	//If they exist, they are returned in an ArrayList.
	private static ArrayList<Fingerprint> scanMap(HashMap<Integer, 
			Fingerprint> map, int hash){
		ArrayList<Fingerprint> matches = new ArrayList<Fingerprint>();
		Fingerprint match;
		for(int i = Math.max(0, hash - ACCEPTABLE_HASH_RANGE); 
				i <= hash + ACCEPTABLE_HASH_RANGE; i++){
			if(map.containsKey(i)){
				match = map.get(i);
				if(hashAcceptable(match.hashCode(), hash))
					matches.add(match);
				else continue;
			}
			else continue;
		}

		if(matches.size() == 0){
			return null;
		} else {
			return matches;
		}
	}

	private static boolean chainCompare(Fingerprint f1, 
			Fingerprint f2){
		Fingerprint currentF1 = f1;
		Fingerprint currentF2 = f2;
		int hits = 0;

		for(int i = 1; i < 55; i++){

			currentF1 = currentF1.getNext();
			currentF2 = currentF2.getNext();	
			//am i sure? null? where is this used for?
			if(currentF1 == null || currentF2 == null) {
				return false;
			}
			//System.out.println(f1.hashCode() + " is compared to " + f2.hashCode() + "Hits: "+hits);

			if(compareFingerprints(currentF1, currentF2)) {
				hits++;
			} 

		}
		//System.out.println("ChainMatch found: " + hits + currentF2.getName() + " " + currentF1.getName());
		return ((double) hits)/55 >= 0.80;

	}


	private static boolean compareFingerprints(Fingerprint f1,
			Fingerprint f2){

		return 
				EuclidianValue.getEuclidianValue(f1.getBands(), f2.getBands())
				<= EUCLIDIAN_DISTANCE_MAX;

	}


	private static ArrayList<CanonicalFile> createCanonicalFiles
	(String mode, String target, File dir){
		// Creates lists of MusicFiles 
		ArrayList<FileWrapper> list = 
				FilesCreator.makeMusicFileList(mode, target);

		ArrayList<CanonicalFile> canonicalList = 
				new ArrayList<CanonicalFile>();

		for(FileWrapper fw: list){
			canonicalList.add(fw.convert(dir));
		}

		return canonicalList;

	}

	//	/**
	//	 * Helps compare SpectogramImage objects with different 
	//   * sampling rates
	//	 * 
	//	 * @param smaller A SpectrogramImage
	//	 * @param larger A SpectrogramImage
	//	 * @param smallerFirst Is the smaller SpectrogramImage the 
	//	 * first argument?
	//	 */
	//	private static void compareDiffSamplingRates
	//	(SimpleSpectrogramImage smaller, SimpleSpectrogramImage larger, 
	//			boolean smallerFirst){
	//		if(smaller.getSampleLength() >= larger.getSampleLength()){
	//			return;
	//		} else {
	//			boolean pass = larger.downsample(smaller.getSamplingRate(), 
	//					smaller.getSampleLength())
	//					.avgDifferenceInAmplitudes(smaller);
	//			if(pass && smallerFirst){
	//				System.out.println("MATCH " + 
	//						smaller.name + " " + larger.name);
	//			} else if (pass && !smallerFirst){
	//				System.out.println("MATCH " + 
	//						larger.name + " " + smaller.name);
	//			}
	//		}
	//	}

	/**
	 * Checks that the given command line arguments are valid
	 * 
	 * @param args Command line arguments
	 */
	private static void checkArgs(String[] args){
		if(((args.length == 4) &&
				(args[0].equals("-f") || args[0].equals("-d")) &&
				(args[2].equals("-f") || args[2].equals("-d")))){
			//Everything is fine
			return;
		} else {
			System.err.println("ERROR: Incorrect command line arguments");
			System.exit(1);
		}
	}
}
