package util;

import java.util.Arrays;
import java.util.List;

/**
 * Provides utility methods for testing the solution.
 */
public final class TestUtils {

	private TestUtils() {
	}

	/**
	 * Returns a string consisting of a specific number of concatenated copies of an input character.<br/>
	 * For example, {@code repeat('x', 3)} returns the string {@code "xxx"}.
	 *
	 * @param character the character to repeat
	 * @param count     the number of times to repeat it; a nonnegative integer
	 * @return a string containing {@code character} repeated {@code count} times
	 * (the empty string if {@code count} is zero)
	 * @throws IllegalArgumentException if {@code count} is negative
	 */
	public static char[] repeat(char character, int count) {
		if (count < 0) {
			throw new IllegalArgumentException("'count' must not be negative");
		}
		char[] bytes = new char[count];
		Arrays.fill(bytes, character);
		return bytes;
	}

	/**
	 * Returns {@code true} if the given array contains the specified element.
	 * More formally, returns {@code true} if and only if this collection contains at least one element <tt>e</tt> such
	 * that <tt>(objectToFind.equals(e))</tt>.
	 *
	 * @param objectToFind element whose presence in the array is to be tested
	 * @return {@code true} if the array contains the specified element, {@code false} otherwise
	 * @throws NullPointerException if the specified element is null
	 */
	public static boolean contains(Object objectToFind, Object... array) {
		if (array != null && array.getClass().getComponentType().isInstance(objectToFind)) {
			for (int i = 0; i < array.length; i++) {
				if (objectToFind.equals(array[i])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns a new String composed of copies of the {@code strings} joined together with a copy
	 * of the specified {@code separator}.
	 * <p/>
	 * Note that if an element is {@code null}, then {@code "null"} is added.
	 *
	 * @param separator the delimiter that separates each element
	 * @param strings   the elements to join together.
	 * @return a new {@code String} that is composed of the {@code strings} separated by the {@code separator}
	 */
	public static String join(String separator, List<String> strings) {
		StringBuilder appendable = new StringBuilder();
		if (strings != null && strings.size() > 0) {
			separator = separator != null ? separator : "";
			appendable.append(strings.get(0));
			for (int i = 1; i < strings.size(); i++) {
				appendable.append(separator).append(strings.get(i));
			}
		}
		return appendable.toString();
	}
}
