package mlbb_Images;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class ImageLoader {
    // Symmetric key and base image
    private boolean firstDraw = true;
    private final byte[] iv = new byte[16];
    private final byte[] pad = new byte[16];
    private int width, height, keySize;
    private BufferedImage bfImage;
    private SecretKey symmetricKey = null;

    private static final int RED = 1;
    private static final int GREEN = 2;
    private static final int BLUE = 3;

    public ImageLoader(File f) {
        try {
            bfImage = ImageIO.read(f);
            bfImage = toIntARGB(bfImage);
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file.");
            bfImage = generatePlaceholder(Color.ORANGE, Color.PINK);
            bfImage = toIntARGB(bfImage);
        } catch (NullPointerException e) {
            System.out.println("Not an image file: " + f.getName());
            bfImage = generatePlaceholder(Color.ORANGE, Color.PINK);
            bfImage = toIntARGB(bfImage);
        }

        width = bfImage.getWidth();
        height = bfImage.getHeight();

        // Replace with metadata reader later
        if (f.getName().lastIndexOf('.') != -1) {
            String fn_no_ext = f.getName().substring(0, f.getName().lastIndexOf('.'));
            String[] nums = fn_no_ext.split("_", -1);

            if (nums.length > 7) {
                try {
                    keySize = Integer.parseInt(nums[nums.length - 1]);
                    if (keySize == 128 || keySize == 192 || keySize == 256) {
                        int n = keySize / 64;
                        long padLSB = Long.parseLong(nums[nums.length - n - 2]);
                        long padMSB = Long.parseLong(nums[nums.length - n - 3]);
                        long ivLSB = Long.parseLong(nums[nums.length - n - 4]);
                        long ivMSB = Long.parseLong(nums[nums.length - n - 5]);
                        long[] secretKeyLongs = new long[keySize / 64];

                        for (int i = 0; i < n; i++) {
                            secretKeyLongs[secretKeyLongs.length - i - 1] = Long.parseLong(nums[nums.length - i - 2]);
                        }

                        byte[] secretKeyBytes = toBytesBE(secretKeyLongs, n);
                        symmetricKey = new SecretKeySpec(secretKeyBytes, "AES");

                        System.arraycopy(toBytesBE(padMSB, padLSB), 0, pad, 0, 16);
                        System.arraycopy(toBytesBE(ivMSB, ivLSB), 0, iv, 0, 16);
                    } else {
                        System.out.println("An error occurred: Invalid key length.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("java.lang.NumberFormatException: " + e.getMessage());
                    clearFields();
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("java.lang.ArrayIndexOutOfBoundsException: " + e.getMessage());
                    clearFields();
                }
            }
        }
    }

    public ImageLoader(File f, byte[] keyBytes) {
        try {
            bfImage = ImageIO.read(f);
            bfImage = toIntARGB(bfImage);
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file.");
            bfImage = generatePlaceholder(Color.ORANGE, Color.PINK);
            bfImage = toIntARGB(bfImage);
        } catch (NullPointerException e) {
            System.out.println("Not an image file: " + f.getName());
            bfImage = generatePlaceholder(Color.ORANGE, Color.PINK);
            bfImage = toIntARGB(bfImage);
        }

        width = bfImage.getWidth();
        height = bfImage.getHeight();

        long[] keyBytesInLong = fromBytesBE(keyBytes, 7);
        int kb_len = keyBytesInLong.length;
        keySize = (int) keyBytesInLong[kb_len - 1];

        if (keySize == 128 || keySize == 192 || keySize == 256) {
            int n = keySize / 64;

            long padLSB = keyBytesInLong[kb_len - n - 2];
            long padMSB = keyBytesInLong[kb_len - n - 3];
            long ivLSB = keyBytesInLong[kb_len - n - 4];
            long ivMSB = keyBytesInLong[kb_len - n - 5];

            long[] secretKeyLongs = new long[keySize / 64];
            System.arraycopy(keyBytesInLong, 4, secretKeyLongs, 0, n);

            byte[] secretKeyBytes = toBytesBE(secretKeyLongs, n);
            symmetricKey = new SecretKeySpec(secretKeyBytes, "AES");

            System.arraycopy(toBytesBE(padMSB, padLSB), 0, pad, 0, 16);
            System.arraycopy(toBytesBE(ivMSB, ivLSB), 0, iv, 0, 16);
        } else {
            System.out.println("An error occurred: Invalid key length.");
        }
    }

    public ImageLoader() {
        bfImage = generatePlaceholder(Color.ORANGE, Color.PINK);
        bfImage = toIntARGB(bfImage);
        width = bfImage.getWidth();
        height = bfImage.getHeight();
    }

    public void encryptImageARGB(boolean printInfo) throws
            NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        ImageLogger imgLog = new ImageLogger(printInfo);
        imgLog.println("--- Starting encryption ---");

        keySize = 128;
        symmetricKey = AesCBC.newAesKey(keySize);

        int[] rgbArray = ((DataBufferInt) bfImage.getRaster().getDataBuffer()).getData();
        byte[] argbBytes = toBytesARGB(rgbArray);
        imgLog.println("Image length in bytes: " + argbBytes.length);
        imgLog.print("Image bytes: ");
        imgLog.printArrayView(argbBytes);

        byte[] ciphertext = AesCBC.aesCbcEncryptPkcs5(argbBytes, symmetricKey, true);
        imgLog.println("Encryption successful!");
        imgLog.println("Ciphertext length in bytes: " + ciphertext.length);
        imgLog.print("Ciphertext bytes: ");
        imgLog.printArrayView(ciphertext);

        System.arraycopy(ciphertext, 0, iv, 0, 16);
        imgLog.print("IV bytes: ");
        imgLog.printArrayView(iv);

        System.arraycopy(ciphertext, ciphertext.length - 16, pad, 0, 16);
        imgLog.print("Encrypted padding block bytes: ");
        imgLog.printArrayView(pad);

        int[] encryptedRgbArray = fromBytesARGB(Arrays.copyOfRange(ciphertext, 16, argbBytes.length + 16));
        imgLog.println("Encrypted rgbArray length in bytes: " + encryptedRgbArray.length * 4);
        imgLog.print("Encrypted image contents: ");
        imgLog.printArrayView(encryptedRgbArray);

        bfImage.setRGB(0, 0, width, height, encryptedRgbArray, 0, width);
        imgLog.print("Current image contents (directly after encryption): ");
        imgLog.printArrayView(rgbArray);
    }

    public void decryptImageARGB(boolean printInfo) throws
            NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        ImageLogger imgLog = new ImageLogger(printInfo);
        imgLog.println("--- Starting decryption ---");

        int[] rgbArray = ((DataBufferInt) bfImage.getRaster().getDataBuffer()).getData();
        byte[] argbBytes = toBytesARGB(rgbArray);
        imgLog.println("Current encrypted image length in bytes: " + argbBytes.length);
        imgLog.print("Current encrypted image bytes: ");
        imgLog.printArrayView(argbBytes);

        byte[] plaintext = AesCBC.aesCbcDecryptPkcs5(getFullEncryptedBytes(argbBytes, printInfo), symmetricKey);
        imgLog.println("Decryption successful!");
        imgLog.println("Plaintext length in bytes: " + plaintext.length);
        imgLog.print("Plaintext bytes: ");
        imgLog.printArrayView(plaintext);

        int[] decryptedRgbArray = fromBytesARGB(plaintext);
        imgLog.println("Decrypted rgbArray length in bytes: " + decryptedRgbArray.length * 4);
        imgLog.print("Decrypted image contents: ");
        imgLog.printArrayView(decryptedRgbArray);

        bfImage.setRGB(0, 0, width, height, decryptedRgbArray, 0, width);
        imgLog.print("Current image contents (directly after decryption): ");
        imgLog.printArrayView(rgbArray);
    }

    public void decryptImageWithKeyARGB(boolean printInfo, long iv1, long iv2, long key1, long key2) throws
            NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        ImageLogger imgLog = new ImageLogger(printInfo);
        imgLog.println("--- Starting decryption ---");

        int[] rgbArray = ((DataBufferInt) bfImage.getRaster().getDataBuffer()).getData();
        byte[] argbBytes = toBytesARGB(rgbArray);
        imgLog.println("Current encrypted image length in bytes: " + argbBytes.length);
        imgLog.print("Current encrypted image bytes: ");
        imgLog.printArrayView(argbBytes);

        byte[] plaintext = AesCBC.aesCbcDecryptPkcs5(getFullEncryptedBytes(argbBytes, printInfo), symmetricKey);
        imgLog.println("Decryption successful!");
        imgLog.println("Plaintext length in bytes: " + plaintext.length);
        imgLog.print("Plaintext bytes: ");
        imgLog.printArrayView(plaintext);

        int[] decryptedRgbArray = fromBytesARGB(plaintext);
        imgLog.println("Decrypted rgbArray length in bytes: " + decryptedRgbArray.length * 4);
        imgLog.print("Decrypted image contents: ");
        imgLog.printArrayView(decryptedRgbArray);

        bfImage.setRGB(0, 0, width, height, decryptedRgbArray, 0, width);
        imgLog.print("Current image contents (directly after decryption): ");
        imgLog.printArrayView(rgbArray);
    }

    public void encryptImageRGB(boolean printInfo) throws
            NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        ImageLogger imgLog = new ImageLogger(printInfo);
        imgLog.println("--- Starting encryption ---");

        keySize = 128;
        symmetricKey = AesCBC.newAesKey(keySize);

        int[] rgbArray = ((DataBufferInt) bfImage.getRaster().getDataBuffer()).getData();
        byte[] rgbBytes = toBytesRGB(rgbArray);
        imgLog.println("Image length in bytes: " + rgbBytes.length);
        imgLog.print("Image bytes: ");
        imgLog.printArrayView(rgbBytes);

        byte[] ciphertext = AesCBC.aesCbcEncryptPkcs5(rgbBytes, symmetricKey, true);
        imgLog.println("Encryption successful!");
        imgLog.println("Ciphertext length in bytes: " + ciphertext.length);
        imgLog.print("Ciphertext bytes: ");
        imgLog.printArrayView(ciphertext);

        System.arraycopy(ciphertext, 0, iv, 0, 16);
        imgLog.print("IV bytes: ");
        imgLog.printArrayView(iv);

        System.arraycopy(ciphertext, ciphertext.length - 16, pad, 0, 16);
        imgLog.print("Encrypted padding block bytes: ");
        imgLog.printArrayView(pad);

        int[] encryptedRgbArray = fromBytesRGB(Arrays.copyOfRange(ciphertext, 16, rgbBytes.length + 16), extractAlpha(rgbArray));
        imgLog.println("Encrypted rgbArray length in bytes: " + encryptedRgbArray.length * 4);
        imgLog.print("Encrypted image contents: ");
        imgLog.printArrayView(encryptedRgbArray);

        bfImage.setRGB(0, 0, width, height, encryptedRgbArray, 0, width);
        imgLog.print("Current image contents (directly after encryption): ");
        imgLog.printArrayView(rgbArray);
    }

    public void decryptImageRGB(boolean printInfo) throws
            NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        ImageLogger imgLog = new ImageLogger(printInfo);
        imgLog.println("--- Starting decryption ---");

        int[] rgbArray = ((DataBufferInt) bfImage.getRaster().getDataBuffer()).getData();
        byte[] rgbBytes = toBytesRGB(rgbArray);
        imgLog.println("Current encrypted image length in bytes: " + rgbBytes.length);
        imgLog.print("Current encrypted image bytes: ");
        imgLog.printArrayView(rgbBytes);

        byte[] plaintext = AesCBC.aesCbcDecryptPkcs5(getFullEncryptedBytes(rgbBytes, printInfo), symmetricKey);
        imgLog.println("Decryption successful!");
        imgLog.println("Plaintext length in bytes: " + plaintext.length);
        imgLog.print("Plaintext bytes: ");
        imgLog.printArrayView(plaintext);

        int[] decryptedRgbArray = fromBytesRGB(plaintext, extractAlpha(rgbArray));
        imgLog.println("Decrypted rgbArray length in bytes: " + decryptedRgbArray.length * 4);
        imgLog.print("Decrypted image contents: ");
        imgLog.printArrayView(decryptedRgbArray);

        bfImage.setRGB(0, 0, width, height, decryptedRgbArray, 0, width);
        imgLog.print("Current image contents (directly after decryption): ");
        imgLog.printArrayView(rgbArray);
    }

    private BufferedImage generatePlaceholder(Color d, Color l) {
        int w = 7;
        int h = 3;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        int s = Math.max(1, h / 30);
        for (int y = 0; y < h; y += s) {
            for (int x = 0; x < w; x += s) {
                boolean dark = ((x / s) + (y / s)) % 2 == 0;
                g.setColor(dark ? d : l);
                g.fillRect(x, y, s, s);
            }
        }
        return img;
    }

    private BufferedImage toIntARGB(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
            return src;
        }
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setComposite(AlphaComposite.Src); // preserve raw pixels, no blending
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    private byte[] getFullEncryptedBytes(byte[] rgbBytes, boolean printInfo) {
        ImageLogger imgLog = new ImageLogger(printInfo);
        byte[] fullEncryptedBytes = new byte[rgbBytes.length + 32 - rgbBytes.length % 16];

        int partial = rgbBytes.length % 16;
        int nonpadding = rgbBytes.length + 16;
        System.arraycopy(iv, 0, fullEncryptedBytes, 0, 16);
        System.arraycopy(rgbBytes, 0, fullEncryptedBytes, 16, rgbBytes.length);
        System.arraycopy(pad, partial, fullEncryptedBytes, nonpadding, 16 - partial);

        imgLog.println("Reconstructed ciphertext length in bytes: " + fullEncryptedBytes.length);
        imgLog.print("Reconstructed ciphertext bytes: ");
        imgLog.printArrayView(fullEncryptedBytes);
        return fullEncryptedBytes;
    }

    /** Converts a RGB array into a byte array including the alpha values. */
    public static byte[] toBytesARGB(int[] rgbArray) {
        byte[] argbBytes = new byte[rgbArray.length * 4];
        for (int i = 0; i < rgbArray.length; i++) {
            int curr_int = rgbArray[i];
            int a = (curr_int >>> 24)	& 0xFF;
            int r = (curr_int >>> 16)	& 0xFF;
            int g = (curr_int >>> 8)	& 0xFF;
            int b = (curr_int)			& 0xFF;
            argbBytes[i * 4] =		(byte) a;
            argbBytes[i * 4 + 1] =	(byte) r;
            argbBytes[i * 4 + 2] =	(byte) g;
            argbBytes[i * 4 + 3] =	(byte) b;
        }
        return argbBytes;
    }

    /** Converts a byte array of ARGB values the corresponding RGB array. */
    private int[] fromBytesARGB(byte[] argbBytes) {
        if (argbBytes.length % 4 != 0) {
            throw new IllegalArgumentException("byte array length not divisible by 4");
        }
        int[] rgbArray = new int[argbBytes.length / 4];
        for (int i = 0; i < rgbArray.length; i++) {
            int a = argbBytes[i * 4]		& 0xFF;
            int r = argbBytes[i * 4 + 1]	& 0xFF;
            int g = argbBytes[i * 4 + 2]	& 0xFF;
            int b = argbBytes[i * 4 + 3]	& 0xFF;
            rgbArray[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return rgbArray;
    }

    /** Converts a RGB array into a byte array without including the alpha values. */
    private byte[] toBytesRGB(int[] rgbArray) {
        byte[] rgbBytes = new byte[rgbArray.length * 3];
        for (int i = 0; i < rgbArray.length; i++) {
            int curr_int = rgbArray[i];
            int r = (curr_int >>> 16)	& 0xFF;
            int g = (curr_int >>> 8)	& 0xFF;
            int b = (curr_int)			& 0xFF;
            rgbBytes[i * 3] =		(byte) r;
            rgbBytes[i * 3 + 1] =	(byte) g;
            rgbBytes[i * 3 + 2] =	(byte) b;
        }
        return rgbBytes;
    }

    /** Converts a byte array of RGB values into the corresponding RGB array, given the original alpha source. */
    private int[] fromBytesRGB(byte[] rgbBytes, byte[] alphaSrc) {
        if (rgbBytes.length % 3 != 0) {
            throw new IllegalArgumentException("byte array length not divisible by 3");
        }
        int[] rgbArray = new int[rgbBytes.length / 3];
        for (int i = 0; i < rgbArray.length; i++) {
            int a = alphaSrc[i]			& 0xFF;
            int r = rgbBytes[i * 3]		& 0xFF;
            int g = rgbBytes[i * 3 + 1]	& 0xFF;
            int b = rgbBytes[i * 3 + 2]	& 0xFF;
            rgbArray[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return rgbArray;
    }

    /** Pack n longs into a (n * 8)-byte array, big-endian. */
    private byte[] toBytesBE(long[] longs, int n) {
        if (n > longs.length) {
            throw new IllegalArgumentException("number of longs is larger than the array length");
        } else if (n == 2) {
            return toBytesBE(longs[0], longs[1]);
        } else {
            ByteBuffer buf = ByteBuffer.allocate(n * 8).order(ByteOrder.BIG_ENDIAN);
            for (int i = 0; i < n; i++) {
                buf.putLong(longs[i]);
            }
            return buf.array();
        }
    }

    /** Pack two longs into a 16-byte array: [high(8) | low(8)], big-endian. */
    private byte[] toBytesBE(long high, long low) {
        return ByteBuffer.allocate(16)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(high)
                .putLong(low)
                .array();
    }

    /** Unpack two longs from a 16-byte array: [high(8) | low(8)], big-endian. */
    private long[] fromBytesBE(byte[] src) {
        if (src.length != 16) {
            throw new IllegalArgumentException("expected 16 bytes, got " + src.length);
        }
        ByteBuffer buf = ByteBuffer.wrap(src).order(ByteOrder.BIG_ENDIAN);
        long high = buf.getLong();
        long low = buf.getLong();
        return new long[] {high, low};
    }

    /** Unpack n longs from a (n * 8)-byte array: big-endian. */
    private long[] fromBytesBE(byte[] src, int n) {
        if (src.length != n * 8) {
            throw new IllegalArgumentException("expected " + n * 8 + " bytes, got " + src.length);
        }
        ByteBuffer buf = ByteBuffer.wrap(src).order(ByteOrder.BIG_ENDIAN);
        long[] longArray = new long[n];
        for (int i = 0; i < n; i++) {
            longArray[i] = buf.getLong();
        }
        return longArray;
    }

    private long[] getSecretParameters() {
        if (iv.length % 8 != 0 || pad.length % 8 != 0) {
            throw new IllegalArgumentException("something went wrong...");
        }
        long[] values = new long[4];
        System.arraycopy(fromBytesBE(iv), 0, values, 0, 2);
        System.arraycopy(fromBytesBE(pad), 0, values, 2, 2);
        return values;
    }

    private long[] getSecretKey() {
        byte[] keyBytes = symmetricKey.getEncoded();
        if (keyBytes.length % 8 != 0) {
            throw new IllegalArgumentException("encoded key byte array length not divisible by 8");
        }
        long[] values = new long[keyBytes.length / 8];
        ByteBuffer buf_key = ByteBuffer.wrap(keyBytes).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < values.length; i++) {
            values[i] = buf_key.getLong();
        }
        return values;
    }

    private byte[] extractAlpha(int[] rgbArray) {
        byte[] alphaArray = new byte[rgbArray.length];
        for (int i = 0; i < rgbArray.length; i++) {
            alphaArray[i] = (byte) ((rgbArray[i] >>> 24) & 0xFF);
        }
        return alphaArray;
    }

    private void clearFields() {
        Arrays.fill(iv, (byte)(0));
        Arrays.fill(pad, (byte)(0));
        symmetricKey = null;
    }

    public int getImgWidth() {
        return width;
    }

    public int getImgHeight() {
        return height;
    }

    public int getKeySize() {
        return keySize;
    }

    private long[] getKeyData() {
        long[] secretParameterValues = getSecretParameters();
        long[] secretKeyValues = getSecretKey();

        int spv_length = secretParameterValues.length;
        int skv_length = secretKeyValues.length;

        long[] result = new long[spv_length + skv_length + 1];
        System.arraycopy(secretParameterValues, 0, result, 0, spv_length);
        System.arraycopy(secretKeyValues, 0, result, spv_length, skv_length);
        result[result.length - 1] = getKeySize();
        return result;
    }

    public File getDecryptPNG(String fileName) {
        try {
            // Create a temp file (automatically placed in system temp dir)
            File tempFile = File.createTempFile(fileName + '_', ".png");
            ImageIO.write(bfImage, "png", tempFile);
            return tempFile;
        } catch (IOException e1) {
            throw new RuntimeException("Failed to generate PNG file", e1);
        }
    }
    
    public void saveAsPNG(String fileName) {
		long[] secretParameterValues = getSecretParameters();
		long[] secretKeyValues = getSecretKey();
		
		String first = Arrays.toString(secretParameterValues);
		String second = Arrays.toString(secretKeyValues);
		
		first = first.replace(", ", "_");
		second = second.replace(", ", "_");
		
		fileName += ('_' + first.substring(1, first.length() - 1) +
		'_' + second.substring(1, second.length() - 1) +
		'_' + getKeySize());
		
		Path filePath = Paths.get("data/new/" + fileName + ".png");
		try {
			// Ensure parent directories exist
			Files.createDirectories(filePath.getParent());
			// Write image directly
			ImageIO.write(bfImage, "png", filePath.toFile());
			System.out.println("Image saved to: " + filePath);
		} catch (IOException e1) {
			System.out.println("Error saving image: " + filePath);
			e1.printStackTrace();
		}
	}
    
    /** Set all non-red bytes to 0. */
	public void keepOnly(int channel) {
		if (!(bfImage.getRaster().getDataBuffer() instanceof DataBufferInt)) {
			throw new IllegalArgumentException("Requires TYPE_INT_ARGB or TYPE_INT_RGB");
		}
		int[] rgbArray = ((DataBufferInt) bfImage.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < rgbArray.length; i++) {
			int a = (rgbArray[i] >>> 24)	& 0xFF;
			int r = (rgbArray[i] >>> 16)	& 0xFF;
			int g = (rgbArray[i] >>> 8)		& 0xFF;
			int b = (rgbArray[i])			& 0xFF;
			
			switch (channel) {
			case RED:
			g = 0; b = 0; break;
			case GREEN:
			r = 0; b = 0; break;
			case BLUE:
			r = 0; g = 0; break;
			}
			rgbArray[i] = (a << 24) | (r << 16) | (g << 8) | b;
		}
		bfImage.setRGB(0, 0, width, height, rgbArray, 0, width);
	}

    /** Return the backing array of the current buffered image. */
    public int[] getRgbArray(boolean printInfo) {
        ImageLogger imgLog = new ImageLogger(printInfo);
        int[] rgbArray = ((DataBufferInt) bfImage.getRaster().getDataBuffer()).getData();
        imgLog.println("Image extracted successfully!");
        if (firstDraw) {
            imgLog.print("Original image contents: ");
            firstDraw = false;
        } else {
            imgLog.print("Current image contents: ");
        }
        imgLog.printArrayView(rgbArray);
        return rgbArray;
    }
}
