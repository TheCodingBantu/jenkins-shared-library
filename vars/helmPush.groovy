
import com.example.Helm

def call(String chartName, String registryUrl, String registryCreds, String helmRepo) {
    return new Helm(this).helmPush(chartName,registryUrl, registryCreds, helmRepo)
}
