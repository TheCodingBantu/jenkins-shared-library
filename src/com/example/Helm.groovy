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
               script.sh "helm push ${chartName} oci://${registryUrl}/${helmRepo}"
            }

            } catch (Exception e) {
                throw e
        }
    }
}
