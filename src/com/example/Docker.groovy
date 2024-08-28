#!/usr/bin/env groovy
package com.example

class Docker implements Serializable {
    def script
    def dockerImage

    Docker(script) {
        this.script = script
    }

    def checkoutGitRepo(String repoUrl, String branchName, String credentialsId){

      try{
        script.checkout scmGit(
                        branches: [[name: '$branchName']],
                        userRemoteConfigs: [[credentialsId: '$credentialsId',
                            url: '$repoUrl']])
        } catch (Exception e) {
            script.error "${repoUrl} checkout failed ${e}"
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


}
