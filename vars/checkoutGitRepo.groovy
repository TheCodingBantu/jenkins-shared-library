#!/usr/bin/env groovy

import com.example.Docker

def call(String repo_url, String branchName, String credentialsId) {
    return new Docker(this).checkoutGitRepo(repoUrl, branchName, credentialsId)
}
