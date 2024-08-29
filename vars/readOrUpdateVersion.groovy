import com.example.Docker

def call(String action, String gitRepoUrl,String gitCredsId, String branchName, String versionFile , String defaultVersion ) {
    return new Docker(this).readOrUpdateVersion(action, gitRepoUrl,gitCredsId, branchName, versionFile , defaultVersion )
}
