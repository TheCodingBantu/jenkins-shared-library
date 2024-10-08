package com.example

class Helm implements Serializable {
    def script

    Helm(script) {
        this.script = script
    }

    def helmAddRepo(String registryUrl, String registryCreds, String helmRepo, String namespace ){
        try {
              script.docker.withRegistry(registryUrl, registryCreds) {
                 def strippedUrl = registryUrl.replaceAll(/^https?:\/\//, '')
                 script.sh """
                    helm repo add ${helmRepo} ${strippedUrl} --namespace=${namespace}
                    helm install prsp-website oci://registry.jenkins.com/sms-helm/prsp-website --version 17.4.7 --namespace=sms-poc
                    helm repo update
                """
            }

            } catch (Exception e) {
                throw e
        }
    }

    def helmPush(String chartName, String registryUrl, registryCreds, String helmRepo) {
        try {
            //we can use docker registry
              script.docker.withRegistry(registryUrl, registryCreds) {
                def strippedUrl = registryUrl.replaceAll(/^https?:\/\//, '')
                script.sh "helm push ${chartName} oci://${strippedUrl}/${helmRepo}"
            }

            } catch (Exception e) {
                throw e
        }
    }
    def commitHelmChanges(String gitRepoUrl, String gitCredsId, String branchName, String chartPath) {
        try {
            //try to stash if previous stages had changed anything
            script.withCredentials([script.usernamePassword(credentialsId: gitCredsId, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                def encodedUsername = URLEncoder.encode(script.env.GIT_USERNAME, 'UTF-8').replaceAll('\\+', '%20')
                def encodedPassword = URLEncoder.encode(script.env.GIT_PASSWORD, 'UTF-8').replaceAll('\\+', '%20')
                def strippedUrl = gitRepoUrl.replaceAll(/^https?:\/\//, '')
                script.sh """
                    git config user.email "jenkins@jenkins.com"
                    git config user.name "Jenkins"
                    git remote set-url origin https://${encodedUsername}:${encodedPassword}@${strippedUrl}
                    git checkout ${branchName}
                    git add "${chartPath}"
                    git commit -m "committed chart version updates"
                    git push origin ${branchName}
                """
            }
        } catch (Exception e) {
            script.error "Failed to read or update version: ${e.message}"
            throw e
        }
    }
}
