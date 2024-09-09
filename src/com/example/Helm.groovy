package com.example

class Helm implements Serializable {
    def script

    Helm(script) {
        this.script = script
    }
    def helmPush(String chartName, String registryUrl, registryCreds, String helmRepo) {
        try {
            //we can use docker registry
              script.docker.withRegistry(registryUrl, registryCreds) {
                def strippedUrl = registryUrl.replaceAll(/^https?:\/\//, '')
                script.echo "${strippedUrl}"
               script.sh "helm push ${chartName} oci://${strippedUrl}/${helmRepo}"
            }

            } catch (Exception e) {
                throw e
        }
    }
}
