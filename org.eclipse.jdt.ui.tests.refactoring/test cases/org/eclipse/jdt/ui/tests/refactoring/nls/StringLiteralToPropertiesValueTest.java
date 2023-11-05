package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.jdt.internal.corext.refactoring.nls.StringLiteralToPropertiesValue;

public class StringLiteralToPropertiesValueTest {

	@Test
	public void emptyString() throws Exception {
		String literal = "\"\"";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("", propertyValue);
	}

	@Test
	public void escapes() throws Exception {
		String literal = "\"\\\"\\r\\n\\\\\"";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("\\\"\\r\\n\\\\", propertyValue);
	}

	@Test
	public void emptyBlock() throws Exception {
		String literal = """
				\"""
				\"""\
				""";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("", propertyValue);
	}

	@Test
	public void blockText() throws Exception {
		// contains indentation and line breaks
		String literal = """
				\"""
				    Some Text
				    with 2 lines.
				    \"""\
				""";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("Some Text\nwith 2 lines.\n", propertyValue);
	}

	@Test
	public void blockWithWhitespaceOnStart() throws Exception {
		String literal = """
				\"""\s
				    Some Text
				    \"""\
				""";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("Some Text\n", propertyValue);
	}

	@Test
	public void blockWithTrailingWhitespaceRemoved() throws Exception {
		String literal = """
				\"""
				    Some Text \s
				    \"""\
				""";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("Some Text\n", propertyValue);
	}

	@Test
	public void blockWithTrailingWhitespaceKept() throws Exception {
		String literal = """
				\"""
				    Some Text \\s
				    \"""\
				""";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("Some Text  \n", propertyValue);
	}

	@Test
	public void blockWithLinefeed() throws Exception {
		String literal = """
				\"""
				    Some Text\\r
				    \"""\
				""";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("Some Text\r\n", propertyValue);
	}

	@Test
	public void blockWithTab() throws Exception {
		String literal = """
				\"""
				    Some\\tText
				    \"""\
				""";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("Some\tText\n", propertyValue);
	}

	@Test
	public void blockWithEscapes() throws Exception {
		String literal = """
				\"""
				    Some\\nText
				    \"""\
				""";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("Some\\nText\n", propertyValue);
	}

	@Test
	public void blockWithUnicodeEscapes() throws Exception {
		String literal = """
				\"""
				    \\u00e4
				    \"""\
				""";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("\\u00e4\n", propertyValue);
	}

	@Test
	public void blockWithJoinedLines() throws Exception {
		String literal = """
				\"""
				    Some Text \\
				    goes on.
				    \"""\
				""";
		String propertyValue = StringLiteralToPropertiesValue.stringLiteralToPropertiesValue(literal);
		assertEquals("Some Text goes on.\n", propertyValue);
	}

}
