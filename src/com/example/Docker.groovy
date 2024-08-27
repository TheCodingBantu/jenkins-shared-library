#!/usr/bin/env groovy
package com.example

class Docker implements Serializable {
    // serializable saves the state if the pipeline is paused & resumed
    def script

    Docker(script) {
        this.script = script
    }

    def buildDockerImage(String imageName) {
        script.echo "building the docker image..."
        script.sh "docker build -t $imageName ."
    }

    def dockerLogin(String credId, String passVar, String userVar) {
        script.withCredentials([script.usernamePassword(credentialsId: '$credId', passwordVariable: '$passVar', usernameVariable: '$userVar')]) {
            script.sh "echo $script.$passVar | docker login -u $script.$userVar --password-stdin"
        }
    }

    def dockerPush(String imageName) {
      
        script.sh "docker push $imageName"
    }

}


    // stage('Pushing Image') {
    //   steps{
    //     script {
    //       docker.withRegistry('https://registry.jambopay.co.ke', 'registry') {
    //         dockerImage.push("latest")
    //       }
    //     }
    //   }
    // }