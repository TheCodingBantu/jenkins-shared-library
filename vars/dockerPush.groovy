
import com.example.Docker

def call(String registryUrl, String registryCreds, String releaseTag) {
    // REGISTRY_URL,REGISTRY_CREDS,env.IMAGEVERSION 
    return new Docker(this).dockerPush(registryUrl, registryCreds, releaseTag)
}
