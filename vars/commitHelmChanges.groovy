
import com.example.Helm

def call(String gitRepoUrl, String gitCredsId, String branchName, String chartPath ) {
    return new Helm(this).commitHelmChanges(gitRepoUrl,gitCredsId, branchName, chartPath)
}
