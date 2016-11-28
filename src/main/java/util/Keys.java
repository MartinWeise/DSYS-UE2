package util;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map; 

/**
 * Please note that this class is not needed for Lab 1, but can later be
 * used in Lab 2.
 * 
 * Reads encryption keys from the file system.
 */
public final class Keys {

	static {
		StaticPasswordReader.setPassword("alice.vienna.at.pem", "12345");
		StaticPasswordReader.setPassword("bill.de.pem", "23456");
		StaticPasswordReader.setPassword("chatserver.pem", "12345");
	}

	private Keys() {
	}

	/**
	 * Reads the {@link PrivateKey} from the given location.
	 *
	 * @param file
	 *            the path to key located in the file system
	 * @return the private key
	 * @throws IOException
	 *             if an I/O error occurs or the security provider cannot handle
	 *             the file
	 */
	public static PrivateKey readPrivatePEM(File file) throws IOException {
		/*
		 * You can switch to the PasswordReader to read the passwords from the
		 * command line.
		 */
		// PEMReader in = new PEMReader(new FileReader(file), new
		// PasswordReader(file.getName()));

		PEMReader in = new PEMReader(new FileReader(file),
				new StaticPasswordReader(file.getName()));

		try {
			KeyPair keyPair = (KeyPair) in.readObject();
			return keyPair.getPrivate();
		} catch (ClassCastException ex) {
			throw new IOException("Could not read private key " + file, ex);
		} finally {
			in.close();
		}
	}

	/**
	 * Reads the {@link PublicKey} from the given location.
	 *
	 * @param file
	 *            the path to key located in the file system
	 * @return the public key
	 * @throws IOException
	 *             if an I/O error occurs or the security provider cannot handle
	 *             the file
	 */
	public static PublicKey readPublicPEM(File file) throws IOException {
		PEMReader in = new PEMReader(new FileReader(file));
		try {
			return (PublicKey) in.readObject();
		} finally {
			in.close();
		}
	}

	/**
	 * Reads the {@link SecretKeySpec} from the given location.
	 *
	 * @param file
	 *            the path to key located in the file system
	 * @return the secret key
	 * @throws IOException
	 *             if an I/O error occurs or the security provider cannot handle
	 *             the file
	 */
	public static Key readSecretKey(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		try {
			byte[] keyBytes = new byte[1024];
			if (fis.read(keyBytes) < 0) {
				throw new IOException(String.format("Cannot read key file %s.",
						file.getCanonicalPath()));
			}
			byte[] input = Hex.decode(keyBytes);
			return new SecretKeySpec(input, "HmacSHA256");
		} finally {
			fis.close();
		}
	}

	/**
	 * Reads the password from the standard input.
	 */
	public static class PasswordReader implements PasswordFinder {

		protected String keyName;

		public PasswordReader(String keyName) {
			this.keyName = keyName;
		}

		@Override
		public char[] getPassword() {
			System.out.printf("Enter pass phrase for %s:", this.keyName);
			try {
				return new BufferedReader(new InputStreamReader(System.in))
						.readLine().toCharArray();
			} catch (IOException ex) {
				throw new RuntimeException("Unable to read pass: "
						+ ex.getMessage());
			}
		}
	}

	/**
	 * Holds a table of passwords for key files.
	 * <p/>
	 * <b>Note that this class can be used alternatively to
	 * {@link PasswordReader} especially for test automation.</b>
	 */
	public static class StaticPasswordReader implements PasswordFinder {

		private static final Map<String, String> passwordMap = new HashMap<String, String>();
		protected String keyName;

		public StaticPasswordReader(String keyName) {
			this.keyName = keyName;
		}

		@Override
		public char[] getPassword() {
			String password = passwordMap.get(keyName);
			if (password == null) {
				throw new RuntimeException("No password for key file "
						+ keyName);
			}
			return password.toCharArray();
		}

		/**
		 * Sets the password to use for decrypting the given key file.
		 *
		 * @param keyName
		 *            the name of the file to use the password for
		 * @param password
		 *            the password for the key file
		 */
		public static void setPassword(String keyName, String password) {
			passwordMap.put(keyName, password);
		}
	}
}
