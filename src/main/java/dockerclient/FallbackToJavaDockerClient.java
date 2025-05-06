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

/**
 * init class ASAP to fix "premature ejaculation"
 * ServiceLoader.load(com.google.cloud.tools.jib.api.DockerClient)
 * 
 * <a href="https://github.com/GoogleContainerTools/jib/blob/9027c8dafb8ee12b9d37e1521caf62dfbcc3d374/jib-gradle-plugin/src/main/java/com/google/cloud/tools/jib/gradle/BuildDockerTask.java#L100">Bug here</a>
 */
public class FallbackToJavaDockerClient implements DockerClient {

    private static final boolean DEFAULT_DOCKER_INSTALLED = CliDockerClient.isDefaultDockerInstalled();

    static {
        if (!DEFAULT_DOCKER_INSTALLED) {
            System.setProperty("jib.dockerClient.executable", "/bin/true"); //fake docker binary to make jibDockerBuild task happy
        }
    }

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
