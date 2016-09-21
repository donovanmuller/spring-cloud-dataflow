/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.core.dsl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.dataflow.core.DefinitionType;
import org.springframework.util.StringUtils;

/**
 * Parser for application group DSL that generates {@link ApplicationGroupNode}.
 *
 */
public class ApplicationGroupParser extends AppParser {

	/**
	 * Application group name (may be {@code null}).
	 */
	private final String name;

	/**
	 * Application group DSL text.
	 */
	private final String dsl;

	/**
	 * Construct a {@code ApplicationGroupParser} without supplying the application group name up front.
	 * The application group name may be embedded in the definition; for example:
	 * {@code myApplicationGroup = http & file}.
	 *
	 * @param dsl the application group definition DSL text
	 */
	public ApplicationGroupParser(String dsl) {
		this(null, dsl);
	}

	/**
	 * Construct a {@code {@link ApplicationGroupParser }} for a application group with the provided name.
	 *
	 * @param name application group name
	 * @param dsl  application group dsl text
	 */
	public ApplicationGroupParser(String name, String dsl) {
		super(new Tokens(dsl));
		this.name = name;
		this.dsl = dsl;
	}

	/**
	 * Parse a application group definition.
	 *
	 * @return the AST for the parsed application group
	 * @throws ParseException
	 */
	public ApplicationGroupNode parse() {
		ApplicationGroupNode ast = eatApplicationGroup();

		// Check the application group name, however it was specified
		if (ast.getName() != null && !isValidName(ast.getName())) {
			throw new ParseException(ast.getName(), 0, DSLMessage.ILLEGAL_APPLICATION_GROUP_NAME, ast.getName());
		}
		if (name != null && !isValidName(name)) {
			throw new ParseException(name, 0, DSLMessage.ILLEGAL_APPLICATION_GROUP_NAME, name);
		}

		for (int m = 0; m < ast.getAppNodes().size(); m++) {
			AppNode node = ast.getAppNodes().get(m);
			// check that the node "label" has a valid DefinitionType defined
			try {
				DefinitionType.valueOf(node.getLabelName());
			} catch (IllegalArgumentException e) {
				throw new ParseException(dsl, node.startPos, DSLMessage.MISSING_DEFINITION_TYPE,
						node.getName(), m, StringUtils.arrayToDelimitedString(DefinitionType.values(), ", "));
			}
		}
		Tokens tokens = getTokens();
		if (tokens.hasNext()) {
			tokens.raiseException(tokens.peek().startPos, DSLMessage.MORE_INPUT,
					toString(tokens.next()));
		}

		return ast;
	}

	/**
	 * If a application group name is present, return it and advance the token position -
	 * otherwise return {@code null}.
	 * <p>
	 * Expected format:
	 * {@code name =}
	 *
	 * @return application group name if present
	 */
	private String eatApplicationGroupName() {
		Tokens tokens = getTokens();
		String applicationGroupName = null;
		if (tokens.lookAhead(1, TokenKind.EQUALS)) {
			if (tokens.peek(TokenKind.IDENTIFIER)) {
				applicationGroupName = tokens.eat(TokenKind.IDENTIFIER).data;
				tokens.next(); // skip '='
			}
			else {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.ILLEGAL_APPLICATION_GROUP_NAME,
						toString(tokens.peek()));
			}
		}
		return applicationGroupName;
	}

	/**
	 * Return a {@link ApplicationGroupNode} based on the tokens resulting from the parsed DSL.
	 * <p>
	 * Expected format:
	 * {@code applicationGroup: (applicationGroupName) appList}
	 *
	 * @return {@code {@link ApplicationGroupNode }} based on parsed DSL
	 */
	private ApplicationGroupNode eatApplicationGroup() {
		String applicationGroupName = eatApplicationGroupName();

		Tokens tokens = getTokens();
		List<AppNode> appNodes = eatAppList();

		// Further data is an error
		if (tokens.hasNext()) {
			Token t = tokens.peek();
			DSLMessage errorMessage = DSLMessage.UNEXPECTED_DATA_AFTER_STREAMDEF;
			if (!appNodes.isEmpty() &&
					tokens.getTokenStream().get(tokens.position() - 1).isKind(TokenKind.GT)) {
				// Additional token where a destination is expected, but has no prefix
				errorMessage = DSLMessage.EXPECTED_DESTINATION_PREFIX;
			}
			tokens.raiseException(t.startPos, errorMessage, toString(t));
		}
		return new ApplicationGroupNode(tokens.getExpression(), applicationGroupName, appNodes);
	}

	/**
	 * Return a list of {@link AppNode} starting from the current token position.
	 * <p>
	 * Expected format:
	 * {@code appList: app (| app)*}
	 *
	 * @return a list of {@code AppNode}
	 */
	private List<AppNode> eatAppList() {
		Tokens tokens = getTokens();
		List<AppNode> appNodes = new ArrayList<AppNode>();

		appNodes.add(eatApp());
		while (tokens.hasNext()) {
			Token t = tokens.peek();
			if (t.kind == TokenKind.AND) {
				tokens.next();
				appNodes.add(eatApp());
			}
		}
		return appNodes;
	}

	@Override
	public String toString() {
		Tokens tokens = getTokens();
		return String.valueOf(tokens.getTokenStream()) + "\n" +
				"tokenApplicationGroupPointer=" + tokens.position() + "\n";
	}

}
