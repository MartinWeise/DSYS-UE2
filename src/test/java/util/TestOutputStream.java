package util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Simulates writing lines to an {@link PrintStream}.
 * <p/>
 * Internally, the lines written to the underlying {@link PrintStream} are buffered and can be retrieved on demand for
 * verification purposes.
 */
public class TestOutputStream extends PrintStream {
	private final Queue<String> lines = new LinkedBlockingQueue<>();
	private volatile StringBuilder line = new StringBuilder();
	private PrintStream delegate;

	/**
	 * Creates a new {@code TestOutputStream} instance writing to an {@link NullOutputStream}.
	 */
	public TestOutputStream() {
		this(new PrintStream(NullOutputStream.getInstance()));
	}

	/**
	 * Creates a new {@code TestOutputStream} instance writing to the provided {@link PrintStream}.
	 *
	 * @param delegate the stream to write to
	 */
	public TestOutputStream(PrintStream delegate) {
		super(delegate);
		this.delegate = delegate;
	}

	@Override
	public void close() {
		if (delegate != System.out) {
			super.close();
		}
	}

	@Override
	public void write(int b) {
		delegate.write(b);
		if (b == '\r') {
			// Do nothing
		} else if (b == '\n') {
			addLine();
		} else {
			line.append((char) b);
		}
	}

	public void write(byte b[], int off, int len) {
		if (b == null) {
			throw new NullPointerException();
		} else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}
		for (int i = 0 ; i < len ; i++) {
			write(b[off + i]);
		}
	}

	/**
	 * Returns a copy of the lines written to the {@link PrintStream} so far.
	 *
	 * @return the written lines
	 */
	public List<String> getLines() {
		synchronized (lines) {
			if (line.length() > 0) {
				addLine();
			}
			return new ArrayList<>(lines);
		}
	}

	/**
	 * Returns a copy of the lines written to the {@link PrintStream} so far and clears the buffer.
	 *
	 * @return the written lines
	 * @see #getLines()
	 * @see #clear()
	 */
	public List<String> reset() {
		synchronized (lines) {
			List<String> lines = getLines();
			clear();
			return lines;
		}
	}

	/**
	 * Clears the buffer holding the lines written to the {@link PrintStream} so far.
	 */
	private void clear() {
		synchronized (lines) {
			lines.clear();
			line = new StringBuilder();
		}
	}

	/**
	 * Appends the current line to the buffer.
	 */
	private void addLine() {
		synchronized (lines) {
			lines.add(line.toString());
			line = new StringBuilder();
		}
	}
}
