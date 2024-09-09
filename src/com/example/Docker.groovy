package com.example

class Docker implements Serializable {
    def script
    def dockerImage

    Docker(script) {
        this.script = script
    }

    def checkoutGitRepo(String repoUrl, String branchName, String credentialsId) {
        try {
            script.sh "git stash"
            script.checkout([$class: 'GitSCM',
                branches: [[name: "refs/heads/${branchName}"]],
                userRemoteConfigs: [[credentialsId: credentialsId, url: repoUrl]]
            ])
        } catch (Exception e) {
            script.error "${repoUrl} checkout failed: ${e.message}"
            throw e
        }
    }

    def buildDockerImage(String imageName, String dockerfilePat sh """
                    helm repo add ${HELM_REPO_NAME} ${HELM_REPO_URL} --username ${HELM_REPO_USERNAME} --password ${HELM_REPO_PASSWORD}
                    helm repo update
                    """h = '.', String buildArgs = '') {
        try {
   
            script.echo "Building the Docker image: ${imageName}..."
            script.docker.build(imageName, "${dockerfilePath} ${buildArgs}")
            script.echo "Docker image ${imageName} built successfully."
        } catch (Exception e) {
            script.error "Failed to build Docker image: ${e.message}"
            throw e
        }
    }

    def dockerPush(String imageName, String registryUrl, String credentialsId, String releaseTag) {
        try {
            script.echo "Pushing Docker image ${imageName} to ${registryUrl}..."
            script.docker.withRegistry(registryUrl, credentialsId) {
                script.docker.image(imageName).push(releaseTag)
            }
            script.echo "Docker image ${imageName} pushed successfully."
        } catch (Exception e) {
            script.error "Failed to push Docker image: ${e.message}"
            throw e
        }
    }

    def cleanupDockerImage(String imageName) {
        try {
            script.echo "Cleaning up Docker image ${imageName}..."
            script.sh "docker rmi -f \$(docker images ${imageName} -a -q)"
            script.echo "Docker image ${imageName} removed successfully."
        } catch (Exception e) {
            script.echo "Warning: Failed to remove Docker image: ${e.message}"
        }
    }

    def readVersion(String releaseType, String gitRepoUrl, String gitCredsId, String branchName, String versionFile, String defaultVersion) {
        try {
            script.withCredentials([script.usernamePassword(credentialsId: gitCredsId, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                def encodedUsername = URLEncoder.encode(script.env.GIT_USERNAME, 'UTF-8').replaceAll('\\+', '%20')
                def encodedPassword = URLEncoder.encode(script.env.GIT_PASSWORD, 'UTF-8').replaceAll('\\+', '%20')

                script.sh """
                    git config user.email "jenkins@jenkins.com"
                    git config user.name "Jenkins"
                    git remote set-url origin https://${encodedUsername}:${encodedPassword}@${gitRepoUrl}
                    git fetch origin
                    git checkout ${branchName}
                    git pull origin ${branchName}
                """
                //calculate and return the new version 

                if (script.fileExists(versionFile)) {
                    def currentVersion = script.readFile(versionFile).trim()
                    return calculateVersion(currentVersion,releaseType) 
               
                } else {
                    return calculateVersion(defaultVersion,releaseType)
                  
                }
            }
        } catch (Exception e) {
            script.error "Failed to read or update version: ${e.message}"
            throw e
        }
    }

  def updateVersion(String gitRepoUrl, String gitCredsId, String branchName, String versionFile, String version) {
        try {
            //try to stash if previous stages had changed anything
            script.sh "git stash"
            script.withCredentials([script.usernamePassword(credentialsId: gitCredsId, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                def encodedUsername = URLEncoder.encode(script.env.GIT_USERNAME, 'UTF-8').replaceAll('\\+', '%20')
                def encodedPassword = URLEncoder.encode(script.env.GIT_PASSWORD, 'UTF-8').replaceAll('\\+', '%20')

                script.sh """
                    git config user.email "jenkins@jenkins.com"
                    git config user.name "Jenkins"
                    git remote set-url origin https://${encodedUsername}:${encodedPassword}@${gitRepoUrl}
                    git fetch origin
                    git checkout ${branchName}
                    git pull origin ${branchName}
                """
                return updateVersionFile(versionFile,version,branchName)
            }
        } catch (Exception e) {
            script.error "Failed to read or update version: ${e.message}"
            throw e
        }
    }

    private def calculateVersion (String currentVersion,  String releaseType) {
        def (major, minor, patch) = currentVersion.tokenize('.').collect { it.toInteger() }

        switch (releaseType) {
            case 'major':
                major++
                break
            case 'minor':
                minor++
                break
            case 'patch':
                patch++
                break
        }
        def newVersion = "${major}.${minor}.${patch}"
        return newVersion
    }

    private def updateVersionFile(String versionFile, String version, String branchName) {
        //if dir doesnt exist
        if (!script.fileExists(versionFile)) {
           script.sh "mkdir -p \$(dirname ${versionFile})"
         
        } 
        script.writeFile file: versionFile, text: version
        script.echo "Created new version file with version: ${version}"
        script.sh """
            git add "${versionFile}"
            git commit -m "Update version to : ${version}"
            git push origin ${branchName}
        """
        return version
    }
}
