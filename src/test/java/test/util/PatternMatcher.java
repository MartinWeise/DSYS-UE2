package test.util;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.regex.Pattern;

public class PatternMatcher extends BaseMatcher<String> {
	Pattern pattern;

	public PatternMatcher(String regex) {
		this.pattern = Pattern.compile(regex, Pattern.DOTALL);
	}

	@Override
	public boolean matches(Object item) {
		return pattern.matcher(String.valueOf(item != null ? item : "")).matches();
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("matches pattern " + pattern);
	}
}
