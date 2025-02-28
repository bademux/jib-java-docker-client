package dockerclient;

import com.google.cloud.tools.jib.api.DockerClient;
import com.google.cloud.tools.jib.api.DockerInfoDetails;
import com.google.cloud.tools.jib.api.ImageDetails;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.docker.CliDockerClient;
import com.google.cloud.tools.jib.image.ImageTarball;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

public class FallbackToJavaDockerClient implements DockerClient {

    private DockerClient dockerClient;

    @Override
    public String load(ImageTarball imageTarball, Consumer<Long> consumer) throws IOException, InterruptedException {
        return dockerClient.load(imageTarball, consumer);
    }

    @Override
    public void save(ImageReference imageReference, Path path, Consumer<Long> consumer) throws IOException, InterruptedException {
        dockerClient.save(imageReference, path, consumer);
    }

    @Override
    public ImageDetails inspect(ImageReference imageReference) throws IOException, InterruptedException {
        return dockerClient.inspect(imageReference);
    }

    @Override
    public DockerInfoDetails info() throws IOException, InterruptedException {
        return dockerClient.info();
    }

    @Override
    public boolean supported(Map<String, String> parameters) {
        if (this.dockerClient == null) {
            this.dockerClient = getDockerClientOrFallbackToJava(parameters);
        }
        return true;
    }

    public DockerClient getDockerClientOrFallbackToJava(Map<String, String> parameters) {
        if (CliDockerClient.isDefaultDockerInstalled()) {
            return new CliDockerClient(CliDockerClient.DEFAULT_DOCKER_CLIENT, parameters);
        }
        return new JavaDockerClient();
    }

}

