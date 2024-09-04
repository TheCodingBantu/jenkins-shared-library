import com.example.Docker

def call(String gitRepoUrl,String gitCredsId, String branchName, String versionFile , String version ) {
    return new Docker(this).updateVersion(gitRepoUrl,gitCredsId, branchName, versionFile , version )
}
