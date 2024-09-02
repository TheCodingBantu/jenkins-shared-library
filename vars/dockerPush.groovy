
import com.example.Docker

def call(String imageName, String registryUrl, String registryCreds, String releaseTag) {
    // REGISTRY_URL,REGISTRY_CREDS,env.IMAGEVERSION 
    return new Docker(this).dockerPush(imageName,registryUrl, registryCreds, releaseTag)
}
