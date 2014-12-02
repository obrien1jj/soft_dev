package main;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//Represents the fingerprint of a chunk
public class Fingerprint {

	//We are keeping this value constant for now
	private static final double LOG2 = Math.log(2);
	private static final double TIME_OF_ONE_CHUNK = (1024.0 / 11025.0); 		
	
	private static final DecimalFormat df = new DecimalFormat("#.#");

	//Points to the next fingerprint or to null
	private Fingerprint next;
	private int[] fingerprint; 	//Array of cut down chunk.
	private String name;	// Name of the song
	private int location; 	//Location in a file as the index of a chunk
	private static final int NUMBER_OF_BANDS = 10;
	private static final double PRIMARY_SCALE_MAX = 50000;
	private static final double HASH_SCALE_MAX = 5001;
	private static final int P = (int) (Math.pow(2, 32)-5);

	public Fingerprint(double[] samples, String name, int location) {
		fingerprint = new int[NUMBER_OF_BANDS];
		double[] amplitudes = new double[samples.length];
		FFT.transform(samples, new double[samples.length],
				new double[samples.length], amplitudes);
		//bandFilter(Arrays.copyOfRange(amplitudes,0,(samples.length/2)));
		bandFilter(amplitudes);
		scale();
		this.name = name;
		this.location = location;
		this.next = null;
	}
	
   public static double findTimeInFileDouble(Fingerprint f){
        return (double)f.location * TIME_OF_ONE_CHUNK;
    }


	public static String findTimeInFile(Fingerprint f){
		return df.format((double)f.location * TIME_OF_ONE_CHUNK);
	}

	//Adds a pointer to the next fingerprint chunk
	public void addNext(Fingerprint f){
		next = f;
	}

	// returns the next fingerprint chunk
	public Fingerprint getNext(){
		return next;
	}

	/*
	 * 
	 * 
	 * sums of amplitudes over ten bands of frequencies.
	 * So:
	 * band[0] = amplitudes[0]
	 * band[1] = amplitudes[1] + amplitudes[2];
	 * band[2] = amplitudes[3]+[4]+[5]+[6]
	 * ...
	 * band[10] = amplitudes[511] +...+amplitudes[1023]
	 */
	public void bandFilter(double[] amplitudes) {
		int window = 1;
		int pointer = 1;
		for(int i = 0; i < fingerprint.length; i++){
			fingerprint[i] = addBand(pointer, window, amplitudes);
			pointer += window;
			window *= 2;
		}

	}

	//Helper for above, adds up a given number of elements
	private int addBand(int start, int length, double[] amplitudes){
		int acc = 0;
		for(int i = start; i < start + length; i++){
			acc += amplitudes[i];
		}
		return acc;
	}

	//Scales the band-filtered fingerprint to be between 0 and 
	//PRIMARY_SCALE_MAX
	private void scale(){
		int largest = findLargest(fingerprint);
		double scaleFactor = HASH_SCALE_MAX / ((double)largest);
		for(int i = 0; i < fingerprint.length; i++){
			fingerprint[i] *= scaleFactor;
		}
	}

	//Gets the bands
	public int[] getBands() {
		return fingerprint;
	}

	//Sets the bands
	public void setBands(int[] bands) {
		this.fingerprint = bands;
	}

	//Gets the name of the file
	public String getName() {
		return name;
	}

	//Sets the name of the file
	public void setName(String name) {
		this.name = name;
	}

	//Gets the index of the file
	public int getLocation() {
		return location;
	}

	//which means our hash will *probably* 
	//reflect the content and the order.
	//Creates the hash code
	/*
	private static final int[] HASH_CONSTANTS = 
			new int[]{10, 12, 21, -22, -21, 23, 25, -22, 24, 22};

					int l = findLargest(fingerprint);

		for (int i = 0; i < fingerprint.length; i++) {
			result +=  HASH_CONSTANTS[i] * (fingerprint[i] + 
					((double)(l/HASH_SCALE_MAX)));  
		}

		return (int)result;
	}

	 */
	private static final int[] HASH_CONSTANTS = 
			//new int[] {1,2,4,8,16,32,64,128,256,512};
			//new int[]{10, 12, 18, 19, 20, 23, 25, 21, 5, 1};
	           new int[]{10, 12, 18, -19, -20, 23, 25, -21, 24, 22};
	          // new int[]{5, 6, 9, -9, -10, 11, 13, -11, 12, 11};
            //new int[]{1, 1, 1, 3, 2, 7, 8, 7, 0, 0};   
	        //new int[]{2, 3, 5, -5, -5, 6, 7, -5, 6, 5};
	            //new int[]{1, 1, 1, 1, 1, 1, 1, 1, 0, 0};

	        //new int[] {1,2,4,8,16,32,128,256,0,0};
			//0: 0-1 : 
			//1: 2-4
			//2: 4-8
			//3: 8-16
			//4: 16-32
			//5: 32-64
			//6: 64-128
			//7: 128-256
			//8: 256-512
			//9: 512-1024
	public int hashCode() {
		int result = 0;
		int l = findLargest(fingerprint);
		
		if(l == 0) {
			return 0;
		}
		else { 
			for (int i = 0; i < fingerprint.length; i++) {
					result +=  HASH_CONSTANTS[i] * (fingerprint[i]);  		
			}
		}
        //System.out.println("Result: " + (int) (((int)result % P) % HASH_SCALE_MAX));
		return (int) ((Math.abs(result) % P) % HASH_SCALE_MAX);
	}
	
	   
    public boolean equals(Object o) {
        if(!(o instanceof Fingerprint))
            return false;
        if(o == null)
            return false;
        if(o == this)
            return true;
        else {
            Fingerprint f = (Fingerprint) o;
            int[] bandsF1 = this.getBands();
            int[] bandsF2 = f.getBands();
            for(int i = 0; i<fingerprint.length; i++)
                bandsF1[i] = bandsF2[i];
        }
        return true;
    }


	//Returns the largest element in the fingerprint
	private int findLargest(int[] target){
		int largest = target[0];
		for(int i = 1; i < target.length; i++){
			if (target[i] > largest)
				largest = target[i];
		}
		return largest;
	}
}
