package dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.LoadImageCallback;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.google.cloud.tools.jib.api.DescriptorDigest;
import com.google.cloud.tools.jib.api.DockerInfoDetails;
import com.google.cloud.tools.jib.api.ImageDetails;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.http.NotifyingOutputStream;
import com.google.cloud.tools.jib.image.ImageTarball;
import com.google.common.io.ByteStreams;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class JavaDockerClient implements com.google.cloud.tools.jib.api.DockerClient {

    private final DockerClient dockerClient;

    public JavaDockerClient() {
        this(DefaultDockerClientConfig.createDefaultConfigBuilder().build());
    }

    public JavaDockerClient(DockerClientConfig config) {
        this(config, new OkDockerHttpClient.Builder().dockerHost(config.getDockerHost()).sslConfig(config.getSSLConfig()).build());
    }

    public JavaDockerClient(DockerClientConfig config, DockerHttpClient httpClient) {
        dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    @Override
    public String load(ImageTarball imageTarball, Consumer<Long> consumer) throws IOException {
        try (
                PipedOutputStream pos = new PipedOutputStream();
                InputStream pis = new PipedInputStream(pos);
                NotifyingOutputStream nos = new NotifyingOutputStream(pos, consumer);
                LoadImageCallback task = dockerClient.loadImageAsyncCmd(pis).start()
        ) {
            imageTarball.writeTo(nos);
            return task.awaitMessage();
        }
    }

    @Override
    public void save(ImageReference imageReference, Path path, Consumer<Long> consumer) throws IOException {
        try (
                InputStream is = new BufferedInputStream(dockerClient.saveImageCmd(imageReference.toString()).exec());
                OutputStream os = new BufferedOutputStream(Files.newOutputStream(path));
                NotifyingOutputStream notifyingOs = new NotifyingOutputStream(os, consumer)
        ) {
            ByteStreams.copy(is, notifyingOs);
        }
    }

    @Override
    public ImageDetails inspect(ImageReference imageReference) {
        InspectImageResponse info = dockerClient.inspectImageCmd(imageReference.toString()).exec();
        return new ImageDetails() {

            @Override
            public long getSize() {
                return info.getSize();
            }

            @Override
            public DescriptorDigest getImageId() throws DigestException {
                return DescriptorDigest.fromDigest(info.getId());
            }

            @Override
            public List<DescriptorDigest> getDiffIds() throws DigestException {
                List<String> layers = info.getRootFS().getLayers();
                List<DescriptorDigest> processedDiffIds = new ArrayList<>(layers.size());
                for (String diffId : layers) {
                    processedDiffIds.add(DescriptorDigest.fromDigest(diffId.trim()));
                }
                return processedDiffIds;
            }
        };
    }

    @Override
    public DockerInfoDetails info() {
        Info info = dockerClient.infoCmd().exec();
        return new DockerInfoDetails() {
            @Override
            public String getOsType() {
                return info.getOsType();
            }

            @Override
            public String getArchitecture() {
                return info.getArchitecture();
            }
        };
    }

    @Override
    public boolean supported(Map<String, String> map) {
        return true;
    }
  
}

