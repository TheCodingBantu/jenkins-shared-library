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

    def dockerPush(String imageName, String registryUrl = 'https://registry.jambopay.co.ke', String credentialsId = 'registry') {
        try {
            script.echo "Pushing Docker image ${imageName} to ${registryUrl}..."
            script.docker.withRegistry(registryUrl, credentialsId) {
                dockerImage.push("latest")
                dockerImage.push("${script.env.BUILD_NUMBER}")
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
            script.sh "docker rmi ${imageName}"
            script.echo "Docker image ${imageName} removed successfully."
        } catch (Exception e) {
            script.echo "Warning: Failed to remove Docker image: ${e.message}"
        }
    }

    def readOrUpdateVersion(String action = 'read', String gitRepoUrl, String gitCredsId, String branchName, String versionFile, String defaultVersion) {
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
                        return updateVersion(currentVersion, versionFile, branchName)
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

    private def updateVersion(String currentVersion, String versionFile, String branchName) {
        def versionParts = currentVersion.tokenize('.')
        def major = versionParts[0].toInteger()
        def minor = versionParts[1].toInteger()
        def patch = versionParts[2].toInteger() + 1
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
