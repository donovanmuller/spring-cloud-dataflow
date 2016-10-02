/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.core.dsl;

/**
 * Parser for standalone DSL that generates {@link AppNode}.
 *
 * @author Donovan Muller
 */
public class StandaloneParser extends AppParser {

	/**
	 * Standalone application name (may be {@code null}).
	 */
	private final String name;

	/**
	 * Construct a {@code StandaloneParser} without supplying the standalone application name up front.
	 * The standalone application name may be embedded in the definition; for example:
	 * {@code myApp = app}
	 *
	 * @param dsl the standalone definition DSL text
	 */
	public StandaloneParser(String dsl) {
		this(null, dsl);
	}

	/**
	 * Construct a {@code StandaloneParser} for a standalone application with the provided name.
	 *
	 * @param name standalone application name
	 * @param dsl  standalone application dsl text
	 */
	public StandaloneParser(String name, String dsl) {
		super(new Tokens(dsl));
		this.name = name;
	}

	/**
	 * Parse a standalone application definition.
	 *
	 * @return the AST for the parsed standalone application
	 * @throws ParseException
	 */
	public AppNode parse() {
		AppNode ast = eatApp();
		if (ast.getName() != null && !isValidName(ast.getName())) {
			throw new ParseException(ast.getName(), 0, DSLMessage.ILLEGAL_STANDALONE_NAME, ast.getName());
		}
		if (name != null && !isValidName(name)) {
			throw new ParseException(name, 0, DSLMessage.ILLEGAL_STANDALONE_NAME, name);
		}
		Tokens tokens = getTokens();
		if (tokens.hasNext()) {
			tokens.raiseException(tokens.peek().startPos, DSLMessage.MORE_INPUT, toString(tokens.next()));
		}

		return ast;
	}

}
