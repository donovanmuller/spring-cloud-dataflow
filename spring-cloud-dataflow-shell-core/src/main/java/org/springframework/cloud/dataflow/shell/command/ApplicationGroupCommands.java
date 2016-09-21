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
import org.springframework.cloud.dataflow.rest.client.ApplicationGroupOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.ApplicationGroupDefinitionResource;
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
 * Application group commands.
 *
 */
@Component
public class ApplicationGroupCommands implements CommandMarker {

	private static final String LIST_APPLICATION_GROUP = "application-group list";

	private static final String IMPORT_APPLICATION_GROUP = "application-group import";

	private static final String CREATE_APPLICATION_GROUP = "application-group create";

	private static final String DEPLOY_APPLICATION_GROUP = "application-group deploy";

	private static final String REDEPLOY_APPLICATION_GROUP = "application-group redeploy";

	private static final String UNDEPLOY_APPLICATION_GROUP = "application-group undeploy";

	private static final String UNDEPLOY_APPLICATION_GROUP_ALL = "application-group all undeploy";

	private static final String DESTROY_APPLICATION_GROUP = "application-group destroy";

	private static final String DESTROY_APPLICATION_GROUP_ALL = "application-group all destroy";

	private static final String PROPERTIES_OPTION = "properties";

	private static final String PROPERTIES_FILE_OPTION = "propertiesFile";

	@Autowired
	private DataFlowShell dataFlowShell;

	@Autowired
	private UserInput userInput;

	@CliAvailabilityIndicator({ LIST_APPLICATION_GROUP, CREATE_APPLICATION_GROUP, DEPLOY_APPLICATION_GROUP, REDEPLOY_APPLICATION_GROUP,
			UNDEPLOY_APPLICATION_GROUP, UNDEPLOY_APPLICATION_GROUP_ALL,
		DESTROY_APPLICATION_GROUP, DESTROY_APPLICATION_GROUP_ALL })
	public boolean available() {
		DataFlowOperations dataFlowOperations = dataFlowShell.getDataFlowOperations();
		return dataFlowOperations != null && dataFlowOperations.applicationGroupOperations() != null;
	}

	@CliCommand(value = LIST_APPLICATION_GROUP, help = "List created application groups")
	public Table listApplicationGroups() {
		final PagedResources<ApplicationGroupDefinitionResource> applicationGroups = applicationGroupOperations().list();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Application Group Name");
		headers.put("dslText", "Application Group Definition");
		headers.put("status", "Status");
		BeanListTableModel<ApplicationGroupDefinitionResource> model = new BeanListTableModel<>(applicationGroups, headers);
		return DataFlowTables.applyStyle(new TableBuilder(model))
				.build();
	}

	@CliCommand(value = IMPORT_APPLICATION_GROUP, help = "Import a new application group")
	public String importApplicationGroup(
			@CliOption(mandatory = true,
					key = {"", "name"},
					help = "the name for the application group")
					String name,
			@CliOption(mandatory = true,
					key = {"uri"},
					help = "URI for the application group artifact")
					String uri,
			@CliOption(key = "force",
					help = "force update if application already exists",
					specifiedDefaultValue = "true",
					unspecifiedDefaultValue = "false")
					boolean force,
			@CliOption(key = "deploy",
					help = "whether to deploy the application group immediately",
					unspecifiedDefaultValue = "false",
					specifiedDefaultValue = "true")
					boolean deploy,
			@CliOption(key = { PROPERTIES_OPTION },
					help = "the properties for this deployment",
					mandatory = false) String properties,
			@CliOption(key = { PROPERTIES_FILE_OPTION },
					help = "the properties for this deployment (as a File)",
					mandatory = false) File propertiesFile) throws IOException {
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
		applicationGroupOperations().importApplicationGroup(name, uri, force, deploy, propertiesToUse);
		return (deploy) ? String.format("Successfully imported and deployed new application group '%s'", name) : String.format(
				"Successfully imported new application group '%s'", name);
	}

	@CliCommand(value = CREATE_APPLICATION_GROUP, help = "Create a new application group definition")
	public String createApplicationGroup(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the application group") String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "an application group definition, using the DSL (e.g. \"http & hdfs\")", optionContext = "disable-string-converter completion-application-group") String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the application group immediately", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean deploy) {
		applicationGroupOperations().createApplicationGroup(name, dsl, deploy);
		return (deploy) ? String.format("Created and deployed new application group '%s'", name) : String.format(
				"Created new application group '%s'", name);
	}

	@CliCommand(value = DEPLOY_APPLICATION_GROUP, help = "Deploy a previously created application group")
	public String deployApplicationGroup(
			@CliOption(key = { "", "name" }, help = "the name of the application group to deploy", mandatory = true/*, optionContext = "existing-application-group undeployed disable-string-converter"*/) String name,
			@CliOption(key = { PROPERTIES_OPTION }, help = "the properties for this deployment", mandatory = false) String properties,
			@CliOption(key = { PROPERTIES_FILE_OPTION }, help = "the properties for this deployment (as a File)", mandatory = false) File propertiesFile
			) throws IOException {
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
				propertiesToUse = Collections.<String, String> emptyMap();
				break;
			default:
				throw new AssertionError();
		}
		applicationGroupOperations().deploy(name, propertiesToUse);
		return String.format("Deployed application group '%s'", name);
	}

	@CliCommand(value = REDEPLOY_APPLICATION_GROUP, help = "Redeploy an existing application group")
	public String redeployApplicationGroup(
			@CliOption(key = { "", "name" }, help = "the name of the existing application group", mandatory = true) String name,
			@CliOption(key = { PROPERTIES_OPTION }, help = "the properties for this deployment", mandatory = false) String properties,
			@CliOption(key = { PROPERTIES_FILE_OPTION }, help = "the properties for this deployment (as a File)", mandatory = false) File propertiesFile
	) throws IOException {
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
		applicationGroupOperations().redeploy(name, propertiesToUse);
		return String.format("Redeployed application group '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_APPLICATION_GROUP, help = "Un-deploy a previously deployed application group")
	public String undeployApplicationGroup(
			@CliOption(key = { "", "name" }, help = "the name of the application group to un-deploy", mandatory = true/*, optionContext = "existing-application-group deployed disable-string-converter"*/) String name
			) {
		applicationGroupOperations().undeploy(name);
		return String.format("Un-deployed application group '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_APPLICATION_GROUP_ALL, help = "Un-deploy all previously deployed application groups")
	public String undeployAllApplicationGroups(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force
			) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really undeploy all application groups?", "n", "y", "n"))) {
			applicationGroupOperations().undeployAll();
			return String.format("Un-deployed all the application groups");
		}
		else {
			return "";
		}
	}

	@CliCommand(value = DESTROY_APPLICATION_GROUP, help = "Destroy an existing application group")
	public String destroyApplicationgroup(
			@CliOption(key = { "", "name" }, help = "the name of the application group to destroy", mandatory = true/*, optionContext = "existing-application-group disable-string-converter"*/) String name) {
		applicationGroupOperations().destroy(name);
		return String.format("Destroyed application group '%s'", name);
	}

	@CliCommand(value = DESTROY_APPLICATION_GROUP_ALL, help = "Destroy all existing application groups")
	public String destroyAllApplicationGroups(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really destroy all application groups?", "n", "y", "n"))) {
			applicationGroupOperations().destroyAll();
			return "Destroyed all application groups";
		}
		else {
			return "";
		}
	}

	ApplicationGroupOperations applicationGroupOperations() {
		return dataFlowShell.getDataFlowOperations().applicationGroupOperations();
	}
}
