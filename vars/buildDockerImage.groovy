import com.example.Docker

def call(String imageName, String dockerfilePath, String buildArgs) {
    return new Docker(this).buildDockerImage(imageName, dockerfilePath, buildArgs)
}

