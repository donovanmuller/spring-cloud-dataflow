/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.features;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.dataflow.server.controller.AppRegistryController;
import org.springframework.cloud.dataflow.server.controller.StandaloneDefinitionController;
import org.springframework.cloud.dataflow.server.controller.StandaloneDeploymentController;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.controller.StreamDeploymentController;
import org.springframework.cloud.dataflow.server.repository.ApplicationGroupDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.RdbmsApplicationGroupDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.impl.ApplicationGroupRegistryService;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = FeaturesProperties.FEATURES_PREFIX, name = FeaturesProperties.APPLICATION_GROUPS_ENABLED)
public class ApplicationGroupConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ApplicationGroupDefinitionRepository applicationGroupDefinitionRepository(
			DataSource dataSource) {
		return new RdbmsApplicationGroupDefinitionRepository(dataSource);
	}

	@Bean
	@ConditionalOnMissingBean
	public ApplicationGroupRegistryService applicationGroupRegistryService(
			MavenProperties mavenProperties, AppRegistryController appRegistryController,
			StandaloneDefinitionController standaloneDefinitionController,
			StandaloneDeploymentController standaloneDeploymentController,
			StreamDefinitionController streamDefinitionController,
			StreamDeploymentController streamDeploymentController) {
		return new ApplicationGroupRegistryService(mavenProperties, appRegistryController,
				standaloneDefinitionController, standaloneDeploymentController,
				streamDefinitionController, streamDeploymentController);
	}
}
