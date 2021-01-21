package dev.galasa.docker.internal;

import java.io.InputStream;
import java.util.Map;

import dev.galasa.docker.DockerManagerException;

public interface IDockerImageBuilder {

    public void buildImage(String imageName, InputStream Dockerfile, Map<String,InputStream> resources) throws DockerManagerException;
    
}