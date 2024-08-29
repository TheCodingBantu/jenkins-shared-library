#!/usr/bin/env groovy
package com.example

class Docker implements Serializable {
    def script
    def dockerImage

    Docker(script) {
        this.script = script
    }

    def checkoutGitRepo(String repoUrl, String branchName, String credentialsId){

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

    def readOrUpdateVersion(String action = 'read' , String gitRepoUrl,String gitCredsId, String branchName, String versionFile , String defaultVersion ){
        try{
            withCredentials([usernamePassword(credentialsId: "${gitCredsId}", passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {

            def encode = { String value ->
                java.net.URLEncoder.encode(value, 'UTF-8').replaceAll('\\+', '%20')
            }

            def encodedUsername = encode(GIT_USERNAME)
            def encodedPassword = encode(GIT_PASSWORD)

                sh """
                    git config user.email "jenkins@jambopay.com"
                    git config user.name "Jenkins"
                    git remote set-url origin https://${encodedUsername}:${encodedPassword}@${gitRepoUrl}
                    git fetch origin
                    git checkout ${branchName}
                    git pull origin ${branchName}
                """

            if (fileExists(versionFile)) {

                def currentVersion = readFile(versionFile).trim()
                if (action =='update'){
                    return currentVersion;
                }
                else if (action =='write'){
                    //write to file
                    def versionParts = currentVersion.tokenize('.')
                    def major = versionParts[0].toInteger()
                    def minor = versionParts[1].toInteger()
                    def patch = versionParts[2].toInteger()
                    patch += 1
                    def newVersion = "${major}.${minor}.${patch}"
                    writeFile file: versionFile, text: "${newVersion}"

                    echo "File '${versionFile}' has been persisted with version: ${defaultVersion}"
                    echo "Pushing version changes to Repository"
                    sh  """
                        git add "${versionFile}"
                        git commit -m "Updated version from ${currentVersion} to: ${newVersion}"
                        git push origin ${BRANCH_NAME}
                        echo "Version change committed to ${GIT_URL}, Branch: ${BRANCH_NAME}"
                    """
                    echo "Working Version updated to ${newVersion} "
                }
            }
            else{
               //write the default version
               if (action == 'read')
                {
                   return defaultVersion;
                }
                else if(action == 'update'){
                    sh "mkdir -p \$(dirname ${versionFile})"
                    writeFile file: versionFile, text: "${defaultVersion}"
                    echo "File '${versionFile}' has been persisted with version: ${defaultVersion}"
                    sh  """
                        git add "${versionFile}"
                        git commit -m "No previous versions found. Persisted version: ${defaultVersion}"
                        git push origin ${branchName}
                        echo "Version change committed to ${GIT_URL}, Branch: ${branchName}"
                    """
                }

            }

        }
        }catch (Exception e) {
            script.echo "Failed to read Version: ${e.message}"
        }
    }

}
