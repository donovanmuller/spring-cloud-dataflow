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

package org.springframework.cloud.dataflow.shell.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.StandaloneOperations;
import org.springframework.cloud.dataflow.rest.resource.StandaloneDefinitionResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.stereotype.Component;

/**
 * Standalone application commands.
 *
 * @author Donovan Muller
 */
@Component
public class StandaloneCommands implements CommandMarker {

	private static final String LIST_STANDALONE = "standalone list";

	private static final String CREATE_STANDALONE = "standalone create";

	private static final String DEPLOY_STANDALONE = "standalone deploy";

	private static final String UNDEPLOY_STANDALONE = "standalone undeploy";

	private static final String UNDEPLOY_STANDALONE_ALL = "standalone all undeploy";

	private static final String DESTROY_STANDALONE = "standalone destroy";

	private static final String DESTROY_STANDALONE_ALL = "standalone all destroy";

	private static final String PROPERTIES_OPTION = "properties";

	private static final String PROPERTIES_FILE_OPTION = "propertiesFile";

	@Autowired
	private DataFlowShell dataFlowShell;

	@Autowired
	private UserInput userInput;

	@CliAvailabilityIndicator({LIST_STANDALONE, CREATE_STANDALONE, DEPLOY_STANDALONE,
			UNDEPLOY_STANDALONE, UNDEPLOY_STANDALONE_ALL, DESTROY_STANDALONE,
			DESTROY_STANDALONE_ALL})
	public boolean available() {
		DataFlowOperations dataFlowOperations = dataFlowShell.getDataFlowOperations();
		return dataFlowOperations != null && (dataFlowOperations.standaloneOperations() != null
				|| dataFlowOperations.applicationGroupOperations() != null);
	}

	@CliCommand(value = LIST_STANDALONE, help = "List standalone applications")
	public Table listStandalone() {
		final PagedResources<StandaloneDefinitionResource> standaloneDefinitions = standaloneOperations().list();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Standalone Name");
		headers.put("dslText", "Standalone Definition");
		headers.put("status", "Status");
		BeanListTableModel<StandaloneDefinitionResource> model = new BeanListTableModel<>(standaloneDefinitions, headers);
		return DataFlowTables.applyStyle(new TableBuilder(model))
				.build();
	}

	@CliCommand(value = CREATE_STANDALONE, help = "Create a new standalone application definition")
	public String createStandalone(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the standalone application") String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "a standalone application definition, using the DSL (e.g. \"myApp --port=9000 \")", optionContext = "disable-string-converter completion-standalone") String dsl,
			@CliOption(key = "force",
					help = "force update if standalone application definition already exists",
					specifiedDefaultValue = "true",
					unspecifiedDefaultValue = "false")
					boolean force,
			@CliOption(key = "deploy", help = "whether to deploy the standalone application immediately", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean deploy) {
		standaloneOperations().createStandalone(name, dsl, force, deploy);
		return (deploy) ? String.format("Created and deployed new standalone application '%s'", name) : String.format(
				"Created new standalone application definition '%s'", name);
	}

	@CliCommand(value = DEPLOY_STANDALONE, help = "Deploy a standalone application")
	public String deployStandalone(
			@CliOption(key = { "", "name" }, help = "the name of the standalone application", mandatory = true) String name,
			@CliOption(key = { PROPERTIES_OPTION }, help = "the properties for this deployment", mandatory = false) String properties,
			@CliOption(key = { PROPERTIES_FILE_OPTION }, help = "the properties for this deployment (as a File)", mandatory = false) File propertiesFile,
			@CliOption(key = "force", help = "force redeploy if standalone application is already deployed", specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") boolean force) throws IOException {
		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, properties, PROPERTIES_FILE_OPTION, propertiesFile);
		Map<String, String> propertiesToUse;
		switch (which) {
			case 0:
				propertiesToUse = DeploymentPropertiesUtils.parse(properties);
				break;
			case 1:
				Properties props = new Properties();
				try (FileInputStream fis = new FileInputStream(propertiesFile)) {
					props.load(fis);
				}
				propertiesToUse = DeploymentPropertiesUtils.convert(props);
				break;
			case -1: // Neither option specified
				propertiesToUse = Collections.emptyMap();
				break;
			default:
				throw new AssertionError();
		}
		standaloneOperations().deploy(name, propertiesToUse, force);
		return String.format("Deployed standalone application '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_STANDALONE, help = "Undeploy a previously deployed standalone application")
	public String undeployStandalone(
			@CliOption(key = { "", "name" }, help = "the name of the standalone application to Undeploy", mandatory = true) String name
			) {
		standaloneOperations().undeploy(name);
		return String.format("Undeployed standalone application '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_STANDALONE_ALL, help = "Undeploy all previously deployed standalone applications")
	public String undeployAllStandalone(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force
			) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really undeploy all standalone applications?", "n", "y", "n"))) {
			standaloneOperations().undeployAll();
			return String.format("Undeployed all the standalone applications");
		}
		else {
			return "";
		}
	}

	@CliCommand(value = DESTROY_STANDALONE, help = "Destroy an existing standalone application")
	public String destroyStandalone(
			@CliOption(key = { "", "name" }, help = "the name of the standalone application to destroy", mandatory = true) String name) {
		standaloneOperations().destroy(name);
		return String.format("Destroyed standalone application '%s'", name);
	}

	@CliCommand(value = DESTROY_STANDALONE_ALL, help = "Destroy all existing standalone applications")
	public String destroyAllStandalone(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really destroy all standalone applications?", "n", "y", "n"))) {
			standaloneOperations().destroyAll();
			return "Destroyed all standalone applications";
		}
		else {
			return "";
		}
	}

	StandaloneOperations standaloneOperations() {
		return dataFlowShell.getDataFlowOperations().standaloneOperations();
	}
}
