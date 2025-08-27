package mlbb_Images;

import java.util.Arrays;

public class ImageLogger {
	
	private static boolean printInfo;
	
	public ImageLogger(boolean print) {
		printInfo = print;
	}
	
	public void print(String msg) {
		if (printInfo) {
			System.out.print(msg);
		}
    }
	
	public void println(String msg) {
		if (printInfo) {
			System.out.println(msg);
		}
    }
	
	public void printArrayView(byte[] arr) {
		if (printInfo) {
			if (arr.length <= 16) {
				System.out.println(Arrays.toString(arr));
			} else {
				printArrayView(arr, 5, 5);
			}
		}
    }
	
	public void printArrayView(int[] arr) {
		if (printInfo) {
			if (arr.length <= 16) {
				System.out.println(Arrays.toString(arr));
			} else {
				printArrayView(arr, 5, 5);
			}
		}
    }
	
	public void printArrayView(byte[] arr, int front, int back) {
		// Negative error
		if (front < 0 || back < 0) {
			throw new IllegalArgumentException("Front < 0 or Back < 0");
		}
		
		if (printInfo) {
			// Null pointer
	        if (arr == null) {
	            System.out.println("null");
	            return;
	        }
	        
	        // Array length
	        int n = arr.length;
	        if (n == 0) {
	            System.out.println("[]");
	            return;
	        }
	
	        StringBuilder sb = new StringBuilder("[");
	        
	        // Front part
	        int limitFront = Math.min(front, n); 
	        for (int i = 0; i < limitFront; i++) {
	            sb.append(arr[i]);
	            if (i < limitFront - 1) sb.append(", ");
	        }
	        
	        // Middle ellipsis if needed
	        if (front < n && front > 0) sb.append(", ");
	       	if (n > front + back) sb.append("...");
	        if (n > front + back && back > 0) sb.append(", ");
	
	        // Back part
	        int startBack = Math.max(n - back, front);
	        for (int i = startBack; i < n; i++) {
	            sb.append(arr[i]);
	            if (i < n - 1) sb.append(", ");
	        }
	
	        sb.append("]");
	        System.out.println(sb.toString());
		}
    }
    
	public void printArrayView(int[] arr, int front, int back) {
		// Negative error
		if (front < 0 || back < 0) {
			throw new IllegalArgumentException("Front < 0 or Back < 0");
		}
		
		if (printInfo) {
			// Null pointer
	        if (arr == null) {
	            System.out.println("null");
	            return;
	        }
	        
	        // Array length
	        int n = arr.length;
	        if (n == 0) {
	            System.out.println("[]");
	            return;
	        }
	
	        StringBuilder sb = new StringBuilder("[");
	        
	        // Front part
	        int limitFront = Math.min(front, n); 
	        for (int i = 0; i < limitFront; i++) {
	            sb.append(arr[i]);
	            if (i < limitFront - 1) sb.append(", ");
	        }
	        
	        // Middle ellipsis if needed
	        if (front < n && front > 0) sb.append(", ");
	       	if (n > front + back) sb.append("...");
	        if (n > front + back && back > 0) sb.append(", ");
	
	        // Back part
	        int startBack = Math.max(n - back, front);
	        for (int i = startBack; i < n; i++) {
	            sb.append(arr[i]);
	            if (i < n - 1) sb.append(", ");
	        }
	
	        sb.append("]");
	        System.out.println(sb.toString());
		}
    }
}
