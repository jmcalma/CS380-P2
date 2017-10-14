import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.lang.StringBuilder;

public class PhysLayerClient {
	private static HashMap<String, Integer> table = new HashMap<String, Integer>();
	private static String[] signal = new String[320];
	private static int[] encoded = new int[320];
	private static byte[] decoded = new byte[32];
	private static double baseline;

	public static void main(String[] args) {
		try (Socket socket = new Socket("18.221.102.182", 38002)) {
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			String reversedSignal = "";
			
			fillTable();
			baseline = getBaseline(is);
			createSignal(is);
			reversedSignal = decodeSignal();
			correctSignal(reversedSignal);
			check(is, os);

		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Disconnected from server.");
	}

	private static void fillTable() {
		table.put("11110", 0);
		table.put("01001", 1);
		table.put("10100", 2);
		table.put("10101", 3);
		table.put("01010", 4);
		table.put("01011", 5);
		table.put("01110", 6);
		table.put("01111", 7);
		table.put("10010", 8);
		table.put("10011", 9);
		table.put("10110", 10);
		table.put("10111", 11);
		table.put("11010", 12);
		table.put("11011", 13);
		table.put("11100", 14);
		table.put("11101", 15);
	}

	private static double getBaseline(InputStream is) throws IOException {
		double ave = 0;
		double sig = 0;
		System.out.println("Connected to server.");
		for(int i = 0; i < 64; i++) {
			sig = (double) is.read();
			ave += sig;
		}
		ave = ave/64.0;
		System.out.printf("Baseline established from preamble: %.2f", ave);
		System.out.println();
		return ave;
	}

	private static void createSignal(InputStream is) throws IOException {
		int val = 0;
		for(int i = 0; i < 320; i++) {
			val = (int) is.read();
			encoded[i] = val;
		}
		for(int i = 0; i < 320; i++) {
			val = encoded[i];
			if(val > baseline) {
				signal[i] = "H";
			} else {
				signal[i] = "L";
			}
		}
	}

	private static String decodeSignal() {
		StringBuilder sb = new StringBuilder();
		if(signal[0].equals("L")) {
			sb.append("0");
		} else {
			sb.append("1");
		}
		for(int i = 1; i < 320; i++) {
			if(signal[i].equals("L")) {
				if(signal[i-1].equals("L")) {
					sb.append("0");
				} else if(signal[i-1].equals("H")) {
					sb.append("1");
				}
			} else if(signal[i].equals("H")) {
				if(signal[i-1].equals("L")) {
					sb.append("1");
				} else if(signal[i-1].equals("H")) {
					sb.append("0");
				}
			}
		}
		return sb.toString();
	}

	private static void correctSignal(String sig) {
		String high = "", low = "";
		int highI = 0, lowI = 5, upper, lower;

		for(int i = 0; i < 32; i++) {
			while(highI < (5 * ((i * 2) + 1))) {
				high += sig.charAt(highI);
				highI++;
			}
			highI += 5;
			while(lowI < (10 * (i + 1))) {
				low += sig.charAt(lowI);
				lowI++;
			}
			lowI += 5;

			upper = table.get(high);
			lower = table.get(low);
			decoded[i] = (byte) (((upper << 4) | lower) & 0xFF);
			high = "";
			low = "";
		}
	}

	private static void check(InputStream is, OutputStream os) throws IOException {
		System.out.print("Received 32 bytes: ");
		for(byte b : decoded) {
			System.out.printf("%02X", b);
		}
		System.out.println();
			
		for(byte b : decoded) {
			os.write(b);
		}

		int fByte = is.read();
		if(fByte == 1) {
			System.out.println("Response good.");
		} else {
			System.out.println("Incorrect response.");
		}
	}
}