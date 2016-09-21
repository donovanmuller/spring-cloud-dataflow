package org.springframework.cloud.dataflow.server.service.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.YamlConfigurationFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.server.controller.AppRegistryController;
import org.springframework.cloud.dataflow.server.controller.StandaloneDefinitionController;
import org.springframework.cloud.dataflow.server.controller.StandaloneDeploymentController;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.controller.StreamDeploymentController;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.zeroturnaround.zip.ZipUtil;

@EnableConfigurationProperties
public class ApplicationGroupRegistryService {

	private static final Logger logger = LoggerFactory
			.getLogger(ApplicationGroupRegistryService.class);

	private static String APPLICATION_GROUP_DESCRIPTOR_FILE = "application-group.yml";

	private final MavenProperties mavenProperties;

	private final MavenResourceJarExtractor mavenResourceJarExtractor;

	private final AppRegistryController appRegistryController;

	private final StandaloneDefinitionController standaloneDefinitionController;

	private final StandaloneDeploymentController standaloneDeploymentController;

	private final StreamDefinitionController streamDefinitionController;

	private final StreamDeploymentController streamDeploymentController;

	public ApplicationGroupRegistryService(MavenProperties mavenProperties,
			AppRegistryController appRegistryController,
			StandaloneDefinitionController standaloneDefinitionController,
			StandaloneDeploymentController standaloneDeploymentController,
			StreamDefinitionController streamDefinitionController,
			StreamDeploymentController streamDeploymentController) {
		this.mavenProperties = mavenProperties;
		this.appRegistryController = appRegistryController;
		this.standaloneDefinitionController = standaloneDefinitionController;
		this.standaloneDeploymentController = standaloneDeploymentController;
		this.streamDefinitionController = streamDefinitionController;
		this.streamDeploymentController = streamDeploymentController;
		this.mavenResourceJarExtractor = new MavenResourceJarExtractor();
	}

	public ApplicationGroupDescriptor extractAndParseDescriptor(
			MavenResource mavenResource) throws Exception {
		Resource applicationGroupDefinitionResource = mavenResourceJarExtractor
				.extractFile(mavenResource, APPLICATION_GROUP_DESCRIPTOR_FILE);

		YamlConfigurationFactory<ApplicationGroupDescriptor> yamlConfigurationFactory = new YamlConfigurationFactory<>(
				ApplicationGroupDescriptor.class);
		yamlConfigurationFactory.setResource(applicationGroupDefinitionResource);
		yamlConfigurationFactory.afterPropertiesSet();

		return yamlConfigurationFactory.getObject();
	}

	public void registerAndCreateApps(String name, ApplicationGroupDescriptor descriptor,
			boolean force) throws Exception {
		for (ApplicationGroupDescriptor.Application app : descriptor.getApps()) {
			logger.info("Registering application '{}:{}' in application group '{}'",
					app.getType(), app.getName(), name);
			appRegistryController.register(app.getType(), app.getName(), app.getUri(),
					force);
		}

		for (ApplicationGroupDescriptor.Standalone standalone : descriptor
				.getStandalone()) {
			standaloneDefinitionController.save(standalone.getName(), standalone.getDsl(),
					force, false);
		}

		for (ApplicationGroupDescriptor.Stream stream : descriptor.getStream()) {
			streamDefinitionController.save(stream.getName(), stream.getDsl(), false);
		}

		// TODO tasks
	}

	class MavenResourceJarExtractor {

		/**
		 * Extract a single file in the specified {@link Resource} that must be a zip
		 * archive. This single file will be represented as a {@link Resource}. The
		 * reference to the file in the archive must be <b>the absolute path</b> to the
		 * file. I.e. <code>/the/path/to/the/file.txt</code>, where <code>file.txt</code>
		 * is the file to extract.
		 *
		 * @param resource
		 * @param file
		 * @return a {@link Resource} representing the extracted file
		 * @throws IOException if the file does not exist in the {@link Resource}
		 */
		public Resource extractFile(Resource resource, String file) throws IOException {
			logger.debug("Extracting [{}] from: [{}]", file, resource.getFile());

			byte[] unpackedEntry = ZipUtil.unpackEntry(resource.getFile(), file);
			if (unpackedEntry != null) {
				return new ByteArrayResource(unpackedEntry);
			}
			else {
				throw new IOException(String.format(
						"File '%s' does not exist in resource: '%s'", file, resource));
			}
		}
	}

}
