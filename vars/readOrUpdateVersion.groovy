import com.example.Docker

def call(String action, String releaseType, String gitRepoUrl,String gitCredsId, String branchName, String versionFile , String defaultVersion ) {
    return new Docker(this).readOrUpdateVersion(action, releaseType, gitRepoUrl,gitCredsId, branchName, versionFile , defaultVersion )
}
