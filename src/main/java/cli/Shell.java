package cli;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads commands from an {@link InputStream}, executes them and writes the
 * result to a {@link OutputStream}.
 */
public class Shell implements Runnable, Closeable {
	private static final PrintStream stdout = System.out;
	private static final InputStream stdin = System.in;
	private static final char[] EMPTY = new char[0];

	private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("HH:mm:ss.SSS");
		}
	};

	private String name;

	private ShellCommandInvocationHandler invocationHandler = new ShellCommandInvocationHandler();
	private Map<String, ShellCommandDefinition> commandMap = new ConcurrentHashMap<>();
	private ConversionService conversionService = new DefaultConversionService();

	private OutputStream out;
	private BufferedReader in;
	private Closeable readMonitor;

	/**
	 * Creates a new {@code Shell} instance.
	 *
	 * @param name
	 *            the name of the {@code Shell} displayed in the prompt
	 * @param in
	 *            the {@code InputStream} to read messages from
	 * @param out
	 *            the {@code OutputStream} to write messages to
	 */
	public Shell(String name, InputStream in, OutputStream out) {
		this.name = name;
		this.out = out;
		this.readMonitor = in;
		this.in = new BufferedReader(new InputStreamReader(in));
	}

	/**
	 * Executes commands read from the provided {@link InputStream} and prints
	 * the output.
	 * <p/>
	 * Note that this method blocks until either
	 * <ul>
	 * <li>This {@code Shell} is closed,</li>
	 * <li>the end of the {@link InputStream} is reached,</li>
	 * <li>or an {@link IOException} is thrown while reading from or writing to
	 * the streams.</li>
	 * </ul>
	 */
	@Override
	public void run() {
		try {
			for (String line; !Thread.currentThread().isInterrupted()
					&& (line = readLine()) != null;) {
				write(String.format("%s\t\t%s> %s%n",
						DATE_FORMAT.get().format(new Date()), name, line)
						.getBytes());
				Object result;
				try {
					result = invoke(line);
				} catch (Throwable throwable) {
					ByteArrayOutputStream str = new ByteArrayOutputStream(1024);
					throwable.printStackTrace(new PrintStream(str, true));
					result = str.toString();
				}
				if (result != null) {
					print(result);
				}
			}
		} catch (IOException e) {
			try {
				writeLine("Shell closed");
			} catch (IOException ex) {
				System.out.println(ex.getClass().getName() + ": "
						+ ex.getMessage());
			}
		}
	}

	private void print(Object result) throws IOException {
		if (result instanceof Iterable) {
			for (Object e : ((Iterable<?>) result)) {
				print(e);
			}
		} else if (result instanceof Map) {
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) result).entrySet()) {
				writeLine(entry.getKey() + "\t" + entry.getValue());
			}
		} else {
			writeLine(String.valueOf(result));
		}
	}

	/**
	 * Writes the given line to the provided {@link OutputStream}.<br/>
	 *
	 * @param line
	 *            the line to write
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void writeLine(String line) throws IOException {
		String now = DATE_FORMAT.get().format(new Date());
		if (line.indexOf('\n') >= 0 && line.indexOf('\n') < line.length() - 1) {
			write((String.format("%s\t\t%s:\n", now, name)).getBytes());
			for (String l : line.split("[\\r\\n]+")) {
				write((String.format("%s\t\t%s\n", now, l)).getBytes());
			}
		} else {
			write((String.format("%s\t\t%s: %s%s", now, name, line,
					line.endsWith("\n") ? "" : "\n")).getBytes());
		}
	}

	/**
	 * Writes {@code b.length} bytes from the specified byte array to the
	 * provided {@link OutputStream}.
	 *
	 * @param bytes
	 *            the data
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public void write(byte[] bytes) throws IOException {
		out.write(bytes);
	}

	/**
	 * Reads a line of text.<br/>
	 * A line is considered to be terminated by any one of a line feed (
	 * {@code '\n'}), a carriage return ({@code '\r'}), or a carriage return
	 * followed immediately by a linefeed.
	 *
	 * @return A String containing the contents of the line, not including any
	 *         line-termination characters, or {@code null} if the end of the
	 *         stream has been reached
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public String readLine() throws IOException {
		synchronized (readMonitor) {
			return in.readLine();
		}
	}

	/**
	 * Reads characters into a portion of an array.<br/>
	 * This method implements the general contract of the corresponding read
	 * method of the {@link Reader} class.<br/>
	 * If no data can be read i.e., the end of the stream is reached, an empty
	 * buffer is returned.
	 * <p/>
	 * If {@code len} is less than {@code 0}, the default buffer size (
	 * {@code 4096}) is used.
	 *
	 * @param len
	 *            maximum number of characters to read
	 * @return the destination buffer containing the bytes read
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public char[] read(int len) throws IOException {
		synchronized (readMonitor) {
			len = len < 0 ? 4096 : len;
			char[] cbuf = new char[len];
			int read = in.read(cbuf, 0, len);
			return read <= 0 ? EMPTY : Arrays.copyOfRange(cbuf, 0, read);
		}
	}

	/**
	 * Reads characters into a portion of an array.<br/>
	 * This method is a convenience method of {@link #read(int)} using the
	 * default buffer size.
	 *
	 * @return the destination buffer containing the bytes read
	 * @throws IOException
	 *             if an I/O error occurs
	 * @see #read(int)
	 */
	public char[] read() throws IOException {
		return read(-1);
	}

	/**
	 * Closes this {@link Shell} by closing the provided streams.<br/>
	 * Note that {@link System#in} and {@link System#out} are not closed. They
	 * have to be closed manually since closing them may affect other objects.
	 */
	@Override
	public void close() {
		Thread.currentThread().interrupt();
		if (readMonitor != stdin) {
			try {
				readMonitor.close();
			} catch (IOException e) {
				System.err.printf("Cannot close console input. %s: %s%n",
						getClass(), e.getMessage());
			}
		}
		if (out != stdout) {
			try {
				out.close();
			} catch (IOException e) {
				System.err.printf("Cannot close console output. %s: %s%n",
						getClass(), e.getMessage());
			}
		}
	}

	/**
	 * Registers all commands provided by the given object.<br/>
	 * An accessible method is considered to be a command if it is annotated
	 * with {@link Command}.
	 * <p/>
	 * If a command with the same name is already registered, an
	 * {@link IllegalArgumentException} is thrown.
	 *
	 * @param obj
	 *            the object implementing commands to be registered
	 * @see cli.Shell.ShellCommandDefinition
	 */
	public void register(Object obj) {
		for (Method method : obj.getClass().getMethods()) {
			Command command = method.getAnnotation(Command.class);
			if (command != null) {
				String name = command.value().isEmpty() ? method.getName()
						: command.value();
				name = name.startsWith("!") ? name : "!" + name;
				if (commandMap.containsKey(name)) {
					throw new IllegalArgumentException(String.format(
							"Command '%s' is already registered.", name));
				}
				method.setAccessible(true);
				commandMap.put(name, new ShellCommandDefinition(obj, method));
			}
		}
	}

	/**
	 * Parses the given command string, extracts the arguments and invokes the
	 * command matching the input.
	 *
	 * @param cmd
	 *            the command string
	 * @return the result of the executed command
	 * @throws Throwable
	 *             any exception that might occur during invocation
	 */
	public Object invoke(String cmd) throws Throwable {
		if (cmd == null || (cmd = cmd.trim()).isEmpty()) {
			return null;
		}

		int pos = cmd.indexOf(' ');
		String cmdName = pos >= 0 ? cmd.substring(0, pos) : cmd;
		ShellCommandDefinition cmdDef = commandMap.get(cmdName);
		if (cmdDef == null) {
			throw new IllegalArgumentException(String.format(
					"Command '%s' not registered.", cmdName));
		}

		String[] parts = cmd.split("\\s+",
				cmdDef.targetMethod.getParameterTypes().length + 1);
		Object[] args = new Object[parts.length - 1];
		for (int i = 1; i < parts.length; i++) {
			args[i - 1] = conversionService.convert(parts[i],
					cmdDef.targetMethod.getParameterTypes()[i - 1]);
		}
		return invocationHandler.invoke(cmdDef.targetObject,
				cmdDef.targetMethod, args);
	}

	/**
	 * Returns the underlying {@link BufferedReader} used for reading commands.
	 * <p/>
	 * <b>Note that this method provided direct access to the {@code Shell}
	 * internals.<br/>
	 * Invokers of this method have to ensure synchronization on their own.</b>
	 *
	 * @return the input
	 */
	public BufferedReader getIn() {
		return in;
	}

	/**
	 * Returns the underlying {@link OutputStream} used for writing to messages.
	 * <p/>
	 * <b>Note that this method provided direct access to the {@code Shell}
	 * internals.<br/>
	 * Invokers of this method have to ensure synchronization on their own.</b>
	 *
	 * @return the output
	 */
	public OutputStream getOut() {
		return out;
	}

	/**
	 * Defines a {@link Method} to be invoked on a certain object.
	 */
	static class ShellCommandDefinition {
		protected Object targetObject;
		protected Method targetMethod;

		ShellCommandDefinition(Object targetObject, Method targetMethod) {
			this.targetObject = targetObject;
			this.targetMethod = targetMethod;
		}
	}

	/**
	 * Invokes the given method represented on the specified object with the
	 * specified parameters.
	 * <p/>
	 * If the method is static, then the specified obj argument is ignored. It
	 * may be {@code null}.<br/>
	 * If the number of formal parameters required by the underlying method is
	 * {@code 0}, the supplied {@code args} array may be of length {@code 0} or
	 * {@code null}.
	 * <p/>
	 * If the method is an instance method, it is invoked using dynamic method
	 * lookup as documented in <i>The Java Language Specification, Second
	 * Edition</i>, section 15.12.4.4; in particular, overriding based on the
	 * runtime type of the target object will occur.
	 * <p/>
	 * If the method completes normally, the value it returns is returned to the
	 * caller. If the method return type is {@code void}, the invocation returns
	 * {@code null}.
	 *
	 * @see Method#invoke(Object, Object...)
	 */
	static class ShellCommandInvocationHandler implements InvocationHandler {
		@Override
		public Object invoke(Object target, Method method, Object... args)
				throws Throwable {
			return method.invoke(target, args);
		}
	}
}
