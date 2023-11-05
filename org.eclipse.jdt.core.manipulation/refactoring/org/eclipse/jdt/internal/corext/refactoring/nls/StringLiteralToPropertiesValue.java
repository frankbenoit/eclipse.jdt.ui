package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.jdt.core.compiler.CharOperation;

public class StringLiteralToPropertiesValue {
	public static String stringLiteralToPropertiesValue( String literal ) {
		char[] escaped = literal.toCharArray();
		int len = escaped.length;

		if( !literal.startsWith("\"\"\"".translateEscapes())) { //$NON-NLS-1$
			return literal.substring(1, literal.length() - 1);
		}

		int start= findTextBlockStartIndex(escaped, len);
		String withoutQuotes = new String(
				CharOperation.subarray(escaped, start, len - 3)
				);
		return escapes( withoutQuotes.stripIndent());

	}
	private static String escapes(String input) {
		var withoutTrailing = input.replaceAll(" +\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		StringBuilder sb = new StringBuilder();
		var chars = withoutTrailing.toCharArray();
		int pos = 0;
		while( pos < chars.length ) {
			char c = chars[pos++];
			if( c == '\\') {
				c = pos < chars.length ? chars[pos++] : '\0';
				switch( c ) {
					case '\'':
						sb.append(c);
						break;
					case 's':
						sb.append(' ');
						break;
					case '\n':
						// backslash at end of line, nothing to insert, the lines will be joined
						break;
					case 'r':
						sb.append('\r');
						break;
					case 't':
						sb.append('\t');
						break;
					case 'n':
					case '\\':
					case '"':
					default:
						sb.append('\\');
						sb.append(c);
						break;
				}
			}
			else {
				switch( c ) {
					case '"':
					case '=':
						sb.append('\\');
						sb.append(c);
						break;
					default:
						if( c > 0x7F ) {
							sb.append("\\u%04x".formatted(Integer.valueOf(c))); //$NON-NLS-1$
						}
						else {
							sb.append(c);
						}
						break;
				}
			}
		}
		return sb.toString();
	}

	private static int findTextBlockStartIndex(char[] escaped, int len) {
		int start = -1;
		loop: for (int i = 3; i < len; i++) {
			char c = escaped[i];
			if (Character.isWhitespace(c)) {
				switch (c) {
					case 10 : /* \ u000a: LINE FEED               */
					case 13 : /* \ u000d: CARRIAGE RETURN         */
						start =  i + 1;
						break loop;
					default:
						break;
				}
			} else {
				break loop;
			}
		}
		if (start == -1) {
			throw new IllegalArgumentException();
		}
		return start;
	}

}