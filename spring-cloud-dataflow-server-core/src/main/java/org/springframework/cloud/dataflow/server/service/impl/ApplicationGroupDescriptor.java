package org.springframework.cloud.dataflow.server.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.DefinitionType;
import org.springframework.util.StringUtils;

public class ApplicationGroupDescriptor {

    private List<Application> apps = new ArrayList<>();

    private List<Standalone> standalone = new ArrayList<>();

    private List<Stream> stream = new ArrayList<>();

    public String toDsl() {
        // TODO task?

        List<String> definitions = new ArrayList<>();
        if (!standalone.isEmpty()) definitions.add(StringUtils.collectionToDelimitedString(standalone, " & "));
        if (!stream.isEmpty()) definitions.add(StringUtils.collectionToDelimitedString(stream, " & "));
        return StringUtils.collectionToDelimitedString(definitions, " & ");
    }

    public List<Application> getApps() {
        return apps;
    }

    public void setApps(final List<Application> apps) {
        this.apps = apps;
    }

    public List<Standalone> getStandalone() {
        return standalone;
    }

    public void setStandalone(final List<Standalone> standalone) {
        this.standalone = standalone;
    }

    public List<Stream> getStream() {
        return stream;
    }

    public void setStream(final List<Stream> stream) {
        this.stream = stream;
    }

    public static class Application {

        private String name;

        private ApplicationType type;

        private String uri;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public ApplicationType getType() {
            return type;
        }

        public void setType(final ApplicationType type) {
            this.type = type;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(final String uri) {
            this.uri = uri;
        }

        @Override
        public String toString() {
            return String.format("%s.%s", type, name);
        }
    }

    public static class Standalone {

        private String name;

        private String dsl;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getDsl() {
            return dsl;
        }

        public void setDsl(final String dsl) {
            this.dsl = dsl;
        }

        @Override
        public String toString() {
            return String.format("%s: %s", DefinitionType.standalone, name);
        }
    }

    public static class Stream {

        private String name;
        private String dsl;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getDsl() {
            return dsl;
        }

        public void setDsl(final String dsl) {
            this.dsl = dsl;
        }

        @Override
        public String toString() {
            return String.format("%s: %s", DefinitionType.stream, name);
        }
    }
}
