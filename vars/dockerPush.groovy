
import com.example.Docker

def call(String imageName, String fqdn, String registryUrl, String registryCreds, String releaseTag) {
    // REGISTRY_URL,REGISTRY_CREDS,env.IMAGEVERSION 
    return new Docker(this).dockerPush(imageName,fqdn, registryUrl, registryCreds, releaseTag)
}
