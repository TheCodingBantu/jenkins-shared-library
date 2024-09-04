import com.example.Docker

def call(String releaseType, String gitRepoUrl,String gitCredsId, String branchName, String versionFile , String defaultVersion ) {
    return new Docker(this).readVersion(releaseType, gitRepoUrl,gitCredsId, branchName, versionFile , defaultVersion )
}
