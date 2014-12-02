package main;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

//represents the canonical file type for our comparisons
public class CanonicalFile {
	public ArrayList<Float> CANONICAL_SAMPLE_RATE = new ArrayList<Float>();
	
	public static final int SAMPLES_PER_CHUNK = 1024;
	private File file;
	private String baseFileName;
	private int sampleSize;
	private int channels;
	private float sampleRate;
	private int numChunks;

	//Creates a canonical file (does not do conversion, that 
	//should be handled in filewrapper)
	public CanonicalFile(String name, File file){
		this.file = file;
		this.baseFileName = name;
		CANONICAL_SAMPLE_RATE.add(new Float(11025.0));
		CANONICAL_SAMPLE_RATE.add(new Float(22050.0));
		CANONICAL_SAMPLE_RATE.add(new Float(44100.0));
		CANONICAL_SAMPLE_RATE.add(new Float(48000.0));
		checkCorrect();
	}

	//Checks that the file is readable and sets important fields
	private void checkCorrect(){

		AudioFileFormat fileFormat = null;

		try {
			fileFormat = AudioSystem.getAudioFileFormat(file);
		} catch (Exception e){
			System.err.println("Debug error: cannot get canonical" +
					" file's fileformat " + baseFileName);
		}

		AudioFormat format = fileFormat.getFormat();
		AudioFileFormat.Type type = fileFormat.getType();
		sampleSize = format.getSampleSizeInBits();
		channels = format.getChannels();
		sampleRate = format.getSampleRate();

		
		if 	((!format.isBigEndian()) && format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) &&
			(channels == 1 || channels == 2) &&
			(sampleSize == 8 || sampleSize == 16) &&
			(CANONICAL_SAMPLE_RATE.contains(sampleRate)) && 
			type.toString().equals("WAVE")){
			
		}else {
			System.err.println("Canonical File: Debug Error: " +
								baseFileName + 
								" isBigEndian? : "+format.isBigEndian()+
								" SampleSize : " +sampleSize+
								" Channels : " +channels+
								" Format : " +format.getEncoding().toString()+
								" sampleRate : " +sampleRate+
								" Type : " +type.toString()+
								" is not a supported format");
			System.exit(1);
		} 
	
	}

	//reads in a fragment sized chunk of data 
	//from which a FingerPrint array will be made.
	//5 seconds at a time. 5 seconds = 5 * getSample


	//reads in a chunk of data from which a FingerPrint array will be made. 
	public Fingerprint[] fingerprintFile(){
		try{
			//read audio data from whatever source (file/classloader/etc.)
			InputStream audioSrc = new FileInputStream(file.getAbsolutePath());
			//add buffer for mark/reset support
			InputStream bufferedIn = new BufferedInputStream(audioSrc);
			AudioInputStream fileIn =  AudioSystem.getAudioInputStream(bufferedIn);
			int pcmByteSize = (int) (fileIn.getFrameLength() * 
					channels * (sampleSize/8));
			fileIn.skip(44);
			int bytesToReadPerChunk = SAMPLES_PER_CHUNK * 
					(sampleSize / 8) * channels;
			int overlap = 64 * (sampleSize/8) * channels;
			int overlap_chunks = (int) Math.ceil((double)pcmByteSize / overlap);
			numChunks = (int) 
					Math.ceil((double)pcmByteSize / bytesToReadPerChunk);
			byte[] bytes = new byte[bytesToReadPerChunk];
			boolean repeat = true;
			Fingerprint[] fc = new Fingerprint[numChunks];
			Fingerprint[] fd = new Fingerprint[overlap_chunks-15]; //15 is hard coded value.
			int i = 0;
			while(repeat){
			    //fileIn.mark(bytesToReadPerChunk*2);
				int read = fileIn.read(bytes, 0, bytesToReadPerChunk);
				if(read == -1){
					break;
				} else if(read != bytesToReadPerChunk){
					fillByteArray(bytes, read);
					repeat = false;
				}
				fc[i] = new Fingerprint(convertToMonoDouble(bytes), 
						baseFileName, i);
				//fd[i] = new Fingerprint(convertToMonoDouble(bytes), 
				//        baseFileName,i);
				if (i > 0){
					fc[i-1].addNext(fc[i]);
				}
				i++;
				//fileIn.reset();
				//fileIn.skip(overlap);
			}
			
			//System.out.println((i<fd.length) + " and those numbers are : actual : " + i + "expected : " + fd.length);
			//interesting, this line, and just this line, is responsible for my woes.
			fileIn.close();
			return fc;
		} catch (Exception e) {
			System.out.println("FingerprintFile" + e);
			System.exit(1);
			return null;
		}
	}

	//Fills a byte array with zeroes from offset to the end
	private void fillByteArray(byte[] bytes, int offset){
		for(int i = offset; i < bytes.length; i++){
			bytes[i] = 0;
		}
	}

	//Converts a byte array to an array of doubles that represent the samples
	private double[] convertToMonoDouble(byte[] bytes){
		double[] samples = new double[SAMPLES_PER_CHUNK];
		int bytesPerSample = (sampleSize / 8) * channels;
		for(int i  = 0; i < SAMPLES_PER_CHUNK; i++){
			samples[i] = getSample(bytes, i*bytesPerSample);
		}
		return samples;
	}

	//Returns a single sample at an index from a byte array
	private double getSample(byte[] bytes, int index){
		int bytesPerChannel = sampleSize / 8;
		if(channels == 1){ 
			return bytesToDouble(bytes, index, bytesPerChannel);
		} else {
			return (bytesToDouble(bytes, index, bytesPerChannel) +
					bytesToDouble(bytes, index + bytesPerChannel, 
							bytesPerChannel))
							/ 2.0;
		}
	}

	//converts a given number of bytes to a double value
	private double bytesToDouble(byte[] arr, int startIndex, int len){
		ByteBuffer bb = ByteBuffer.allocate(len);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for(int i = 0; i < len; i++){
			bb.put(arr[startIndex+i]);
		}

		return (double)bb.getShort(0);   	 
	}

}
