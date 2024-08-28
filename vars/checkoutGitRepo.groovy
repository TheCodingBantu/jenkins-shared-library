#!/usr/bin/env groovy

import com.example.Docker

def call(String gitUrl, String branchName, String credentialsId) {
    return new Docker(this).checkoutGitRepo(imageName)
}
