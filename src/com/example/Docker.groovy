package com.example

class Docker implements Serializable {
    def script
    def dockerImage

    Docker(script) {
        this.script = script
    }

    def checkoutGitRepo(String repoUrl, String branchName, String credentialsId) {
        try {
            script.checkout([$class: 'GitSCM',
                branches: [[name: "refs/heads/${branchName}"]],
                userRemoteConfigs: [[credentialsId: credentialsId, url: repoUrl]]
            ])
        } catch (Exception e) {
            script.error "${repoUrl} checkout failed: ${e.message}"
            throw e
        }
    }

    def buildDockerImage(String imageName, String dockerfilePath = '.', String buildArgs = '') {
        try {
            script.echo "Building the Docker image: ${imageName}..."
            dockerImage = script.docker.build(imageName, "${dockerfilePath} ${buildArgs}")
            script.echo "Docker image ${imageName} built successfully."
        } catch (Exception e) {
            script.error "Failed to build Docker image: ${e.message}"
            throw e
        }
    }

    def dockerPush(String registryUrl, String credentialsId, String releaseTag) {
        try {
            script.echo "Pushing Docker image ${dockerImage.imageName()} to ${registryUrl}..."
            script.docker.withRegistry(registryUrl, credentialsId) {
                dockerImage.push(releaseTag)
            }
            script.echo "Docker image ${dockerImage.imageName()} pushed successfully."
        } catch (Exception e) {
            script.error "Failed to push Docker image: ${e.message}"
            throw e
        }
    }

    def cleanupDockerImage() {
        try {
            script.echo "Cleaning up Docker image ${dockerImage.imageName()}..."
            script.sh "docker rmi ${dockerImage.imageName()}"
            script.echo "Docker image ${dockerImage.imageName()} removed successfully."
        } catch (Exception e) {
            script.echo "Warning: Failed to remove Docker image: ${e.message}"
        }
    }

    def readOrUpdateVersion(String action = 'read', String releaseType = 'patch' , String gitRepoUrl, String gitCredsId, String branchName, String versionFile, String defaultVersion) {
        try {
            script.withCredentials([script.usernamePassword(credentialsId: gitCredsId, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                def encodedUsername = URLEncoder.encode(script.env.GIT_USERNAME, 'UTF-8').replaceAll('\\+', '%20')
                def encodedPassword = URLEncoder.encode(script.env.GIT_PASSWORD, 'UTF-8').replaceAll('\\+', '%20')

                script.sh """
                    git config user.email "jenkins@jambopay.com"
                    git config user.name "Jenkins"
                    git remote set-url origin https://${encodedUsername}:${encodedPassword}@${gitRepoUrl}
                    git fetch origin
                    git checkout ${branchName}
                    git pull origin ${branchName}
                """

                if (script.fileExists(versionFile)) {
                    def currentVersion = script.readFile(versionFile).trim()
                    if (action == 'read') {
                        return currentVersion
                    } else if (action == 'update') {
                        return updateVersion(currentVersion, versionFile, branchName, releaseType)
                    }
                } else {
                    if (action == 'read') {
                        return defaultVersion
                    } else if (action == 'update') {
                        return createNewVersionFile(versionFile, defaultVersion, branchName)
                    }
                }
            }
        } catch (Exception e) {
            script.error "Failed to read or update version: ${e.message}"
            throw e
        }
    }

    private def updateVersion(String currentVersion, String versionFile, String branchName, String releaseType) {
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

        script.writeFile file: versionFile, text: newVersion
        script.echo "Updated version: ${currentVersion} -> ${newVersion}"

        script.sh """
            git add "${versionFile}"
            git commit -m "Updated version from ${currentVersion} to ${newVersion}"
            git push origin ${branchName}
        """

        return newVersion
    }

    private def createNewVersionFile(String versionFile, String defaultVersion, String branchName) {
        script.sh "mkdir -p \$(dirname ${versionFile})"
        script.writeFile file: versionFile, text: defaultVersion
        script.echo "Created new version file with version: ${defaultVersion}"

        script.sh """
            git add "${versionFile}"
            git commit -m "No previous versions found. Persisted version: ${defaultVersion}"
            git push origin ${branchName}
        """

        return defaultVersion
    }
}
