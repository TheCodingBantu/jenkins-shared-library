import com.example.Docker

def call(String repoUrl, String branchName, String credentialsId) {
    return new Docker(this).checkoutGitRepo(repoUrl, branchName, credentialsId)
}
